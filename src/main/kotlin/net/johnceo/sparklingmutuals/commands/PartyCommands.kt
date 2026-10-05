package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.party.PartyManager
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object PartyCommands {
    private data class Pending(val request: PartyRequest, val roster: List<String>, val election: ResponderElection)
    private data class Claim(val uuid: String, val time: Long)
    private val claimPattern = Regex("""^\[SM] Checking ([a-f0-9]{12}) ([a-f0-9-]{36}): .+$""")
    private val pending = mutableMapOf<String, Pending>()
    private val recent = mutableMapOf<String, Long>()
    private val claims = mutableMapOf<String, MutableList<Claim>>()
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
        val claim = claimPattern.matchEntire(chat.body)
        if (claim != null) {
            val token = claim.groupValues[1]
            val uuid = claim.groupValues[2]
            val senderInfo = Minecraft.getInstance().connection?.getPlayerInfoIgnoreCase(chat.sender)
            if (senderInfo != null && senderInfo.profile.id.toString() != uuid) return
            // Buffer claims until our HMAPI party refresh finishes.
            if (token in recent) {
                claims.getOrPut(token) { mutableListOf() }.add(Claim(uuid, now))
                pending[token]?.election?.observeClaim(uuid, now)
            }
            return
        }
        val request = PartyCommand.fromChat(text) ?: return
        if (now - (recent[request.token] ?: 0) < 10_000) return
        recent[request.token] = now
        claims.remove(request.token)
        val client = Minecraft.getInstance()
        val connection = client.connection ?: return
        PartyManager.refreshPartyInfo {
            if (client.connection !== connection || !ConfigManager.partyCommandsEnabled) return@refreshPartyInfo
            val roster = PartyManager.getMembers()
            val local = client.player?.uuid?.toString() ?: return@refreshPartyInfo
            if (local !in roster) return@refreshPartyInfo
            val election = ResponderElection(local, roster, System.currentTimeMillis())
            claims[request.token]?.forEach { election.observeClaim(it.uuid, it.time) }
            pending[request.token] = Pending(request, roster, election)
        }
    }

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.connection == null) { reset(); return }
        if (!ConfigManager.partyCommandsEnabled) { pending.clear(); return }
        val now = System.currentTimeMillis()
        recent.entries.removeIf { now - it.value > 40_000 }
        claims.keys.retainAll(recent.keys)
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (token, state) = iterator.next()
            if (state.election.expired(now) || state.roster.toSet() != PartyManager.getMembers().toSet()) {
                iterator.remove(); continue
            }
            if (state.election.shouldClaim(now)) {
                val uuid = client.player!!.uuid.toString()
                state.election.observeClaim(uuid, now)
                client.player!!.connection.sendCommand("pc [SM] Checking $token $uuid: ${state.request.command.text}")
            }
            if (state.election.shouldRespond(now)) {
                iterator.remove()
                val connection = client.connection
                val key = ConfigManager.apiKey
                executor.execute {
                    // Cache access and network calls stay on this single worker.
                    if (key != lastKey) { SafariLookup.clearCache(); lastKey = key }
                    val result = try { SafariLookup.run(state.request.command, state.roster) }
                    catch (failure: ApiFailure) { "[SM] ${failure.message}" }
                    catch (_: Exception) { "[SM] Lookup failed unexpectedly. Please try again." }
                    client.execute {
                        if (client.connection === connection && ConfigManager.partyCommandsEnabled &&
                            state.roster.toSet() == PartyManager.getMembers().toSet()) {
                            client.player?.connection?.sendCommand("pc $result")
                        }
                    }
                }
            }
        }
    }

    private var lastKey = ""
    fun reset() { pending.clear(); recent.clear(); claims.clear() }
}
