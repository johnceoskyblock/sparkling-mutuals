package net.johnceo.sparklingmutuals.sync

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HotspotWinHistoryTest {
    @Test fun `a pending new visit retains confirmed wins for the same roster`() {
        val history = HotspotWinHistory()
        val members = setOf("self", "peer")
        val counts = mutableMapOf("self" to 2, "peer" to 1)
        history.remember(members, counts)
        counts.clear()
        repeat(3) { assertEquals(mapOf("self" to 2, "peer" to 1), history.counts(members.reversed().toSet())) }
        history.remember(members, mapOf("self" to 0, "peer" to 0))
        assertEquals(mapOf("self" to 0, "peer" to 0), history.counts(members))
    }

    @Test fun `changing party discards previous fairness instead of leaking it into another party`() {
        val history = HotspotWinHistory()
        history.remember(setOf("self", "peer"), mapOf("self" to 2, "peer" to 1))
        assertTrue(history.counts(setOf("self", "other")).isEmpty())
        assertTrue(history.counts(setOf("self", "peer")).isEmpty())
    }
}
