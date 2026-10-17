package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HistoryServerPolicyTest {
    @Test fun `alpha address excludes run through scoreboard loss and departure`() {
        val state = HistoryServerState()
        val run = SafariRun(0)
        assertFalse(state.observe(listOf("§calpha.hypixel.net"), run))
        assertTrue(run.historyExcluded)
        assertFalse(state.observe(emptyList(), run))
        assertFalse(state.observe(emptyList()))
        assertFalse(state.observe(listOf("www.hypixel.net"), run), "alpha exclusion is permanent for this run")
        assertTrue(state.observe(listOf("www.hypixel.net"), SafariRun(1)), "later main runs save normally")
    }
    @Test fun `main and unknown scoreboards preserve existing recording behavior`() {
        val state = HistoryServerState()
        val run = SafariRun(0)
        assertTrue(state.observe(emptyList(), run))
        assertTrue(state.observe(listOf("WWW.HYPIXEL.NET"), run))
        assertTrue(state.observe(listOf("Party > Friend: alpha.hypixel.net"), run))
        assertFalse(run.historyExcluded)
        assertFalse(state.observe(listOf("alpha.hypixel.net"), run), "late alpha detection still excludes the run")
    }
}
