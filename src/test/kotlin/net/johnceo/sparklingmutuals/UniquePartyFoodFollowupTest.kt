package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniquePartyFoodFollowupTest {
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
            repeat(11) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
            assertTrue(birds.allSpawned)
            assertEquals(11, birds.requiredCaptures(0, 0))
        }
    }
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    private fun forest() = SafariRun(0).apply {
        visitBiome(SafariBiome.FOREST)
        SafariBiome.FOREST.critters.forEach { record(SafariCatch(it)) }
        updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
    }
    @Test fun `Unique party cd is complete outside Cavern despite an earlier unfinished local survey`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.ICY)
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.CAVERN)
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
    }
    @Test fun `local Unique Forest waits for feeding even when nobody needs sparkling birds`() {
        val run = forest(); val state = party()
        assertTrue(run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state) < 9)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        repeat(9) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
    }
    @Test fun `random starting food adds feedings and Macaw pairs consume one food each`() {
        val run = forest(); val state = party()
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
        repeat(8) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        assertEquals(9, run.birds.used)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.birds.spawn("A Parakeet was attracted to the Birdfeeder!")
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
    }
    @Test fun `incoming party fd still completes the Unique bar without local feeding evidence`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = forest(); val state = party("Bluebird", "Parakeet", "Macaw")
        SafariMessages.forRun("Party > Friend: fd", run, true)
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state))
        assertEquals("9/9", SafariPanels.progress(run, true, 0, state).rows.single { it.label == "Forest" }.value)
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
    }
    @Test fun `Full Clear feeding and capture demand include extra starting inventory`() {
        val birds = SafariBirdLedger()
        birds.startingInventory(listOf("Yogi Berry" to 2), 0)
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
        repeat(9) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertFalse(birds.allSpawned)
        assertFalse(birds.sufficient(9, 0, 0))
        repeat(2) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertTrue(birds.allSpawned)
        assertEquals(11, birds.requiredCaptures(0, 0))
        assertFalse(birds.sufficient(9, 0, 0))
        assertTrue(birds.sufficient(11, 0, 0))
    }
}
