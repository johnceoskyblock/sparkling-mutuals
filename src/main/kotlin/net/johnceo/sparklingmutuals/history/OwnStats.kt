package net.johnceo.sparklingmutuals.history

import java.util.concurrent.Executors
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.api.ApiProvider
import net.johnceo.sparklingmutuals.history.achievements.AchievementScope
import net.johnceo.sparklingmutuals.history.achievements.DexSnapshot
import net.johnceo.sparklingmutuals.history.achievements.ProfileResponseGuard
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

/**
 * Your own Safari numbers from Hypixel's API (your key, or the shared cloud API), so achievements and the dex count
 * everything from before the mod was installed: the sparkling dex, runs (the highest of the Doomspiral, Wumpa and
 * Hideyho bestiary counts, one of each per run) and critters caught (the 37 Safari critters' bestiary counts). Read
 * when you join SkyBlock, then every ten minutes and after each run.
 */
object OwnStats {
    private const val REFRESH_MILLIS = 10 * 60_000L
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/own-stats")
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals own stats").apply { isDaemon = true } }
    @Volatile var dex: Set<String> = emptySet()
        private set
    @Volatile var runs = 0L
        private set
    @Volatile var caught = 0L
        private set
    /** Null means unknown; an explicitly empty API dex is still a known snapshot. */
    @Volatile var dexSnapshot: DexSnapshot? = null
        private set
    private val responses = ProfileResponseGuard()
    private var selectedOwner: String? = null
    private var selectedProvider: ApiProvider? = null
    private var fetchedAt = 0L
    private var fetching = false

    /** Read again soon, e.g. after a run ends. */
    fun refreshSoon() { fetchedAt = 0 }

    fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        val provider = HypixelApi.provider
        val uuid = client.player?.uuid?.toString()
        if (uuid != selectedOwner || provider != selectedProvider) {
            reset()
            selectedOwner = uuid
            selectedProvider = provider
        }
        if (fetching || now - fetchedAt < REFRESH_MILLIS || !provider.enabled || !SkyblockSidebar.inSkyblock(client) || uuid == null) return
        val token = responses.begin(uuid, provider)
        fetching = true; fetchedAt = now
        executor.submit {
            val result = runCatching { HypixelApi.getSafariProfile(uuid) }
            client.execute {
                if (!responses.accepts(token, client.player?.uuid?.toString(), HypixelApi.provider)) return@execute
                fetching = false
                result.onFailure { logger.info("Own Safari stats lookup failed: {}", it.message) }.onSuccess { profile ->
                    val scope = profile.profileId?.takeIf { it.isNotBlank() }?.let { AchievementScope(uuid, it) }
                    if (scope?.key != dexSnapshot?.scope?.key) { runs = 0; caught = 0 }
                    dex = profile.caught.toSet()
                    dexSnapshot = scope?.let { DexSnapshot(it, dex.toSet(), System.currentTimeMillis()) }
                    Achievements.refreshDex(dexSnapshot)
                    profile.runs?.let { runs = it }
                    profile.catches?.let { caught = it }
                    val earned = Achievements.afterApiStats()
                    if (earned > 0) client.player?.sendSystemMessage(LocalChat.component(
                        "[SM] Your Hypixel profile earns §e$earned§r achievement${if (earned == 1) "" else "s"}. See /sm achievements."))
                }
            }
        }
    }

    fun reset() {
        responses.invalidate()
        Achievements.refreshDex(null)
        fetching = false; fetchedAt = 0; dex = emptySet(); dexSnapshot = null; runs = 0; caught = 0
        selectedOwner = null; selectedProvider = null
    }
}
