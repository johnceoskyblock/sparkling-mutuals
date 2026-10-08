package net.johnceo.sparklingmutuals.party

/** Unknown membership is distinct from a server-confirmed solo player. */
class PartyRosterState {
    var members = emptyList<String>()
        private set
    var known = false
        private set
    fun update(ids: List<String>) { members = ids.map(String::lowercase).distinct(); known = true }
    fun invalidate() { known = false }
    fun withLocal(local: String): Set<String>? = if (known) (members + local.lowercase()).toSet() else null
    fun reset() { members = emptyList(); known = false }
}

/** Membership messages only: ordinary chat and invitations do not change the roster. */
object PartyRosterSignals {
    private val name = "(?:\\[[^]\\r\\n]+]\\s*)*[A-Za-z0-9_]{1,16}"
    private val messages = listOf(
        Regex("^$name (?:joined the party[.!]|has left the party[.!]|has been removed from the party[.!]|has disbanded the party[.!])$"),
        Regex("^You have joined $name's party!$"),
        Regex("^You (?:left the party[.!]|have been kicked from the party(?: by $name[.!]?)?)$"),
        Regex("^The party was disbanded(?:[.!]| because .+)$"),
        Regex("^You are not (?:currently )?in a party[.!]?$"))
    fun changed(text: String) = messages.any { it.matches(text.replace(Regex("§."), "").trim()) }
}
