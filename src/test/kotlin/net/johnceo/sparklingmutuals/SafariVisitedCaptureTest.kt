package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class SafariVisitedCaptureTest {
    @TempDir lateinit var dir: Path
    @Test fun `party captures stay white until biome visit verifies remaining critters`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!, personal = false)) }
        fun color() = SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Driftling" }.valueColor
        assertEquals(-1, color())
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertEquals(-1, color())
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertEquals(0xFFFF5555.toInt(), color())
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertEquals(0xFF55FF55.toInt(), color())
        assertEquals(0, run.personalCount("Driftling"))
    }
    @Test fun `party-cleared mounds need a survey but no personal minimum`() {
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        assertFalse(run.moundsComplete())
        run.moundSurvey.scan(emptySet()) { true }
        assertTrue(run.moundsComplete()); assertTrue(run.captureComplete("Rockmite"))
        assertEquals(0, run.brokenMounds)
        val site = Triple(-100, 40, 30)
        run.moundSurvey.scan(setOf(site)) { true }
        assertFalse(run.moundsComplete())
        run.moundSurvey.scan(emptySet()) { true }
        assertTrue(run.moundsComplete())
    }
    @Test fun `stationary exclusion updates remaining evidence without inventing captures and reverses`() {
        val run = SafariRun(0); val motion = EspMotion(); val id = UUID.randomUUID()
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Driftling")!!, false)) }
        motion.observe(id, "Driftling", 0.0, 0.0, 0.0, 1.0, 0)
        fun check(now: Long) {
            run.updateCaptureEvidence(SafariBiome.CAVERN, if (motion.current(id, now)) setOf("Driftling") else emptySet(), false)
        }
        check(1999); assertFalse(run.captureComplete("Driftling"))
        check(2000); assertTrue(run.captureComplete("Driftling"))
        assertEquals(3, run.count("Driftling")); assertEquals(0, run.personalCount("Driftling"))
        motion.observe(id, "Driftling", .5, 0.0, 0.0, 1.0, 2100)
        check(2100); assertFalse(run.captureComplete("Driftling"))
    }
}
