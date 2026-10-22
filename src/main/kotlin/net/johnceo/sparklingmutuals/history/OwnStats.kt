package net.johnceo.sparklingmutuals.history

import java.util.concurrent.Executors
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.api.ApiRetryWindow
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/**
 * Your own Safari numbers from Hypixel's API (your key, or the shared cloud API), so achievements and the dex count
 * everything from before the mod was installed: the sparkling dex, runs (the highest of the Doomspiral, Wumpa and
 * Hideyho bestiary counts, one of each per run) and critters caught (the 37 Safari critters' bestiary counts). Read
 * when you join SkyBlock, then every thirty minutes.
 */
object OwnStats {
    private const val REFRESH_MILLIS = 30 * 60_000L
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/own-stats")
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals own stats").apply { isDaemon = true } }
    @Volatile var dex: Set<String> = emptySet()
        private set
    @Volatile var runs = 0L
        private set
    @Volatile var caught = 0L
        private set
    private var fetchedAt = 0L
    private var fetching = false
    private val retry = ApiRetryWindow()

    private var identity: String? = null
    private var source: net.johnceo.sparklingmutuals.api.ApiProvider? = null
    private var generation = 0L

    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        val provider = HypixelApi.provider
        val uuid = client.player?.uuid?.toString() ?: return
        if (identity != uuid || source != provider) {
            reset(); identity = uuid; source = provider
        }
        if (fetching || (if (retry.pending) !retry.due(now, provider) else now - fetchedAt < REFRESH_MILLIS) ||
            !provider.enabled || !SkyblockSidebar.inSkyblock(client)) return
        retry.reset()
        fetching = true; fetchedAt = now
        val revision = generation
        executor.submit {
            val result = runCatching { HypixelApi.getSafariProfile(uuid) }
            client.execute {
                if (revision != generation) return@execute
                fetching = false
                result.onFailure {
                    retry.failed(System.currentTimeMillis(), provider)
                    logger.info("Own Safari stats lookup failed: {}", it.message)
                }.onSuccess { profile ->
                    dex = profile.caught.toSet()
                    profile.runs?.let { runs = it }
                    profile.catches?.let { caught = it }
                    val earned = Achievements.afterApiStats()
                    if (earned > 0) client.player?.sendSystemMessage(LocalChat.component(
                        "[SM] Your Hypixel profile earns §e$earned§r achievement${if (earned == 1) "" else "s"}. See /sm achievements."))
                }
            }
        }
    }

    fun sparkling(species: String) { dex = dex + species }

    fun reset() { generation++; fetching = false; identity = null; source = null; fetchedAt = 0; dex = emptySet(); runs = 0; caught = 0; retry.reset() }
}
