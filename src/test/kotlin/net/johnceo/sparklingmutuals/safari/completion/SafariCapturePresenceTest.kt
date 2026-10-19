package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariCapturePresenceTest {
    @Test fun `minimum captures need a scan with no same species and turn red when it returns`() {
        for ((name, minimum) in listOf("Foxtrot" to 6, "Scrappy" to 3, "Troodon" to 3, "Doomspiral" to 1)) {
            val critter = SafariRoster.named(name)!!
            val run = SafariRun(0)
            repeat(minimum - 1) { run.record(SafariCatch(critter)) }
            run.updateCaptureEvidence(critter.biome, emptySet(), false)
            assertFalse(run.captureComplete(name))
            run.record(SafariCatch(critter))
            assertTrue(run.captureComplete(name))
            run.updateCaptureEvidence(critter.biome, setOf(name), false)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(critter.biome, setOf("Macaw"), false)
            assertTrue(run.captureComplete(name))
            val unscanned = SafariRun(0)
            repeat(minimum) { unscanned.record(SafariCatch(critter)) }
            assertFalse(unscanned.captureComplete(name))
        }
    }
    @Test fun `birds clear independently after feeding and every remaining species blocks itself`() {
        val run = SafariRun(0)
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)); run.birds.renderBird("Bluebird") }
        run.birds.renderBird("Macaw")
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        assertFalse(run.captureComplete("Bluebird"))
        assertFalse(run.captureComplete("Parakeet")) // Expected birds are not all captured yet.
        assertFalse(run.captureComplete("Macaw"))
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        assertTrue(run.captureComplete("Macaw")) // One shared catch suffices despite the remaining Macaw.
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(run.birdsComplete(emptySet(), personalOnly = true))
        run.recordBirdFood("FLOOR DROP! Bag of Seeds")
        run.birds.inventory(listOf("Bag of Seeds" to 1))
        assertFalse(run.captureComplete("Bluebird")) // Extra food reopens every feeder row.
    }
}
