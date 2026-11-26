package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CaptureProgressTest {
    @Test fun `only personal captures after the snapshot count in a run that crosses the cutoff`() {
        val run = RunRecord(start = 1_000, length = 5_000, caught = 3, lootShares = 12,
            captureOffsetsMillis = listOf(500, 1_000, 4_000))
        assertEquals(1L, CaptureProgress.savedCaptures(listOf(run), 2_000, Long.MAX_VALUE))
        assertEquals(1L, CaptureProgress.savedCaptures(listOf(run), 2_000, 5_000))
        assertEquals(0L, CaptureProgress.savedCaptures(listOf(run), 2_000, 4_999))

        val newerLegacyRun = RunRecord(start = 6_000, length = 2_000, caught = 6, lootShares = 40)
        assertEquals(7L, CaptureProgress.savedCaptures(listOf(run, newerLegacyRun), 2_000, Long.MAX_VALUE))
        val withoutSnapshot = CaptureProgress.total(null) { CaptureProgress.savedCaptures(listOf(run, newerLegacyRun), it, Long.MAX_VALUE) }
        assertEquals(9L, withoutSnapshot)
    }

    @Test fun `refetches carry captures the API has yet to include and absorb them when it catches up`() {
        val personalCaptures = listOf(1_100L, 1_200L, 2_100L)
        fun between(after: Long, through: Long) = personalCaptures.count { it > after && it <= through }.toLong()
        fun observed(at: Long, n: Long, profile: String = "main") = CaptureSnapshot(profile, at, mapOf(SafariBiome.FOREST to n), 20)
        val first = observed(1_000, 9_998)
        assertEquals(10_001L, CaptureProgress.total(first) { between(it, Long.MAX_VALUE) })
        val unchanged = observed(2_000, 9_998).reconcile(first, ::between)
        assertEquals(2L, unchanged.unreflected)
        assertEquals(10_001L, CaptureProgress.total(unchanged) { between(it, Long.MAX_VALUE) })
        val partial = observed(2_200, 10_000).reconcile(unchanged, ::between)
        assertEquals(1L, partial.unreflected)
        val updated = observed(2_300, 10_001).reconcile(partial, ::between)
        assertEquals(0L, updated.unreflected)
        assertEquals(10_001L, CaptureProgress.total(updated) { between(it, Long.MAX_VALUE) })
        assertSame(updated, observed(2_300, 9_998).reconcile(updated, ::between))
        assertEquals(0L, observed(2_400, 1, profile = "other").reconcile(partial, ::between).unreflected)
    }

    @Test fun `bestiary fallback supports milestones without masquerading as API biome captures`() {
        val snapshot = CaptureSnapshot("main", 1_000, emptyMap(), 9_999)
        assertNull(snapshot.apiTotal)
        assertEquals(10_000L, CaptureProgress.total(snapshot) { 1 })
    }
}
