package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft
import java.util.concurrent.Executors
import java.util.concurrent.Future

object PartyCommands {
    private data class Pending(val request: PartyRequest, val roster: List<String>, val election: ResponderElection,
        var task: Future<*>? = null)
    private val pending = mutableMapOf<String, Pending>()
    private val recent = mutableMapOf<String, Long>()
    private val requests = mutableMapOf<String, PartyRequest>()
    private val answered = mutableSetOf<String>()
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
        // Also record replies arriving while HMAPI is still refreshing the party roster.
        requests.forEach { (token, request) ->
            if (request.command.matchesResponse(chat.body)) {
                val state = pending[token]
                if (state != null && senderUuid != null && senderUuid !in state.roster) return@forEach
                state?.election?.observeResponse(senderUuid)
                answered.add(token)
                pending.remove(token)?.task?.cancel(false)
            }
        }
        val request = PartyCommand.fromChat(text) ?: return
        if (now - (recent[request.token] ?: 0) < 10_000) return
        recent[request.token] = now
        requests[request.token] = request
        answered.remove(request.token)
        pending.remove(request.token)?.task?.cancel(false)
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        PartyManager.refreshPartyInfo {
            if (client.connection !== connection || !ConfigManager.partyCommandsEnabled ||
                request.token in answered || requests[request.token] !== request) return@refreshPartyInfo
            val roster = PartyManager.getMembers()
            val local = client.player?.uuid?.toString() ?: return@refreshPartyInfo
            if (local !in roster) return@refreshPartyInfo
            val election = ResponderElection(local, roster, System.currentTimeMillis())
            pending[request.token] = Pending(request, roster, election)
        }
    }

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (!ConfigManager.partyCommandsEnabled) { cancelPending(); return }
        val now = System.currentTimeMillis()
        recent.entries.removeIf { now - it.value > 40_000 && it.key !in pending }
        requests.keys.retainAll(recent.keys)
        answered.retainAll(recent.keys)
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (token, state) = iterator.next()
            if (state.election.expired(now) || state.roster.toSet() != PartyManager.getMembers().toSet()) {
                state.task?.cancel(false)
                iterator.remove(); continue
            }
            if (state.election.shouldStartLookup(now)) {
                val connection = client.connection
                val key = ConfigManager.apiKey
                state.task = executor.submit {
                    // Cache access and network calls stay on this single worker.
                    if (key != lastKey) { SafariLookup.clearCache(); lastKey = key }
                    val result = try { SafariLookup.run(state.request.command, state.roster) }
                    catch (failure: ApiFailure) { state.request.command.failureResult(failure.message.orEmpty()) }
                    catch (_: Exception) { state.request.command.failureResult("Lookup failed unexpectedly. Please try again.") }
                    client.execute {
                        if (pending[token] === state && state.election.canPublish(System.currentTimeMillis()) &&
                            client.connection === connection && ConfigManager.partyCommandsEnabled &&
                            state.roster.toSet() == PartyManager.getMembers().toSet()) {
                            pending.remove(token)
                            answered.add(token)
                            client.player?.connection?.sendCommand("pc $result")
                        }
                    }
                }
            }
        }
    }

    private var lastKey = ""
    private fun cancelPending() {
        pending.values.forEach { it.task?.cancel(false) }
        pending.clear()
    }
    fun reset() { cancelPending(); recent.clear(); requests.clear(); answered.clear() }
}
