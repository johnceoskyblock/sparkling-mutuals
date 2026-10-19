package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniquePartyResourceEvidenceTest {
    @Test fun `later same-type food is additional demand rather than overlapping starting inventory`() {
        val run = SafariRun(1000)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 1000)
        repeat(2) { run.recordBirdFood("FLOOR DROP! Bag of Seeds", null, 1100) }
        assertEquals(11, run.birds.feedingTarget)
        run.recordBirdFood("FLOOR DROP! Bag of Seeds", null, 5000)
        assertEquals(12, run.birds.feedingTarget)
        assertEquals(12, run.birds.sparklingTarget)
    }
    @Test fun `starting pickup and inventory evidence count the same food once in either order`() {
        for (inventoryFirst in listOf(false, true)) {
            val birds = SafariBirdLedger()
            if (inventoryFirst) birds.startingInventory(listOf("Yogi Berry" to 2), 0)
            repeat(2) { birds.pickup("FLOOR DROP! Yogi Berry", null) }
            if (!inventoryFirst) birds.startingInventory(listOf("Yogi Berry" to 2), 500)
            repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
            assertEquals(11, birds.feedingTarget)
            repeat(11) { birds.renderBird("Bluebird") }
            birds.inventory(emptyList())
            birds.scan(0)
            assertTrue(birds.allSpawned)
            assertEquals(11, birds.requiredCaptures(0, 0))
        }
    }
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    private fun forest() = SafariRun(0).apply {
        visitBiome(SafariBiome.FOREST)
        repeat(6) { recordBirdFood("FLOOR DROP! Wriggleworm", SafariBiome.FOREST, 10000) }
        SafariBiome.FOREST.critters.forEach { record(SafariCatch(it)) }
        updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
    }
    @Test fun `Unique party cd has no effect before or after visiting Cavern`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.ICY)
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.CAVERN)
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
    }
    @Test fun `owned bird discoveries do not waive Unique spawning requirements`() {
        val run = forest(); val state = party()
        assertEquals(6, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
    }
    @Test fun `random starting food adds feedings and Macaw pairs consume one food each`() {
        val run = forest(); val state = party("Bluebird")
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
        repeat(8) { run.birds.renderBird("Bluebird") }
        run.birds.renderBird("Macaw")
        assertEquals(9, run.birds.accountedFeedings)
        assertFalse(run.sparklingComplete("Bluebird", state))
        run.birds.renderBird("Parakeet")
        assertFalse(run.sparklingComplete("Bluebird", state))
        run.birds.renderBird("Macaw")
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), birdsSpawned = run.birds.sparklingSpawned)
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
    }
    @Test fun `incoming party fd supplies Unique checking after all species have shared catches`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = forest(); val state = party("Bluebird", "Parakeet", "Macaw")
        SafariMessages.forRun("Party > Friend: fd", run, true)
        assertEquals(6, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state))
        assertEquals("6/9", SafariPanels.progress(run, true, 0, state).rows.single { it.label == "Forest" }.value)
        assertFalse(run.sparklingComplete("Bluebird", state))
    }
    @Test fun `Full Clear feeding and capture demand include extra starting inventory`() {
        val birds = SafariBirdLedger()
        birds.startingInventory(listOf("Yogi Berry" to 2), 0)
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
        repeat(9) { birds.renderBird("Bluebird") }
        assertFalse(birds.allSpawned)
        assertFalse(birds.sufficient(9, 0, 0))
        repeat(2) { birds.renderBird("Bluebird") }
        birds.inventory(emptyList())
            birds.scan(0)
            assertTrue(birds.allSpawned)
        assertEquals(11, birds.requiredCaptures(0, 0))
        assertFalse(birds.sufficient(9, 0, 0))
        assertTrue(birds.sufficient(11, 0, 0))
    }
}
