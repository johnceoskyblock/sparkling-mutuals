package net.johnceo.sparklingmutuals.history

/** Shared, read-only history filters. Unknown and AFK lengths do not distort the run average. */
object HistoryQueries {
    data class LiveLuck(val runs: Int, val sparklingDryStreak: Int, val macawDrought: Int, val macawRuns: Int = 0)
    fun liveLuck(runs: List<RunRecord>): LiveLuck {
        var sparkling = 0; var macaw = 0; var longestSparkling = 0; var longestMacaw = 0
        var liveRuns = 0; var macawRuns = 0
        runs.sortedBy { it.start }.forEach { run ->
            // An unobserved imported stretch cannot join two native streaks together.
            if (run.imported) { sparkling = 0; macaw = 0; return@forEach }
            liveRuns++
            sparkling = if (run.hasSparkling() || run.ownSparklings > 0) 0 else sparkling + 1
            longestSparkling = maxOf(longestSparkling, sparkling)
            val caughtMacaw = run.macaws > 0 || (run.catches["Macaw"] ?: 0) > 0
            // A zero reward count is not proof that a Macaw could have spawned. Old rows lack that evidence.
            if (caughtMacaw || run.macawChecked == true) {
                macawRuns++
                macaw = if (caughtMacaw) 0 else macaw + 1
                longestMacaw = maxOf(longestMacaw, macaw)
            } else macaw = 0
        }
        return LiveLuck(liveRuns, longestSparkling, longestMacaw, macawRuns)
    }
    /** An older complete run's duration is a safe upper bound when the exact capture time is unknown. */
    fun allTypesTime(run: RunRecord): Long? = if (run.party < net.johnceo.sparklingmutuals.safari.SafariRoster.all.size) null
        else run.allTypesMillis.takeIf { it >= 0 } ?: run.length.takeIf { it > 0 }

    data class PartyStreaks(val current: Int, val longest: Int, val currentUnique: Int, val longestUnique: Int)
    fun partyStreaks(runs: List<RunRecord>, entries: List<SparklingEntry>): PartyStreaks {
        var current = 0; var longest = 0; var unique = 0; var longestUnique = 0
        val discoveries = entries.filter { it.unique }.sortedBy { it.at }
        var cursor = 0
        runs.sortedBy { it.start }.forEach { run ->
            val end = run.start + run.length.coerceAtLeast(0)
            while (cursor < discoveries.size && discoveries[cursor].at < run.start) cursor++
            val newType = discoveries.getOrNull(cursor)?.at?.let { it <= end } == true
            if (run.imported) { current = 0; unique = 0; return@forEach }
            current = if (run.hasSparkling() || run.ownSparklings > 0) 0 else current + 1
            unique = if (newType) 0 else unique + 1
            longest = maxOf(longest, current); longestUnique = maxOf(longestUnique, unique)
        }
        return PartyStreaks(current, longest, unique, longestUnique)
    }
    const val MAX_RUN_MILLIS = 30 * 60_000L
    fun keepRunLength(length: Long) = length <= MAX_RUN_MILLIS
    fun averageLength(runs: List<RunRecord>): Long? = runs.filter { it.length in 1 until 600_000L }
        .takeIf { it.isNotEmpty() }?.let { valid -> valid.sumOf { it.length } / valid.size }
    fun runIndex(runs: List<RunRecord>, query: String): Int {
        val number = query.trim().removePrefix("#").toIntOrNull() ?: return -1
        return runs.indexOfFirst { it.number == number }
    }
    fun matchesCritter(entry: SparklingEntry, query: String) = entry.critter.contains(query.trim(), ignoreCase = true)
}
