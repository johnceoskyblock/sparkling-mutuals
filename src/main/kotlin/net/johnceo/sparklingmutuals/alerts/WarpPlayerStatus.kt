package net.johnceo.sparklingmutuals.alerts

import java.util.concurrent.CompletableFuture
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiFailureKind
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft

/** Client-thread cache with one asynchronous group of at most 20 players in flight. */
internal object WarpPlayerStatus {
    const val DESCRIPTION = "Checks online status every 2 hours. Add your own Hypixel API key with /apikey. ShinyHunter does not provide player status."
    private const val REFRESH = 7_200_000L
    private data class Player(val uuid: String?, val resolvedAt: Long, val online: Boolean? = null, val checkedAt: Long? = null)
    private data class Batch(val players: Map<String, Player>, val pause: Boolean)
    private val cache = mutableMapOf<String, Player>()
    private var pending: CompletableFuture<Batch>? = null
    private var pendingKey = ""
    private var statusKey = ""
    private var nextBatchAt = 0L

    fun tick(client: Minecraft, now: Long) {
        val key = ConfigManager.apiKey.trim().takeIf { ConfigManager.autoPartyOnlineOnly }.orEmpty()
        if (statusKey != key) {
            statusKey = key
            cache.replaceAll { _, entry -> entry.copy(online = null, checkedAt = null) }
        }
        val connected = ConfigManager.warpAlertsEnabled && !ConfigManager.leechMode && ConfigManager.autoParty && client.connection != null && JoinWelcomeState.hypixel(client.currentServer?.ip)
        if (pending == null && (!connected || now < nextBatchAt)) return
        val names = WarpList.players().associateBy(String::lowercase)
        pending?.let { job ->
            if (!job.isDone) return
            pending = null
            val result = runCatching { job.join() }.getOrNull()
            if (result != null) {
                result.players.forEach { (name, entry) ->
                    if (name in names) cache[name] = if (pendingKey == key) entry else entry.copy(online = null, checkedAt = null)
                }
                if (result.pause) nextBatchAt = maxOf(nextBatchAt, now + 300_000)
            }
        }
        if (!connected || now < nextBatchAt) return
        cache.keys.retainAll(names.keys)
        val batch = names.filter { (id, name) ->
            val old = cache[id]
            old == null || old.uuid == null && now - old.resolvedAt >= REFRESH ||
                key.isNotEmpty() && !WarpList.settings(name).alwaysInvite && old.uuid != null &&
                (old.checkedAt == null || now - old.checkedAt >= REFRESH)
        }.entries.take(20).associate { (id, name) -> name to cache[id] }
        if (batch.isEmpty()) {
            nextBatchAt = now + 5_000
            return
        }
        val overrides = batch.keys.filter { WarpList.settings(it).alwaysInvite }.toSet()
        pendingKey = key
        nextBatchAt = now + 60_000
        pending = CompletableFuture.supplyAsync {
            var pause = false
            val updates = batch.map { (name, previous) ->
                val uuid = previous?.uuid ?: runCatching { HypixelApi.getPlayerUuid(name) }.getOrNull()
                var result = previous?.takeIf { it.uuid != null } ?: Player(uuid, System.currentTimeMillis())
                if (uuid != null && key.isNotEmpty() && name !in overrides && !pause) {
                    val online = try { HypixelApi.onlineStatus(uuid, key) }
                    catch (failure: ApiFailure) {
                        pause = failure.kind in setOf(ApiFailureKind.RATE_LIMIT, ApiFailureKind.INVALID_KEY)
                        null
                    }
                    result = result.copy(online = online, checkedAt = System.currentTimeMillis())
                }
                name.lowercase() to result
            }.toMap()
            Batch(updates, pause)
        }
    }

    fun eligible(name: String, now: Long): Boolean {
        if (!ConfigManager.autoPartyOnlineOnly || WarpList.settings(name).alwaysInvite) return true
        val entry = cache[name.lowercase()] ?: return false
        return statusKey.isNotEmpty() && statusKey == ConfigManager.apiKey.trim() && entry.online == true &&
            entry.checkedAt?.let { now - it < REFRESH } == true
    }

    fun ordered(critter: String): List<String> = WarpList.names(critter).sortedWith(
        compareBy<String> { WarpList.priority(it, critter) }
            .thenBy { cache[it.lowercase()]?.uuid ?: "~" }.thenBy(String::lowercase))
}
