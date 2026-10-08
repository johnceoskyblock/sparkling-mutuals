package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariForestFeedingTest {
    private fun forest(): SafariRun = SafariRun(1000).apply {
        mapOf("Foxtrot" to 6, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
            "Fluffling" to 1, "Hideonfloor" to 1).forEach { (name, amount) ->
            repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!)) }
        }
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) recordBirdFood("FLOOR DROP! $food") }
        repeat(5) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        repeat(4) { birds.spawn("Two Macaws were attracted to the Birdfeeder!") }
        birds.inventory(emptyList())
    }
    @Test fun `six personal birds and seven leftover Macaws record a Forest PB after nests clear`() {
        val run = forest()
        repeat(5) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        val ready = BiomeClearEvidence(observed = true, nearbyCritters = 7, nearbyMacaws = 7, nestsChecked = true)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, true, 7)
        assertTrue(run.birdsComplete(setOf("Macaw"), true))
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nestsChecked = false)))
        val bests = SafariPersonalBests()
        assertNotNull(SafariFullClear.recordClear(run, SafariBiome.FOREST, ready, bests, 31000))
        assertEquals(30000L, bests.time("Forest"))
    }
    @Test fun `loot shared birds cannot substitute for personal birds and remaining normal birds block PB`() {
        val run = forest()
        repeat(5) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!, false)) }
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        val ready = BiomeClearEvidence(observed = true, nearbyCritters = 7, nearbyMacaws = 7, nestsChecked = true)
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, true, 7)
        assertTrue(run.captureComplete("Bluebird"))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        repeat(5) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        assertTrue(SafariFullClear.eligible(run, SafariBiome.FOREST, ready))
        assertFalse(SafariFullClear.eligible(run, SafariBiome.FOREST, ready.copy(nearbyCritters = 8)))
    }
}
