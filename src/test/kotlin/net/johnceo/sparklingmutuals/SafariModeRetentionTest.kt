package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariModeRetentionTest {
    @TempDir lateinit var dir: Path
    @Test fun `all done words received in every mode survive switching and do not create captures`() {
        ConfigManager.init(dir)
        for (mode in SafariMode.entries) for ((word, biome) in mapOf("fd" to SafariBiome.FOREST,
            "cd" to SafariBiome.CAVERN, "id" to SafariBiome.ICY, "hd" to SafariBiome.HAUNTED)) {
            val run = SafariRun(0); SafariFullClear.select(mode)
            assertTrue(SafariMessages.forRun("Party > Player: $word", run, true).isEmpty())
            for (selected in SafariMode.entries) {
                SafariFullClear.select(selected)
                assertTrue(biome.critters.all { run.sparklingChecks.checked(it.name) })
                assertEquals(biome.critters.size, run.fullClearProgress(biome.critters))
                assertEquals(0, run.progress(biome.critters, true))
                assertTrue(run.biomeClears.isEmpty()); assertTrue(run.uniqueBiomeClears.isEmpty())
            }
        }
    }
    @Test fun `switching modes retains capture structure and Sparkling evidence on the same run`() {
        ConfigManager.init(dir)
        val ledger = SafariLedger(); ledger.arrive(0); val run = ledger.current!!
        run.visitBiome(SafariBiome.CAVERN)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!)) }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        run.sparklingChecks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList())
        val original = run.fullClearProgress(SafariBiome.CAVERN.critters)
        repeat(2) { for (mode in SafariMode.entries) {
            SafariFullClear.select(mode)
            assertSame(run, ledger.current); assertEquals(3, run.count("Driftling"))
            assertEquals(original, run.fullClearProgress(SafariBiome.CAVERN.critters))
            assertTrue(run.sparklingChecks.checked("Driftling"))
        } }
    }
    @Test fun `outside messages invalid channels and new runs cannot inherit done evidence`() {
        ConfigManager.init(dir)
        for (text in listOf("Party > Player: fd", "Guild > Player: fd", "Party > Player: fd please", "Party > Player: quoted\\nfd")) {
            val run = SafariRun(0)
            SafariMessages.forRun(text, run, text != "Party > Player: fd")
            assertFalse(run.sparklingChecks.checked("Foxtrot")); assertEquals(0, run.fullClearProgress(SafariBiome.FOREST.critters))
        }
        val ledger = SafariLedger(); ledger.arrive(0)
        SafariMessages.forRun("Party > Player: fd", ledger.current, true)
        ledger.enter(100)
        assertFalse(ledger.current!!.sparklingChecks.checked("Foxtrot"))
        assertEquals(0, ledger.current!!.fullClearProgress(SafariBiome.FOREST.critters))
    }
}
