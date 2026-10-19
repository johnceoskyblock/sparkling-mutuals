package net.johnceo.sparklingmutuals.safari.structures

import net.johnceo.sparklingmutuals.safari.resources.knownStartingEvidence

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.BiomeClearEvidence
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules
import net.johnceo.sparklingmutuals.safari.completion.SafariPersonalBests
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForestNestRuntimeTest {
    private fun caught(run: SafariRun, name: String, count: Int = 1, personal: Boolean = true) =
        repeat(count) { run.record(SafariCatch(SafariRoster.named(name)!!, personal)) }
    private fun party(needsHoneybug: Boolean = false) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() -
            if (needsHoneybug) setOf("Honeybug") else emptySet())))
    }
    private fun forestRun(): SafariRun = SafariRun(0).knownStartingEvidence().apply {
        visitBiome(SafariBiome.FOREST)
        SafariBiome.FOREST.critters.filter { it.name !in setOf("Honeybug", "Bluebird", "Parakeet", "Macaw") }
            .forEach { caught(this, it.name, SafariFullClear.minimum(it.name)) }
        caught(this, "Bluebird", 9)
        for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) repeat(3) {
            birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST)
        }
        repeat(9) { birds.renderBird("Bluebird") }
        birds.inventory(emptyList())
        scanRenderedBirds()
        updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
    }
    @Test fun `Full Clear uses punched nest totals even when released bees are outside entity range`() {
        for (total in 3..6) {
            val run = forestRun()
            caught(run, "Honeybug", 3)
            run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true, nestsPunched = total)
            assertEquals(total == 3, run.captureComplete("Honeybug"), "run with $total nests")
            assertEquals(total == 3, run.captureComplete("Honeybug", personalOnly = true))
            if (total > 3) caught(run, "Honeybug", total - 3)
            assertTrue(run.captureComplete("Honeybug"))
        }
    }
    @Test fun `an unseen released bee prevents a Forest announcement and personal Full Clear PB`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true, nestsPunched = 6)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = true), SafariPersonalBests(), 1000))
        caught(run, "Honeybug", 3)
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
        assertNotNull(SafariFullClear.recordClear(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = true), SafariPersonalBests(), 1000))
    }
    @Test fun `unknown sites prevent local completion despite three captures and no visible nests`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, false, nestsPunched = 3)
        assertFalse(run.captureComplete("Honeybug"))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = false), SafariPersonalBests(), 1000))
    }
    @Test fun `shared captures can finish run progress but cannot satisfy the personal PB bee total`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        caught(run, "Honeybug", 3, personal = false)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true, nestsPunched = 6)
        assertTrue(run.captureComplete("Honeybug"))
        assertFalse(run.captureComplete("Honeybug", personalOnly = true))
    }
    @Test fun `incomplete nest survey keeps Honeybug listed without helper prose`() {
        net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = false
        net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = true
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, false, nestsPunched = 3)
        run.updateNestEvidence(false, 3, 3, 1)
        val rows = SafariPanels.missing(run, SafariBiome.FOREST, false, 0, party = party()).rows
        assertTrue(rows.any { it.label == "Honeybug" })
        assertFalse(rows.any { it.label == "Nest survey" })
        assertFalse(run.nestSurveyComplete)
    }
    @Test fun `fresh block evidence revokes local completion before another entity scan`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateNestEvidence(true, 3, 3, 0)
        assertTrue(run.sparklingComplete("Honeybug", party(), localOnly = true))
        assertFalse(SafariHelperRules.showNests(SafariMode.SPARKLING, run, party(true)))
        assertTrue(run.uniqueComplete("Honeybug", party(true), localOnly = true))
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
        run.biomeClears.add(SafariBiome.FOREST)
        run.updateNestEvidence(false, 3, 4, 1)
        assertFalse(run.captureComplete("Honeybug"))
        assertFalse(run.sparklingComplete("Honeybug", party(), localOnly = true))
        assertTrue(SafariHelperRules.showNests(SafariMode.SPARKLING, run, party(true)))
        assertTrue(run.uniqueComplete("Honeybug", party(true), localOnly = true))
        assertFalse(SafariBiome.FOREST in run.biomeClears)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
        assertNull(SafariFullClear.recordClear(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = true, nestsFound = 4), SafariPersonalBests(), 1000))
    }
    @Test fun `personal PB evidence cannot replace six known nests with the old three bee minimum`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateNestEvidence(true, 6, 6, 0)
        val evidence = BiomeClearEvidence(observed = true, nestsChecked = true, nestsFound = 6)
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, evidence))
        caught(run, "Honeybug", 3)
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, evidence))
    }
    @Test fun `verified nests and captures complete with runtime coordinates even if bees were never rendered`() {
        val run = forestRun()
        caught(run, "Honeybug", 3)
        run.updateNestEvidence(true, 3, 3, 0)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true,
            playerX = 3.0, playerZ = 47.0, nestsPunched = 3, nestsFound = 3)
        assertTrue(run.captureComplete("Honeybug"))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST,
            BiomeClearEvidence(observed = true, nestsChecked = true, nestsFound = 3)))
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.FULL_CLEAR, party()))
    }
}
