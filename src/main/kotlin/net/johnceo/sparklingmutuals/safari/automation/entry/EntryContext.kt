package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.Collections
import java.util.UUID

/** Local evidence only: these generations must never be compared across clients. */
class EntryParty(val leader: UUID, members: Set<UUID>, val continuity: Long) {
    val members: Set<UUID> = Collections.unmodifiableSet(LinkedHashSet(members))
    init { require(this.members.size in 1..4 && leader in this.members && continuity >= 0) }
    override fun equals(other: Any?) = other is EntryParty && leader == other.leader && members == other.members && continuity == other.continuity
    override fun hashCode() = 31 * (31 * leader.hashCode() + members.hashCode()) + continuity.hashCode()
}

data class EntryContext(
    val account: UUID, val selfName: String, val connection: Long, val world: Long,
    val arrival: Long, val realm: String, val server: String, val party: EntryParty
) {
    init {
        require(account in party.members && EntryWire.ign.matches(selfName))
        require(connection >= 0 && world >= 0 && arrival >= 0)
        require(EntryWire.realm(realm) && EntryWire.server.matches(server))
    }
}

data class EntryPeer(val id: UUID, val name: String) {
    init { require(EntryWire.ign.matches(name)) }
}

internal object EntryWire {
    val ign = Regex("[A-Za-z0-9_]{1,16}")
    val token = Regex("[a-f0-9]{32}")
    val server = Regex("[A-Za-z0-9_-]{1,64}")
    fun realm(value: String) = value == "main" || value == "alpha"
    fun fields(nonce: String, realm: String, serverName: String) = token.matches(nonce) && realm(realm) && server.matches(serverName)
}

/** Accepts elapsed milliseconds. Overflow and rollback cannot extend a permission. */
internal class EntryClock {
    private var previous = 0L
    private var failed = false
    fun accept(now: Long): Boolean {
        if (now < previous || now < 0) failed = true
        if (!failed) previous = now
        return !failed
    }
    fun reset(now: Long) { previous = now.coerceAtLeast(0); failed = now < 0 }
}

internal fun entryDeadline(now: Long, duration: Long): Long =
    if (now > Long.MAX_VALUE - duration) Long.MAX_VALUE else now + duration
