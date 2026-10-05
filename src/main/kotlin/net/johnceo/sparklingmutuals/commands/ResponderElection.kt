package net.johnceo.sparklingmutuals.commands

/** One staggered claim normally suffices; a settling window resolves overlapping claims. */
class ResponderElection(localUuid: String, members: List<String>, private val startedAt: Long) {
    private val local = localUuid.lowercase()
    private val roster = members.map(String::lowercase).distinct().sorted()
    private val rank = roster.indexOf(local)
    private val candidates = sortedSetOf<String>()
    private var lastClaimAt = 0L
    private var responded = false

    fun expired(now: Long): Boolean = now - startedAt > maxOf(30_000L, roster.size * 800L + 2000L)

    fun shouldClaim(now: Long): Boolean = rank >= 0 && !expired(now) && candidates.isEmpty() &&
        now - startedAt >= 200L + rank * 800L

    fun observeClaim(uuid: String, now: Long) {
        val candidate = uuid.lowercase()
        if (candidate in roster && candidates.add(candidate)) lastClaimAt = now
    }

    fun shouldRespond(now: Long): Boolean {
        if (responded || expired(now) || candidates.firstOrNull() != local || now - lastClaimAt < 1000) return false
        responded = true
        return true
    }
}
