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
        val mode: SafariMode = SafariFullClear.mode, var task: Future<*>? = null, var chunks: List<String>? = null)
    private val pending = mutableMapOf<String, Pending>()
    private val recent = mutableMapOf<String, Long>()
    private val requests = mutableMapOf<String, PartyRequest>()
    private val history = PartyResponseHistory()
    private data class QueuedReply(val token: String, val body: String, val roster: List<String>, val mode: SafariMode,
        val cursor: Long, val part: Int)
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
        // Keep actual result bodies; a different result for the same command cannot cancel ours.
        val parsed = PartyCommand.fromChat(text) ?: return
        val request = parsed.copy(command = parsed.command.forResponder(Minecraft.getInstance().player?.name?.string ?: return))
        if (!request.command.allowed()) return
        val chatCursor = history.cursor
        if (now - (recent[request.token] ?: 0) < 10_000) return
        recent[request.token] = now
        requests[request.token] = request
        pending.remove(request.token)?.task?.cancel(false)
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        val mode = SafariFullClear.mode
        PartyManager.withPartyInfo {
            if (client.connection !== connection || !ConfigManager.partyCommandsEnabled ||
                requests[request.token] !== request || mode != SafariFullClear.mode) return@withPartyInfo
            val local = client.player?.uuid?.toString() ?: return@withPartyInfo
            val roster = PartyManager.withLocal(local)?.toList() ?: return@withPartyInfo
            pending[request.token] = Pending(request, roster, ResponderElection(local, roster, now), chatCursor, mode)
        }
    }

    private fun sameParty(client: Minecraft, roster: List<String>) =
        client.player?.uuid?.toString()?.let(PartyManager::withLocal) == roster.toSet()

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (!ConfigManager.partyCommandsEnabled) { cancelPending(); return }
        val now = System.currentTimeMillis()
        if (replies.isNotEmpty()) {
            val reply = replies.first()
            if (reply.mode != SafariFullClear.mode || !sameParty(client, reply.roster) ||
                history.containsResult(reply.body, reply.cursor, reply.roster)) replies.removeFirst()
            else if (reply.part == 0 || now - sentReplyAt >= 1500) {
                replies.removeFirst()
                client.player?.connection?.sendCommand("pc ${reply.body}")
                sentReplyAt = now
            }
        }
        recent.entries.removeIf { now - it.value > 40_000 && it.key !in pending }
        requests.keys.retainAll(recent.keys)
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (token, state) = iterator.next()
            if (state.mode != SafariFullClear.mode || !state.request.command.allowed() || state.election.expired(now) || !sameParty(client, state.roster)) {
                state.task?.cancel(false)
                iterator.remove(); continue
            }
            val chunks = state.chunks
            if (chunks != null && state.election.canPublish(now)) {
                chunks.forEachIndexed { part, body ->
                    if (!history.containsResult(body, state.chatCursor, state.roster))
                        replies.addLast(QueuedReply(token, body, state.roster, state.mode, state.chatCursor, part))
                }
                iterator.remove()
            } else if (state.election.shouldStartLookup(now)) {
                val connection = client.connection
                val key = ConfigManager.apiKey
                val localName = client.player!!.name.string
                state.task = executor.submit {
                    // Network calls stay off the client thread; the shared cache is key-scoped.
                    val result = PartyLookupResult.fetch(state.request.command, key) {
                        SafariLookup.run(state.request.command, state.roster, localName)
                    }
                    client.execute {
                        if (pending[token] === state &&
                            client.connection === connection && ConfigManager.partyCommandsEnabled && state.mode == SafariFullClear.mode && state.request.command.allowed() &&
                            sameParty(client, state.roster)) {
                            if (result.successful) {
                                state.chunks = PartyReplyChunks.split(result.partyMessage!!)
                                state.election.lookupReady(System.currentTimeMillis())
                            } else {
                                pending.remove(token)
                                client.player?.sendSystemMessage(Component.literal(result.text))
                            }
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
    fun reset() { cancelPending(); recent.clear(); requests.clear(); history.clear(); sentReplyAt = 0 }
}
