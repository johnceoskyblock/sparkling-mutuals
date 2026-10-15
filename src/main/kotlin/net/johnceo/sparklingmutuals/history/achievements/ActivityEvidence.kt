package net.johnceo.sparklingmutuals.history.achievements

/** Durable observed lower bounds, stored with achievements. Never reconstructs pre-install activity. */
class ActivityEvidence {
    data class Admission(val scope: AchievementScope? = null, val at: Long = 0)
    data class Contest(val playerUuid: String = "", val window: Long = 0, val scope: AchievementScope? = null, val at: Long = 0)
    data class Interval(var startedAt: Long = 0, var tickets: Int = 0,
        val id: String = java.util.UUID.randomUUID().toString(), val unresolved: MutableSet<String> = mutableSetOf())
    data class Entry(val scope: AchievementScope? = null, val intervalId: String? = null)
    private val entries = linkedMapOf<String, Entry>()
    private val admissionEvents = linkedMapOf<String, Admission>()
    private val contestEvents = linkedMapOf<String, Contest>()
    private val intervals = linkedMapOf<String, Interval>()
    var coverageStart: Long? = null
        private set
    /** No lifetime total is inferred from installing this collector. */
    val historicalCoverageComplete = false
    val unscopedAdmissions: Int get() = admissionEvents.values.count { it.scope == null }

    /** A confirmed entry without an acknowledgement is unknown spending, never zero. */
    fun recordEntry(scope: AchievementScope?, runId: String, at: Long): Boolean {
        if (runId.isBlank() || runId in entries || runId in admissionEvents) return false
        val interval = scope?.let { intervals[it.key] }
        entries[runId] = Entry(scope, interval?.id)
        if (scope == null) invalidateIntervals() else interval?.unresolved?.add(runId)
        observed(at)
        return true
    }

    fun recordAdmission(scope: AchievementScope, runId: String, at: Long): Boolean = admission(scope, runId, at)
    fun recordUnscopedAdmission(runId: String, at: Long): Boolean = admission(null, runId, at)
    private fun admission(scope: AchievementScope?, runId: String, at: Long): Boolean {
        if (runId.isBlank() || runId in admissionEvents) return false
        admissionEvents[runId] = Admission(scope, at)
        observed(at)
        if (scope == null) invalidateIntervals() else intervals[scope.key]?.let { interval ->
            val entry = entries[runId]
            // A late acknowledgement belongs to the interval containing its entry, not the current one.
            if (entry == null || entry.scope?.key == scope.key && entry.intervalId == interval.id) {
                interval.unresolved.remove(runId)
                interval.tickets++
            }
        }
        return true
    }
    /** Personal reward identity is player + absolute window, independent of current profile or tier. */
    fun recordContest(playerUuid: String, window: Long, scope: AchievementScope?, at: Long): Boolean {
        val player = normalizePlayer(playerUuid)
        if (player.isBlank() || window < 0 || scope != null && normalizePlayer(scope.playerUuid) != player) return false
        val key = "$player:$window"
        if (key in contestEvents || contestEvents.values.any { it.playerUuid == player && it.window >= window }) return false
        contestEvents[key] = Contest(player, window, scope, at)
        observed(at)
        return true
    }
    private fun observed(at: Long) { coverageStart = minOf(coverageStart ?: at, at) }
    fun admissions(scope: AchievementScope): Int = admissionEvents.values.count { it.scope?.key == scope.key }
    fun contests(scope: AchievementScope): Int = contestEvents.values.count { it.scope?.key == scope.key }
    /** Coverage belongs to the selected profile; another profile's event cannot establish it. */
    fun coverageStart(scope: AchievementScope): Long? =
        (admissionEvents.values.filter { it.scope?.key == scope.key }.map { it.at } +
            contestEvents.values.filter { it.scope?.key == scope.key }.map { it.at } +
            listOfNotNull(intervals[scope.key]?.startedAt)).minOrNull()
    /** Modeled reward at 1.25 tickets per qualifying contest; never actual spent tickets. */
    fun expectedRewardTickets(scope: AchievementScope): Double = contests(scope) * 1.25
    fun ticketsSinceOwnSparkling(scope: AchievementScope): Int? = intervals[scope.key]?.takeIf { it.unresolved.isEmpty() }?.tickets
    /** Returns eligibility only; award versioning and notification policy belong to the achievement facade. */
    fun recordOwnSparkling(scope: AchievementScope, at: Long): Boolean {
        val eligible = intervals[scope.key]?.let { at >= it.startedAt && it.unresolved.isEmpty() && it.tickets <= 5 } ?: false
        intervals[scope.key] = Interval(at)
        observed(at)
        return eligible
    }
    fun invalidateIntervals() { intervals.clear() }
}
