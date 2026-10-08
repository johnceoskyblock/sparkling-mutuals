package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariPanelSeparationTest {
    @TempDir lateinit var dir: Path
    @Test fun `normal missing panel needs one capture and never shows quotas`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        for (name in listOf("Scrappy", "Gemzie", "Troodon", "Gazer")) {
            val critter = SafariRoster.named(name)!!
            assertTrue(SafariPanels.missing(run, critter.biome, true, 0).rows.any { it.label == name && it.value == null })
            run.record(SafariCatch(critter))
            assertFalse(SafariPanels.missing(run, critter.biome, true, 0).rows.any { it.label == name })
        }
    }
    @Test fun `scanned empty mounds complete Rockmite only after revealed critters are caught`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        repeat(9) { run.recordMound("The mound falls apart, but nothing is inside") }
        run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!")
        assertFalse(run.captureComplete("Rockmite"))
        run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
        run.moundSurvey.scan(emptySet()) { true }
        assertTrue(run.captureComplete("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite Mound"), true)
        assertFalse(run.captureComplete("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite"), true)
        assertFalse(run.captureComplete("Rockmite"))
        val sites = (0..9).map { Triple(it, 0, 0) }.toSet()
        val surveyed = SafariRun(0)
        surveyed.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        surveyed.moundSurvey.scan(sites) { true }
        assertFalse(surveyed.captureComplete("Rockmite"))
        surveyed.moundSurvey.scan(emptySet()) { true }
        assertTrue(surveyed.captureComplete("Rockmite"))
    }
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
            names.forEach { name -> assertEquals(-1, rows.first { it.label == name }.valueColor) }
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
    @Test fun `sixteen broken mounds cannot make mound counter green while a mound remains loaded`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        repeat(16) { run.recordMound("The mound falls apart, but nothing is inside") }
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite Mound"), true)
        val row = SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }
        assertEquals("16", row.value)
        assertEquals(0xFFFF5555.toInt(), row.valueColor)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        run.moundSurvey.scan(emptySet()) { true }
        assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == row.label }.valueColor)
    }
    @Test fun `mound counter stays red for unobservable known mound even after empty nearby scan`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        repeat(16) { run.recordMound("The mound falls apart, but nothing is inside") }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        val site = Triple(-100, 40, 30)
        run.moundSurvey.scan(setOf(site)) { true }
        run.moundSurvey.scan(emptySet()) { false }
        assertEquals(0xFFFF5555.toInt(), SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }.valueColor)
        run.moundSurvey.scan(emptySet()) { true }
        assertEquals(0xFF55FF55.toInt(), SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }.valueColor)
    }
    @Test fun `mound counter needs scan evidence but not the captures of spawned Rockmites`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        repeat(16) { run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!") }
        assertEquals(-1, SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }.valueColor)
        run.moundSurvey.scan(emptySet()) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite"), true)
        val rows = SafariPanels.captures(run, SafariBiome.CAVERN).rows
        assertEquals(0xFF55FF55.toInt(), rows.first { it.label == "Rockmite Mounds" }.valueColor)
        assertEquals(0xFFFF5555.toInt(), rows.first { it.label == "Rockmite" }.valueColor)
        ConfigManager.fullClearMode = false
        assertEquals(-1, SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }.valueColor)
    }
    @Test fun `bird colors require all pickups food used sufficient catches and no same species nearby`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        fun green() = SafariPanels.captures(run, SafariBiome.FOREST).rows.first { it.label == "Bluebird" }.valueColor == 0xFF55FF55.toInt()
        assertFalse(run.recordBirdFood("Party > Friend: FLOOR DROP! Bag of Seeds"))
        assertFalse(run.recordBirdFood("FLOOR DROP! Something else"))
        repeat(3) { assertTrue(run.recordBirdFood("§aFLOOR DROP! Bag of Seeds")); run.recordBirdFood("FLOOR DROP! Wriggleworm") }
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        repeat(9) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(green())
        repeat(3) { run.recordBirdFood("FLOOR DROP! Yogi Berry") }
        run.birds.inventory(emptyList())
        assertFalse(green())
        run.record(SafariCatch(SafariRoster.named("Parakeet")!!))
        assertTrue(green())
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Bluebird"), false)
        assertFalse(green())
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(SafariPanels.captures(run, SafariBiome.FOREST).rows.filter { it.label in listOf("Bluebird", "Parakeet", "Macaw") }.all { it.valueColor == 0xFF55FF55.toInt() })
        assertFalse(SafariRun(0).birdFoodsComplete)
    }
    @Test fun `caught Macaw stays green with remaining critters after food and capture requirements`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        assertFalse(run.captureComplete("Macaw"))
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        assertFalse(run.captureComplete("Macaw"))
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        assertFalse(run.captureComplete("Macaw"))
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }
        repeat(8) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        run.birds.inventory(emptyList())
        val birds = setOf("Bluebird", "Parakeet", "Macaw")
        assertTrue(SafariPanels.captures(run, SafariBiome.FOREST).rows.filter { it.label in birds }.all { it.valueColor == 0xFF55FF55.toInt() })
        for (other in listOf("Bluebird", "Parakeet", "Foxtrot")) {
            run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw", other), false)
            assertTrue(run.captureComplete("Macaw"))
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
        run.moundSurvey.scan(emptySet()) { true }
        assertTrue(green("Rockmite"))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite"), true)
        assertFalse(green("Rockmite"))
    }
}
