package net.johnceo.sparklingmutuals.history

import java.util.concurrent.Executors
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.api.ApiRetryWindow
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/**
 * Personal Safari profile reads for the dex, Stats and achievement baselines.
 */
object OwnStats {
    private const val REFRESH_MILLIS = 30 * 60_000L
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/own-stats")
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals own stats").apply { isDaemon = true } }
    @Volatile var dex: Set<String> = emptySet()
        private set
    @Volatile var runs = 0L
        private set
    private class SavedCaptures(val accounts: MutableMap<String, CaptureSnapshot> = mutableMapOf())
    private val savedCaptures by lazy { HistoryStore.load("api-captures", SavedCaptures::class.java, ::SavedCaptures) }
    internal val captures get() = savedCaptures.accounts[ConfigManager.historyOwnerUuid]
    internal fun captureProgress() = CaptureProgress.total(captures) { RunHistory.capturesBetween(it) }
    private var lastRequestAt = 0L
    private var fetching = false
    internal val loadingCaptures get() = fetching && identity == ConfigManager.historyOwnerUuid
    private val retry = ApiRetryWindow()

    private var identity: String? = null
    private var source: net.johnceo.sparklingmutuals.api.ApiProvider? = null
    private var generation = 0L

    fun tick(client: Minecraft) {
        if (ConfigManager.achievements && SkyblockSidebar.inSkyblock(client)) refresh(client, openingStats = false)
    }

    internal fun openStats(client: Minecraft) = refresh(client, openingStats = true)

    private fun refresh(client: Minecraft, openingStats: Boolean) {
        val now = System.currentTimeMillis()
        val provider = HypixelApi.provider
        val uuid = HistoryAccountId.selected(client) ?: return
        if (HistoryServerPolicy.alpha(client)) return
        if (identity != uuid || source != provider) {
            reset(); identity = uuid; source = provider
        }
        if (!provider.enabled || fetching || retry.pending && !retry.due(now, provider) ||
            !openingStats && !retry.pending && now - lastRequestAt < REFRESH_MILLIS) return
        retry.reset()
        fetching = true; lastRequestAt = now
        val revision = generation
        executor.submit {
            val result = runCatching { HypixelApi.getSafariProfileObservation(uuid, fresh = true) }
            client.execute {
                if (revision != generation) return@execute
                fetching = false
                if (HistoryAccountId.selected(client) != uuid || HypixelApi.provider != provider ||
                    HistoryServerPolicy.alpha(client)) { lastRequestAt = 0; return@execute }
                result.onFailure {
                    retry.failed(System.currentTimeMillis(), provider)
                    logger.info("Own Safari stats lookup failed: {}", it.message)
                }.onSuccess { observation ->
                    val profile = observation.profile
                    dex = profile.caught.toSet()
                    runs = profile.runs ?: 0L
                    val snapshot = CaptureSnapshot(observation.profileId, observation.fetchedAt, profile.biomeCaptures, profile.catches)
                    val previous = savedCaptures.accounts[uuid]
                    if (snapshot.achievementTotal != null || previous?.profileId != snapshot.profileId) {
                        val updated = snapshot.reconcile(previous, RunHistory::capturesBetween)
                        if (updated != previous) {
                            savedCaptures.accounts[uuid] = updated
                            HistoryStore.save("api-captures", savedCaptures)
                        }
                    }
                    val earned = Achievements.afterApiStats()
                    if (earned > 0) Achievements.queueLocal(LocalChat.component(
                        "[SM] Your Hypixel profile earns §e$earned§r achievement${if (earned == 1) "" else "s"}. See /sm achievements."))
                }
            }
        }
    }

    fun sparkling(species: String) { dex = dex + species }

    fun reset() { generation++; fetching = false; identity = null; source = null; lastRequestAt = 0; dex = emptySet(); runs = 0; retry.reset() }
}
