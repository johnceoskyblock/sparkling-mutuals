package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.commands.PartyChat

/** Candidate-arrival diagnostics only. A location claim is not an admission or reach guarantee. */
class EntryDryRunEnvironment {
    private var connection: Any? = null
    private var level: Any? = null
    private var connectionId = 0L
    private var worldId = 0L
    private var generation = 0L
    private var location: Triple<String?, String?, String>? = null
    private var party: Triple<UUID, Set<UUID>, Long>? = null
    var arrivedAt = 0L
        private set

    fun matchesWorld(claim: EntryContext, connection: Any?, level: Any?) =
        connection != null && level != null && this.connection === connection && this.level === level &&
            claim.connection == connectionId && claim.world == worldId

    fun world(connection: Any?, world: Any?, now: Long) {
        if (this.connection === connection && level === world) return
        if (this.connection !== connection) { this.connection = connection; connectionId++ }
        level = world; worldId++; generation++; location = null; party = null; arrivedAt = now
    }

    fun location(mode: String?, server: String?, realm: String) {
        val next = Triple(mode, server, realm)
        if (location != null && location != next) generation++
        location = next.takeIf { connection != null && level != null }
    }

    fun context(account: UUID, name: String, party: EntryParty?): EntryContext? {
        val next = party?.let { Triple(it.leader, it.members, it.continuity) }
        if (this.party != next) { generation++; this.party = next }
        val info = location ?: return null
        val server = info.second ?: return null
        if (connection == null || level == null || !info.first.equals("safari", true) ||
            !EntryWire.server.matches(server) || !EntryWire.realm(info.third) ||
            !EntryWire.ign.matches(name) || party == null || account !in party.members) return null
        return EntryContext(account, name, connectionId, worldId, worldId, info.third, server,
            EntryParty(party.leader, party.members, generation))
    }
}

/** Caller must supply only non-overlay server GAME messages, never ordinary player CHAT. */
object EntryPartySender {
    fun resolve(text: String, profiles: List<EntryPeer>, members: Set<UUID>): Pair<EntryPeer, EntryMessage>? {
        if (text.length > 1024 || '\n' in text || '\r' in text) return null
        val chat = PartyChat.parse(text) ?: return null
        val message = SparklingEntryProtocol.parse(chat.body) ?: return null
        val peer = profiles.filter { it.name.equals(chat.sender, true) }.singleOrNull() ?: return null
        if (peer.id !in members) return null
        return peer to message
    }
}
