package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
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
    @Test fun `birds require the food derived total and independently clear with caught Macaw exception`() {
        val run = SafariRun(0)
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) run.recordBirdFood("FLOOR DROP! $food") }
        repeat(8) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        repeat(8) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        run.birds.inventory(emptyList())
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(run.captureComplete("Parakeet"))
        run.record(SafariCatch(SafariRoster.named("Macaw")!!))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Bluebird", "Macaw", "Foxtrot"), false)
        assertTrue(run.captureComplete("Macaw"))
        assertTrue(run.captureComplete("Parakeet")) // No per-species minimum for absent birds.
        assertFalse(run.captureComplete("Bluebird"))
        assertFalse(run.birdsComplete(setOf("Bluebird", "Macaw"), personalOnly = true))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        assertTrue(run.captureComplete("Bluebird"))
        assertTrue(run.birdsComplete(setOf("Macaw"), personalOnly = true))
        val uncaughtMacaw = SafariRun(0)
        repeat(8) { uncaughtMacaw.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) uncaughtMacaw.recordBirdFood("FLOOR DROP! $food") }
        repeat(8) { uncaughtMacaw.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        uncaughtMacaw.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        uncaughtMacaw.birds.inventory(emptyList())
        uncaughtMacaw.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false, macawsInRange = 2)
        assertFalse(uncaughtMacaw.captureComplete("Macaw"))
        assertFalse(uncaughtMacaw.captureComplete("Bluebird")) // All announced bird captures are still required.
        assertFalse(uncaughtMacaw.birdsComplete(setOf("Macaw"), personalOnly = true))
    }
}
