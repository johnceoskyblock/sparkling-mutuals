package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariFullClear
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors
import java.util.concurrent.Future

object PartyCommands {
    private data class Pending(val request: PartyRequest, val roster: List<String>, val election: ResponderElection, val chatCursor: Long,
        val mode: SafariMode = SafariFullClear.mode, var task: Future<*>? = null)
    private val pending = mutableMapOf<String, Pending>()
    private val recent = mutableMapOf<String, Long>()
    private val requests = mutableMapOf<String, PartyRequest>()
    private val answered = mutableSetOf<String>()
    private val history = PartyResponseHistory()
    private data class QueuedReply(val token: String, val body: String, val roster: Set<String>, val mode: SafariMode)
    private val replies = ArrayDeque<QueuedReply>()
    private var sentReplyAt = 0L
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "Sparkling Mutuals lookups").apply { isDaemon = true } }

    fun register() {
        // React to the server echo only: /pc and /p chat work without double-processing local messages.
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            Minecraft.getInstance().execute { receive(message.string) }
        }
    }

    private fun receive(text: String) {
        if (!ConfigManager.partyCommandsEnabled) return
        val chat = PartyChat.parse(text) ?: return
        val now = System.currentTimeMillis()
        val senderUuid = Minecraft.getInstance().connection?.getPlayerInfoIgnoreCase(chat.sender)?.profile?.id?.toString()
        history.record(chat.body, senderUuid)
        // Also record replies arriving while HMAPI is still refreshing the party roster.
        requests.forEach { (token, request) ->
            if (request.command.matchesResponse(chat.body)) {
                val state = pending[token]
                if (state != null && senderUuid != null && senderUuid !in state.roster) return@forEach
                state?.election?.observeResponse(senderUuid)
                val ownReply = chat.sender.equals(Minecraft.getInstance().player?.name?.string, true)
                if (!ownReply) replies.removeAll { it.token == token }
                answered.add(token)
                pending.remove(token)?.task?.cancel(false)
            }
        }
        val parsed = PartyCommand.fromChat(text) ?: return
        val request = parsed.copy(command = parsed.command.forResponder(Minecraft.getInstance().player?.name?.string ?: return))
        if (!request.command.allowed()) return
        val chatCursor = history.cursor
        if (now - (recent[request.token] ?: 0) < 10_000) return
        recent[request.token] = now
        requests[request.token] = request
        answered.remove(request.token)
        pending.remove(request.token)?.task?.cancel(false)
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        PartyManager.refreshPartyInfo {
            if (client.connection !== connection || !ConfigManager.partyCommandsEnabled ||
                request.token in answered || requests[request.token] !== request || !request.command.allowed()) return@refreshPartyInfo
            val roster = PartyManager.getMembers()
            if (history.hasReply(request.command, chatCursor, roster)) return@refreshPartyInfo
            val local = client.player?.uuid?.toString() ?: return@refreshPartyInfo
            if (local !in roster) return@refreshPartyInfo
            val election = ResponderElection(local, roster, System.currentTimeMillis())
            pending[request.token] = Pending(request, roster, election, chatCursor)
        }
    }

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (!ConfigManager.partyCommandsEnabled) { cancelPending(); return }
        val now = System.currentTimeMillis()
        if (replies.isNotEmpty() && now - sentReplyAt >= 1500) {
            val reply = replies.removeFirst()
            if (reply.mode == SafariFullClear.mode && reply.roster == PartyManager.getMembers().toSet()) {
                client.player?.connection?.sendCommand("pc ${reply.body}")
                sentReplyAt = now
            }
        }
        recent.entries.removeIf { now - it.value > 40_000 && it.key !in pending }
        requests.keys.retainAll(recent.keys)
        answered.retainAll(recent.keys)
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (token, state) = iterator.next()
            if (state.mode != SafariFullClear.mode || !state.request.command.allowed() || state.election.expired(now) || state.roster.toSet() != PartyManager.getMembers().toSet()) {
                state.task?.cancel(false)
                iterator.remove(); continue
            }
            if (state.election.shouldStartLookup(now)) {
                val connection = client.connection
                val key = ConfigManager.apiKey
                val localName = client.player!!.name.string
                state.task = executor.submit {
                    // Network calls stay off the client thread; the shared cache is key-scoped.
                    val result = PartyLookupResult.fetch(state.request.command, key) {
                        SafariLookup.run(state.request.command, state.roster, localName)
                    }
                    client.execute {
                        if (pending[token] === state && state.election.canPublish(System.currentTimeMillis()) &&
                            client.connection === connection && ConfigManager.partyCommandsEnabled && state.mode == SafariFullClear.mode && state.request.command.allowed() &&
                            state.roster.toSet() == PartyManager.getMembers().toSet() &&
                            !history.hasReply(state.request.command, state.chatCursor, state.roster)) {
                            pending.remove(token)
                            if (result.successful) {
                                answered.add(token)
                                PartyReplyChunks.split(result.partyMessage!!).forEach { body ->
                                    replies.addLast(QueuedReply(token, body, state.roster.toSet(), state.mode))
                                }
                            } else client.player?.sendSystemMessage(Component.literal(result.text))
                        }
                    }
                }
            }
        }
    }

    private fun cancelPending() {
        pending.values.forEach { it.task?.cancel(false) }
        pending.clear()
        replies.clear()
    }
    fun reset() { cancelPending(); recent.clear(); requests.clear(); answered.clear(); history.clear() }
}
