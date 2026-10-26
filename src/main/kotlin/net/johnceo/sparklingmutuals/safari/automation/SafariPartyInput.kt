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
    private val displayNames = mutableMapOf<String, String>()
    fun nameOf(uuid: String): String? = ids.entries.firstOrNull {
        it.value == SafariEntryPresence.normalize(uuid)
    }?.key?.let { displayNames[it] ?: it }
    private val resolvingNames = mutableSetOf<String>()
    private val nameRetryAt = mutableMapOf<String, Long>()
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
        if (SafariDowntime.partyWarpMessage(text)) {
            val inCanyon = client.level != null && !SafariManagerGuard.activeRun(client) &&
                (net.johnceo.sparklingmutuals.safari.SafariIsland.currentMode.equals("foraging_3", true) ||
                    net.johnceo.sparklingmutuals.hud.SkyblockSidebar.areaName(client).equals("Torrhus Canyon", true))
            SafariDowntime.partyWarp(text, inCanyon, client.player?.uuid?.toString()?.let(PartyManager::isLeader) == true,
                client.player?.name?.string.orEmpty())
            return
        }
        if (SafariDowntime.travel(text)) return
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
                SafariDowntime.readyMessage(chat.body) || SafariDowntime.ticketMessage(chat.body))
        val kick = ConfigManager.holdManagerOnKick && SafariManagerGuard.inSafariInstance() &&
            chat.body.contains("kick", true)
        if (!downtime && !kick) return
        displayNames[chat.sender.lowercase()] = chat.sender
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
            if (id in roster) {
                ids[info.profile.name.lowercase()] = id
                displayNames[info.profile.name.lowercase()] = info.profile.name
            }
        }
        displayNames[self.name.string.lowercase()] = self.name.string
        ids[self.name.string.lowercase()] = SafariEntryPresence.normalize(self.uuid.toString())
        // A missing member may never have appeared in this connection's tab/player list.
        // Resolve the server-confirmed roster UUID directly; do not depend on them sending a command.
        roster.filter { nameOf(it) == null && it !in resolvingNames && System.currentTimeMillis() >= (nameRetryAt[it] ?: 0) }
            .forEach { id ->
                if (!id.matches(Regex("[0-9a-f]{32}"))) return@forEach
                val uuid = java.util.UUID.fromString("${id.take(8)}-${id.substring(8, 12)}-${id.substring(12, 16)}-${id.substring(16, 20)}-${id.substring(20)}")
                resolvingNames.add(id)
                val generation = epoch
                CompletableFuture.supplyAsync { client.services().sessionService().fetchProfile(uuid, false)?.profile() }
                    .whenComplete { profile, error -> client.execute {
                        if (epoch != generation) return@execute
                        resolvingNames.remove(id)
                        val currentRoster = PartyManager.withLocal(client.player?.uuid?.toString().orEmpty())
                            ?.mapTo(HashSet(), SafariEntryPresence::normalize).orEmpty()
                        if (error == null && profile != null && SafariEntryPresence.normalize(profile.id.toString()) == id &&
                            profile.name.matches(Regex("[A-Za-z0-9_]{1,16}"))) {
                            if (id in currentRoster) {
                                ids[profile.name.lowercase()] = id
                                displayNames[profile.name.lowercase()] = profile.name
                            }
                        } else nameRetryAt[id] = System.currentTimeMillis() + 10000
                    } }
            }
        SafariDowntime.pruneVerified(ids, roster)
        queue.entries().forEach { (key, item) ->
            val id = ids[item.sender]
            if (id != null) {
                queue.remove(key)
                if (id !in roster) return@forEach
                if (item.downtime) queue.validated(id,
                    PartyChat.parse(item.text)?.body?.let { SafariDowntime.readyMessage(it) || SafariDowntime.ticketMessage(it) } == true, roster)
                if (item.downtime) SafariDowntime.chat(item.text, SafariManagerGuard.activeRun(client), self.name.string,
                    if (PartyManager.getMembers().any { PartyManager.isLeader(it) && SafariEntryPresence.normalize(it) == id })
                        roster.map { nameOf(it) ?: it } else emptyList())
                    ?.let(SafariDowntime::queueAnnouncement)
                if (item.kickContext && item.world === client.level && SafariManagerGuard.inSafariInstance())
                    SafariManagerGuard.partyKick(PartyChat.parse(item.text)!!, item.receivedAt)
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
        if (ids.size > 64) ids.keys.retainAll(queue.entries().map { it.second.sender }.toSet() + SafariDowntime.holders().split(", ").map(String::lowercase) + connection.onlinePlayers.map { it.profile.name.lowercase() } + ids.filterValues { it in roster }.keys)
        displayNames.keys.retainAll(ids.keys + queue.entries().map { it.second.sender })
    }
    private fun resetPending() { epoch++; queue.clear(); resolving.clear(); retryAt.clear(); resolvingNames.clear(); nameRetryAt.clear() }
    /** Preserve requests elsewhere, but never replay an old session's unverified chat. */
    fun disconnect() { epoch++; resolving.clear(); retryAt.clear(); resolvingNames.clear(); nameRetryAt.clear(); queue.transfer() }
}
