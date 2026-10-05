package net.johnceo.sparklingmutuals.commands

/** Stagger lookups without sending announcements; a final reply cancels the remaining clients. */
class ResponderElection(localUuid: String, members: List<String>, private val startedAt: Long) {
    private val local = localUuid.lowercase()
    private val roster = members.map(String::lowercase).distinct().sorted()
    private val rank = roster.indexOf(local)
    private var started = false
    private var answered = false

    fun expired(now: Long): Boolean = !started && now - startedAt > maxOf(30_000L, roster.size * 800L + 2000L)

    fun observeResponse(uuid: String?) {
        // Remote party members may be absent from the local tab list.
        if (uuid == null || uuid.lowercase() in roster) answered = true
    }

    fun shouldStartLookup(now: Long): Boolean {
        if (started || answered || rank < 0 || expired(now) || now - startedAt < 200L + rank * 800L) return false
        started = true
        return true
    }

    fun canPublish(now: Long): Boolean = started && !answered && !expired(now)
}
