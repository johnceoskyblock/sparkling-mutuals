package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GazerRetainedChecksTest {
    @TempDir lateinit var dir: Path
    private fun party() = PartySparklingState().apply {
        select(setOf("local")); accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - "Gazer"))
    }
    private fun scan(run: SafariRun, vararg ids: UUID) = run.sparklingChecks.scan(SafariBiome.HAUNTED,
        -4.0, -64.0, ids.map { "Gazer" to it })
    @Test fun `four Gazers seen separately retain four checked when unloaded and after biome reentry`() {
        val run = SafariRun(0)
        val ids = List(4) { UUID.randomUUID() }
        ids.forEachIndexed { i, id -> scan(run, id); assertEquals(i + 1, run.sparklingChecks.checkCount("Gazer")) }
        scan(run)
        assertTrue(run.sparklingComplete("Gazer", party()))
        assertEquals("4/4 checked", SafariPanels.fixedCounter("Gazer", run, SafariMode.SPARKLING, party()))
        run.sparklingChecks.reenter(SafariBiome.HAUNTED)
        assertTrue(run.sparklingComplete("Gazer", party()))
        scan(run)
        assertTrue(run.sparklingComplete("Gazer", party()))
        assertEquals(4, run.sparklingChecks.checkCount("Gazer"))
        assertEquals(0, run.count("Gazer"))
    }
    @Test fun `same UUID never adds progress and catching a previously seen Gazer cannot double count it`() {
        val run = SafariRun(0); val ids = List(3) { UUID.randomUUID() }
        repeat(4) { scan(run, ids[0]) }
        assertEquals(1, run.sparklingChecks.checkCount("Gazer"))
        scan(run, *ids.toTypedArray())
        run.record(SafariCatch(SafariRoster.named("Gazer")!!))
        scan(run, ids[1], ids[2])
        assertEquals(3, run.sparklingChecks.checkCount("Gazer"))
        assertFalse(run.sparklingComplete("Gazer", party()))
        scan(run, UUID.randomUUID())
        assertEquals(4, run.sparklingChecks.checkCount("Gazer"))
        assertTrue(run.uniqueComplete("Gazer", party()))
    }
    @Test fun `earlier shared catches combine with later distinct sightings and Full Clear stays capture based`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Gazer")!!, false)) }
        scan(run, UUID.randomUUID()); scan(run, UUID.randomUUID()); scan(run)
        assertEquals("4/4 checked", SafariPanels.fixedCounter("Gazer", run, SafariMode.UNIQUE, party()))
        assertEquals("2/4 captured", SafariPanels.fixedCounter("Gazer", run, SafariMode.FULL_CLEAR, party()))
        run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false)
        assertFalse(run.captureComplete("Gazer"))
        val next = SafariRun(1)
        scan(next)
        assertEquals(0, next.sparklingChecks.checkCount("Gazer"))
    }
}
