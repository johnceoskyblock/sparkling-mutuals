package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GemzieChamberCheckTest {
    @TempDir lateinit var dir: Path
    private val message = "A rumbling sound can be heard, and the door at the back of the chamber opens..."
    private fun party() = PartySparklingState().apply {
        select(setOf("me"))
        accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - "Gemzie"))
    }

    @Test fun `chamber opening requires distinct Gemzie checks in both checking modes`() {
        ConfigManager.init(dir)
        for (mode in listOf(SafariMode.SPARKLING, SafariMode.UNIQUE)) {
            SafariFullClear.select(mode)
            var now = 1000L
            val run = SafariRun(0) { now }
            val state = party()
            run.sparklingChecks.chat("§a$message")
            now = 5999
            assertFalse(run.sparklingComplete("Gemzie", state))
            assertEquals("0/3 checked", SafariPanels.fixedCounter("Gemzie", run, mode, state))
            // Opening the chamber and waiting cannot establish checks.
            run.sparklingChecks.chat(message)
            now = 6000
            assertFalse(run.sparklingComplete("Gemzie", state, localOnly = true))
            assertEquals("0/3 checked", SafariPanels.fixedCounter("Gemzie", run, mode, state))
            val sightings = List(3) { "Gemzie" to java.util.UUID.randomUUID() }
            run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, sightings.take(2))
            assertFalse(run.sparklingComplete("Gemzie", state, localOnly = true))
            run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, sightings)
            assertTrue(run.sparklingComplete("Gemzie", state, localOnly = true))
            assertEquals("3/3 checked", SafariPanels.fixedCounter("Gemzie", run, mode, state))
            assertEquals(0, run.count("Gemzie"))
            assertFalse(run.uniqueComplete("Gemzie", state))
            assertFalse(run.captureComplete("Gemzie"))
            assertEquals("0/3 captured", SafariPanels.fixedCounter("Gemzie", run, SafariMode.FULL_CLEAR, state))
            run.visitBiome(SafariBiome.FOREST)
            run.visitBiome(SafariBiome.CAVERN)
            run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0, emptyList())
            assertTrue(run.sparklingComplete("Gemzie", state))
            if (mode == SafariMode.SPARKLING) assertTrue(SafariPanels.missingCritters(run, SafariBiome.CAVERN, state).isEmpty())
            run.record(SafariCatch(SafariRoster.named("Gemzie")!!))
            assertTrue(run.uniqueComplete("Gemzie", state))
        }
    }

    @Test fun `player quoted chamber messages and a new run cannot inherit chamber completion`() {
        var now = 1000L
        val run = SafariRun(0) { now }
        for (raw in listOf("Party > Friend: $message", "Friend: $message")) {
            SafariMessages.lines(raw, run.sparklingChecks, run).forEach { run.sparklingChecks.chat(it) }
        }
        now = 10000
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        run.sparklingChecks.chat(message)
        now = 15000
        assertFalse(run.sparklingChecks.checked("Gemzie"))
        run.sparklingChecks.scan(SafariBiome.CAVERN, -141.0, 51.0,
            List(3) { "Gemzie" to java.util.UUID.randomUUID() })
        assertTrue(run.sparklingChecks.checked("Gemzie"))
        assertFalse(SafariRun(now) { now }.sparklingChecks.checked("Gemzie"))
    }
}
