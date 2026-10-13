package net.johnceo.sparklingmutuals.commands

/** Fetch immediately, then stagger ready replies by descending UUID without announcements. */
class ResponderElection(localUuid: String, members: List<String>, private val startedAt: Long) {
    private val local = localUuid.lowercase()
    private val roster = members.map(String::lowercase).distinct().sortedDescending()
    private val rank = roster.indexOf(local)
    private var started = false
    private var answered = false
    private var readyAt: Long? = null

    fun expired(now: Long): Boolean = !started && now - startedAt > maxOf(30_000L, roster.size * 800L + 2000L)

    fun observeResponse(uuid: String?) {
        // Remote party members may be absent from the local tab list.
        if (uuid == null || uuid.lowercase() in roster) answered = true
    }

    fun shouldStartLookup(now: Long): Boolean {
        if (started || answered || rank < 0 || expired(now)) return false
        started = true
        return true
    }

    fun lookupReady(now: Long) { if (started && readyAt == null) readyAt = now }
    fun canPublish(now: Long): Boolean = started && !answered && readyAt?.let { now - it >= rank * 500L } == true
}
