package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.history.achievements.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdmissionEvidenceTest {
    private val scope = AchievementScope("player", "profile")
    private val first = "[NPC] Safari Manager: Looks good to me. Have fun out there!"
    private val again = "[NPC] Safari Manager: I already saw your ticket, so you're free to go."

    @Test fun `both exact variants require entry and duplicate acknowledgements fan out once`() {
        for (line in listOf(first, again)) {
            val history = LiveHistorySession()
            val run = SafariRun(1000)
            var sparklingTickets = 0
            val evidence = AdmissionEvidence { _, _, _ -> history.onTicket(run); sparklingTickets++ }
            evidence.acknowledge(line, 1000)
            evidence.acknowledge(first, 1001)
            evidence.acknowledge(again, 1002)
            assertEquals(0, sparklingTickets)
            assertTrue(evidence.confirmEntry(run.runId, scope, 1100))
            history.sync(run) { }
            evidence.acknowledge(again, 1101)
            assertFalse(evidence.confirmEntry(run.runId, scope, 1102))
            assertEquals(1, sparklingTickets)
            assertEquals(1, history.tickets)
            evidence.leave()
            assertFalse(evidence.confirmEntry("next", scope, 1200), "bound duplicates must not queue next admission")
        }
    }
    @Test fun `acknowledgement just after entry belongs to that run`() {
        var count = 0
        val evidence = AdmissionEvidence { _, _, _ -> count++ }
        assertFalse(evidence.confirmEntry("one", scope, 1000))
        evidence.acknowledge(first, 1001)
        evidence.acknowledge(again, 1002)
        assertEquals(1, count)
    }
    @Test fun `same text can admit two distinct runs while revisits preserve identity`() {
        val ids = mutableListOf<String>()
        val evidence = AdmissionEvidence { id, _, _ -> ids += id }
        val ledger = SafariLedger()
        evidence.acknowledge(first, 1000)
        ledger.confirmEntry(1001)
        val id = ledger.current!!.runId
        evidence.confirmEntry(id, scope, 1001)
        ledger.update(SafariLocation.INSIDE, SafariBiome.FOREST, 1010)
        ledger.confirmEntry(1011)
        assertEquals(id, ledger.current!!.runId)
        assertFalse(evidence.confirmEntry(id, scope, 1011))
        ledger.leave(1020); evidence.leave()
        evidence.acknowledge(first, 2000)
        ledger.confirmEntry(2001)
        evidence.confirmEntry(ledger.current!!.runId, scope, 2001)
        assertEquals(2, ids.distinct().size)
    }
    @Test fun `stale failed and disconnected admissions never count`() {
        var count = 0
        val evidence = AdmissionEvidence { _, _, _ -> count++ }
        evidence.acknowledge(first, 0)
        assertFalse(evidence.confirmEntry("stale", scope, 120001))
        evidence.leave()
        evidence.acknowledge(first, 200000)
        evidence.disconnect()
        assertFalse(evidence.confirmEntry("reconnect", scope, 200001))
        evidence.acknowledge("Party > $first", 200002)
        assertEquals(0, count)
        evidence.leave()
        evidence.acknowledge(first, 300000)
        assertTrue(evidence.confirmEntry("boundary", scope, 420000))
    }
    @Test fun `repeated banners do not extend entry matching deadline and unscoped stays unscoped`() {
        val scopes = mutableListOf<AchievementScope?>()
        val evidence = AdmissionEvidence { _, owner, _ -> scopes += owner }
        evidence.confirmUnscopedEntry("unknown", 0)
        evidence.confirmEntry("unknown", scope, 119999)
        evidence.acknowledge(first, 120001)
        assertTrue(scopes.isEmpty())
        evidence.leave()
        evidence.acknowledge(first, 200000)
        assertTrue(evidence.confirmUnscopedEntry("raw", 200001))
        assertEquals(listOf<AchievementScope?>(null), scopes)
    }
    @Test fun `authoritative fanout updates both real histories once and persisted admission rejects replay`() {
        val oldDirectory = HistoryStore.directory
        HistoryStore.directory = java.nio.file.Files.createTempDirectory("admission-fanout")
        // Inspect the real live session in the test; no test-only production accessor is needed.
        val field = RunHistory::class.java.getDeclaredField("session").apply { isAccessible = true }
        val session = field.get(null) as LiveHistorySession
        try {
            TicketCounter.disconnect()
            session.sync(null) { }
            val run = SafariRun(1000)
            val before = SparklingLog.ticketsSinceSparkling
            TicketCounter.acknowledge(first, 1000)
            TicketCounter.confirmEntry(run, scope, 1001)
            session.sync(run) { }
            TicketCounter.acknowledge(again, 1002)
            TicketCounter.confirmEntry(run, scope, 1003)
            val originalFull = net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode
            val originalSparkling = net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode
            try {
                for (mode in SafariMode.entries) {
                    net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = mode == SafariMode.FULL_CLEAR
                    net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = mode == SafariMode.SPARKLING
                    assertEquals(mode, net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode)
                    for (biome in listOf(SafariBiome.FOREST, SafariBiome.ICY, SafariBiome.FOREST, SafariBiome.CAVERN, SafariBiome.FOREST)) {
                        run.visitBiome(biome)
                        TicketCounter.confirmEntry(run, scope, 1003)
                        session.sync(run) { fail("Mode or biome revisit must not end the run") }
                        assertEquals(1, session.tickets)
                        assertEquals(before + 1, SparklingLog.ticketsSinceSparkling)
                    }
                }
            } finally {
                net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = originalFull
                net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = originalSparkling
            }
            assertEquals(1, session.tickets)
            assertEquals(before + 1, SparklingLog.ticketsSinceSparkling)
            val saved = HistoryStore.load("achievements", Achievements.Data::class.java) { Achievements.Data() }
            assertFalse(requireNotNull(saved.activity).recordAdmission(scope, run.runId, 1004))
            TicketCounter.disconnect()
            TicketCounter.acknowledge(first, 2000)
            TicketCounter.confirmEntry(run, scope, 2001)
            assertEquals(1, session.tickets)
            assertEquals(before + 1, SparklingLog.ticketsSinceSparkling)
            val record = RunRecord(runId = run.runId)
            val restored = com.google.gson.Gson().fromJson(com.google.gson.Gson().toJson(record), RunRecord::class.java)
            assertEquals(run.runId, restored.runId)
            assertNull(com.google.gson.Gson().fromJson("{}", RunRecord::class.java).runId)
        } finally {
            TicketCounter.disconnect()
            session.sync(null) { }
            HistoryStore.directory = oldDirectory
        }
    }
}
