package net.johnceo.sparklingmutuals.safari.automation

import java.util.concurrent.CompletableFuture
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.party.PartyRosterSignals
import net.johnceo.sparklingmutuals.safari.SafariEntryPresence
import net.minecraft.client.Minecraft

/** Only scoped party commands are retained. Resolve names off-thread, then check the current UUID roster. */
object SafariPartyInput {
    private val queue = EntryPartyInbox()
    private val ids = mutableMapOf<String, String>()
    private val resolving = mutableSetOf<String>()
    private val retryAt = mutableMapOf<String, Long>()
    private var epoch = 0L
    private var requestedAt = 0L
    val pending get() = queue.pending
    val pendingKick get() = queue.pendingKick
    val pendingDowntime get() = queue.pendingDowntime
    val overflowReadiness get() = queue.overflowReadiness
    val overflowKick get() = queue.overflowKick
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message.string) }
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            Minecraft.getInstance().execute { receive(message.string) }
        }
    }
    private fun receive(text: String) {
        val client = Minecraft.getInstance()
        if (PartyRosterSignals.changed(text)) {
            // Membership notices are server system lines, never strings quoted within party chat.
            if (SafariDowntime.partyResetSignal(text)) resetPending()
            SafariDowntime.chat(text, SafariManagerGuard.activeRun(client), client.player?.name?.string.orEmpty())
            return
        }
        val chat = PartyChat.parse(text) ?: return
        // Reports only suppress duplicate announcements; they never release a readiness or kick hold.
        SafariDowntime.observeAnnouncement(chat.body)
        val downtime = ConfigManager.downtimeCommand &&
            (chat.body.matches(Regex("^!dt(?:\\s+.*)?$", RegexOption.IGNORE_CASE)) ||
                chat.body.equals("r", true) || chat.body.equals("Ticket earned!", true))
        val kick = ConfigManager.holdManagerOnKick && SafariManagerGuard.waitingInSafari(client) &&
            chat.body.contains("kick", true)
        if (!downtime && !kick) return
        // Duplicate delivery on GAME/CHAT is idempotent while identity is pending.
        queue.add(EntryPartyMessage(text.take(1024), chat.sender.lowercase(), client.level, kick, downtime))
        tick(client)
    }
    fun tick(client: Minecraft) {
        if (!ConfigManager.downtimeCommand) queue.disableDowntime()
        if (!ConfigManager.holdManagerOnKick) queue.disableKick()
        if (!ConfigManager.downtimeCommand && !ConfigManager.holdManagerOnKick) { resetPending(); return }
        val world = client.level ?: return
        queue.observeWorld(world)
        val connection = client.connection ?: return
        val self = client.player ?: return
        if (pending && !PartyManager.hasInfo) {
            val now = System.currentTimeMillis()
            if (now - requestedAt > 5000) { requestedAt = now; PartyManager.requestPartyInfo() }
            return
        }
        val roster = PartyManager.withLocal(self.uuid.toString())?.mapTo(HashSet(), SafariEntryPresence::normalize) ?: return
        connection.onlinePlayers.forEach { info ->
            val id = SafariEntryPresence.normalize(info.profile.id.toString())
            if (id in roster) ids[info.profile.name.lowercase()] = id
        }
        ids[self.name.string.lowercase()] = SafariEntryPresence.normalize(self.uuid.toString())
        SafariDowntime.pruneVerified(ids, roster)
        queue.entries().forEach { (key, item) ->
            val id = ids[item.sender]
            if (id != null) {
                queue.remove(key)
                if (id !in roster) return@forEach
                if (item.downtime) queue.validated(id,
                    PartyChat.parse(item.text)?.body?.let { it.equals("r", true) || it.equals("Ticket earned!", true) } == true, roster)
                if (item.downtime) SafariDowntime.chat(item.text, SafariManagerGuard.activeRun(client), self.name.string)
                    ?.let(SafariDowntime::queueAnnouncement)
                if (item.kickContext && item.world === client.level && SafariManagerGuard.waitingInSafari(client))
                    SafariManagerGuard.partyKick(PartyChat.parse(item.text)!!, id)
            } else if (item.sender !in resolving && System.currentTimeMillis() >= (retryAt[item.sender] ?: 0)) {
                resolving.add(item.sender)
                val generation = epoch
                CompletableFuture.supplyAsync { HypixelApi.getPlayerUuid(item.sender) }.whenComplete { resolved, error ->
                    client.execute {
                        if (epoch != generation) return@execute
                        resolving.remove(item.sender)
                        if (error == null) ids[item.sender] = SafariEntryPresence.normalize(resolved)
                        else retryAt[item.sender] = System.currentTimeMillis() + 10000
                        tick(client)
                    }
                }
            }
        }
        if (ids.size > 64) ids.keys.retainAll(queue.entries().map { it.second.sender }.toSet() + SafariDowntime.holders().split(", ").map(String::lowercase) + connection.onlinePlayers.map { it.profile.name.lowercase() })
    }
    private fun resetPending() { epoch++; queue.clear(); resolving.clear(); retryAt.clear() }
    /** Preserve requests elsewhere, but never replay an old session's unverified chat. */
    fun disconnect() { epoch++; resolving.clear(); retryAt.clear(); queue.transfer() }
}
