package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.safari.SafariBiome

internal data class CaptureSnapshot(
    val profileId: String,
    val fetchedAt: Long,
    val biomes: Map<SafariBiome, Long>,
    val bestiary: Long?,
    val unreflected: Long = 0
) {
    val apiTotal get() = biomes.takeIf { it.isNotEmpty() }?.values?.sum()
    val achievementTotal get() = apiTotal ?: bestiary

    fun reconcile(previous: CaptureSnapshot?, capturesBetween: (Long, Long) -> Long): CaptureSnapshot {
        if (previous == null || previous.profileId != profileId) return this
        if (fetchedAt <= previous.fetchedAt) return previous
        val before = previous.achievementTotal ?: return this
        val after = achievementTotal ?: return this
        if ((apiTotal == null) != (previous.apiTotal == null) || after < before) return this
        val pending = previous.unreflected + capturesBetween(previous.fetchedAt, fetchedAt)
        return copy(unreflected = (pending - (after - before)).coerceAtLeast(0))
    }
}

internal object CaptureProgress {
    fun offsets(times: List<Long>, start: Long): List<Int> = times.map { (it - start).toInt() }

    fun savedCaptures(runs: List<RunRecord>, after: Long, through: Long): Long = runs.sumOf { run ->
        val offsets = run.captureOffsetsMillis
        when {
            offsets != null && offsets.size == run.caught ->
                offsets.count { run.start + it > after && run.start + it <= through }.toLong()
            run.start > after && (through == Long.MAX_VALUE || run.length > 0 && run.start + run.length <= through) ->
                run.caught.toLong()
            else -> 0L
        }
    }

    fun total(snapshot: CaptureSnapshot?, capturesSince: (Long) -> Long): Long =
        snapshot?.achievementTotal?.let { it + snapshot.unreflected + capturesSince(snapshot.fetchedAt) }
            ?: capturesSince(Long.MIN_VALUE)
}
