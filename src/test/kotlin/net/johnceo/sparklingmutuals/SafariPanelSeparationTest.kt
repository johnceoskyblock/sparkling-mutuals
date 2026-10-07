package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariPanelSeparationTest {
    @TempDir lateinit var dir: Path
    @Test fun `full clear never precompletes progress or changes its one catch logic`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Forest" }.value)
        run.record(SafariCatch(SafariRoster.named("Foxtrot")!!))
        assertEquals("1/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Forest" }.value)
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Cavern" }.value)
    }
    @Test fun `missing panel quotas are only the four known multiple critters`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        val rows = SafariBiome.entries.flatMap { SafariPanels.missing(run, it, false, 0).rows }
        assertEquals(setOf("Scrappy", "Gemzie", "Troodon", "Gazer"), rows.filter { it.value?.contains('/') == true }.map { it.label }.toSet())
        assertTrue(rows.any { it.label == "Bluebird" })
        run.record(SafariCatch(SafariRoster.named("Foxtrot")!!))
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, false, 0).rows.any { it.label == "Foxtrot" })
    }
    @Test fun `zero minimum variable species require clear evidence before turning green`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        for ((biome, names) in listOf(SafariBiome.FOREST to listOf("Bluebird", "Parakeet", "Macaw"),
            SafariBiome.CAVERN to listOf("Rockmite", "Snoozle"))) {
            val rows = SafariPanels.captures(run, biome).rows
            names.forEach { name -> assertEquals(0xFFFF5555.toInt(), rows.first { it.label == name }.valueColor) }
        }
    }
    @Test fun `Rockmite capture and two mound rows are separated below the total`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val rows = SafariPanels.captures(SafariRun(0), SafariBiome.CAVERN).rows
        assertEquals(3, rows.count { it.label.contains("Rockmite", true) })
        assertTrue(rows.any { it.label == "Rockmite" })
        val total = rows.indexOfFirst { it.label == "Total captures" }
        assertEquals("", rows[total + 1].label)
        assertEquals("Rockmite Mounds", rows[total + 2].label)
        assertEquals("Mounds with Rockmite", rows[total + 3].label)
    }
    @Test fun `bird colors require three of each food eight catches and no nearby birds`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        fun green() = SafariPanels.captures(run, SafariBiome.FOREST).rows.first { it.label == "Macaw" }.valueColor == 0xFF55FF55.toInt()
        assertFalse(run.recordBirdFood("Party > Friend: FLOOR DROP! Bag of Seeds"))
        assertFalse(run.recordBirdFood("FLOOR DROP! Something else"))
        repeat(3) { assertTrue(run.recordBirdFood("§aFLOOR DROP! Bag of Seeds")); run.recordBirdFood("FLOOR DROP! Wriggleworm") }
        repeat(7) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(green())
        repeat(3) { run.recordBirdFood("FLOOR DROP! Yogi Berry") }
        assertFalse(green())
        run.record(SafariCatch(SafariRoster.named("Parakeet")!!))
        assertTrue(green())
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Bluebird"), false)
        assertFalse(green())
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(SafariPanels.captures(run, SafariBiome.FOREST).rows.filter { it.label in listOf("Bluebird", "Parakeet", "Macaw") }.all { it.valueColor == 0xFF55FF55.toInt() })
        assertFalse(SafariRun(0).birdFoodsComplete)
    }
    @Test fun `only Macaws remaining completes bird colors after food and capture requirements`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        assertFalse(run.captureComplete("Macaw"))
        repeat(7) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        assertFalse(run.captureComplete("Macaw"))
        run.record(SafariCatch(SafariRoster.named("Parakeet")!!))
        assertFalse(run.captureComplete("Macaw"))
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }
        val birds = setOf("Bluebird", "Parakeet", "Macaw")
        assertTrue(SafariPanels.captures(run, SafariBiome.FOREST).rows.filter { it.label in birds }.all { it.valueColor == 0xFF55FF55.toInt() })
        for (other in listOf("Bluebird", "Parakeet", "Foxtrot")) {
            run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw", other), false)
            assertFalse(run.captureComplete("Macaw"))
        }
    }
    @Test fun `Cavern colors require checked structures collected rockmites and no nearby entities`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        fun green(name: String) = SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == name }.valueColor == 0xFF55FF55.toInt()
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        assertFalse(green("Snoozle")); assertFalse(green("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Snoozle"), true)
        assertFalse(green("Snoozle"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        assertTrue(green("Snoozle"))
        repeat(19) { run.recordMound("The mound falls apart, but nothing is inside") }
        run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!")
        assertFalse(green("Rockmite"))
        run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
        assertTrue(green("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite"), true)
        assertFalse(green("Rockmite"))
    }
}
