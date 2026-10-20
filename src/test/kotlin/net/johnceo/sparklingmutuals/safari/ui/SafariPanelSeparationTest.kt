package net.johnceo.sparklingmutuals.safari.ui

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariPanelSeparationTest {
    @TempDir lateinit var dir: Path
    @Test fun `Unique missing panel removes ordinary species after its first catch`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        for (name in listOf("Scrappy", "Gemzie", "Troodon", "Gazer")) {
            val critter = SafariRoster.named(name)!!
            assertTrue(SafariPanels.missing(run, critter.biome, true, 0).rows.any { it.label == name && it.value == "0/${critter.quota} checked" })
            run.record(SafariCatch(critter))
            assertFalse(SafariPanels.missing(run, critter.biome, true, 0).rows.any { it.label == name })
            assertEquals(0, SafariPanels.missingCritters(run, critter.biome).count { it.name == name })
        }
    }
    @Test fun `Unique captured birds and source critters leave checklist before source progress completes`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        val party = net.johnceo.sparklingmutuals.safari.PartySparklingState()
        for (name in listOf("Bluebird", "Parakeet", "Macaw", "Rockmite", "Snoozle")) {
            val critter = SafariRoster.named(name)!!
            run.record(SafariCatch(critter))
            assertFalse(SafariPanels.missingCritters(run, critter.biome, party).any { it.name == name })
            assertFalse(run.uniqueComplete(name, party), "source gates still apply to progress for $name")
        }
        val rows = SafariPanels.missing(run, SafariBiome.FOREST, true, 3, party = party).rows
        assertEquals("9", rows.first { it.label == "Bird food left to use" }.value)
        run.birds.inventory(listOf("Bag of Seeds" to 2))
        run.birds.scan(2, 7)
        assertEquals("4", SafariPanels.missing(run, SafariBiome.FOREST, true, 3, party = party).rows
            .first { it.label == "Bird food left to use" }.value)
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
    @Test fun `full clear progress waits for minimum captures and absence evidence`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Forest" }.value)
        run.visitBiome(SafariBiome.FOREST)
        repeat(6) { run.record(SafariCatch(SafariRoster.named("Foxtrot")!!)) }
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Forest" }.value)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Forest" }.value)
        assertEquals("0/9", SafariPanels.progress(run, false, 0).rows.first { it.label == "Cavern" }.value)
    }
    @Test fun `nest helper does not add extra species capture quotas to the missing panel`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        val rows = SafariBiome.entries.flatMap { SafariPanels.missing(run, it, false, 0).rows }
        assertEquals(setOf("Scrappy", "Gemzie", "Troodon", "Gazer"),
            rows.filter { it.value?.contains('/') == true }.map { it.label }.toSet())
        assertNull(rows.first { it.label == "Honeybug" }.value)
        assertTrue(rows.any { it.label == "Bluebird" })
        repeat(6) { run.record(SafariCatch(SafariRoster.named("Foxtrot")!!)) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), true, playerX = 3.0, playerZ = 47.0)
        run.partyFullClear("Party > Friend: fd")
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
    @Test fun `bird colors become independent once known food demand is consumed`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        fun green() = SafariPanels.captures(run, SafariBiome.FOREST).rows.first { it.label == "Bluebird" }.valueColor == 0xFF55FF55.toInt()
        assertFalse(run.recordBirdFood("Party > Friend: FLOOR DROP! Bag of Seeds"))
        assertFalse(run.recordBirdFood("FLOOR DROP! Something else"))
        repeat(3) { assertTrue(run.recordBirdFood("§aFLOOR DROP! Bag of Seeds")); run.recordBirdFood("FLOOR DROP! Wriggleworm") }
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        repeat(8) { run.birds.renderBird("Bluebird") }
        run.birds.renderBird("Parakeet")
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        run.partyFullClear("Party > Friend: fd")
        assertFalse(green(), "the rendered Parakeet has not been caught")
        repeat(3) { run.recordBirdFood("FLOOR DROP! Yogi Berry") }
        repeat(3) { run.birds.renderBird("Bluebird") }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        run.birds.inventory(emptyList())
        run.birds.scan(0)
        run.partyFullClear("Party > Friend: fd")
        assertFalse(green(), "fd cannot replace the remaining Parakeet capture")
        run.record(SafariCatch(SafariRoster.named("Parakeet")!!))
        run.partyFullClear("Party > Friend: fd")
        assertTrue(green())
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Bluebird"), false)
        run.scanBirdSources(0, listOf("Bluebird"))
        assertFalse(green())
        run.record(SafariCatch(SafariRoster.named("Bluebird")!!))
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
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

        assertTrue(run.captureComplete("Macaw"), "eight normal catches plus one Macaw satisfy the base opportunities")

        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }

        repeat(8) { run.birds.renderBird("Bluebird") }

        run.birds.renderBird("Macaw")

        run.birds.inventory(emptyList())

        run.birds.scan(0)

        run.partyFullClear("Party > Friend: fd")
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
        run.partyFullClear("Party > Friend: cd")
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
