package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariBirdAnnouncementsTest {
    private fun run() = SafariRun(0).apply {
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) recordBirdFood("FLOOR DROP! $food") }
        repeat(3) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        repeat(3) { birds.spawn("A Parakeet was attracted to the Birdfeeder!") }
        repeat(3) { birds.spawn("Two Macaws were attracted to the Birdfeeder!") }
        updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        birds.inventory(emptyList())
        birds.scan(0)
    }
    private fun SafariRun.catch(name: String, amount: Int, personal: Boolean = true) {
        repeat(amount) { record(SafariCatch(SafariRoster.named(name)!!, personal)) }
    }
    @Test fun `spawn announcements count food but held inventory blocks completion`() {
        val run = run()
        run.birds.inventory(listOf("Bag of Seeds" to 3, "Wriggleworm" to 3, "Yogi Berry" to 3))
        assertEquals(9, run.birds.used)
        assertFalse(run.birds.allSpawned)
        run.birds.inventory(emptyList())
        assertTrue(run.birds.allSpawned)
        run.catch("Bluebird", 3); run.catch("Parakeet", 3); run.catch("Macaw", 1)
        assertTrue(run.birdsComplete(emptySet()))
        run.recordBirdFood("FLOOR DROP! Bag of Seeds")
        assertFalse(run.birds.allSpawned)
        run.birds.spawn("A Bluebird was attracted to the Birdfeeder!")
        run.birds.scan(0)
        assertFalse(run.birdsComplete(emptySet()))
        run.catch("Bluebird", 1)
        assertTrue(run.birdsComplete(emptySet()))
    }
    @Test fun `other bird captures cannot substitute for announced species even after unloading`() {
        val run = run(); run.birds.inventory(emptyList())
        run.catch("Bluebird", 2); run.catch("Parakeet", 4); run.catch("Macaw", 6)
        assertFalse(run.birdsComplete(emptySet()))
        run.catch("Bluebird", 1)
        assertTrue(run.birdsComplete(emptySet()))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Bluebird"), false)
        assertFalse(run.birdsComplete(setOf("Bluebird")))
    }
    @Test fun `one Macaw completes Full Clear despite unloaded extra pairs but not personal PB from party catches`() {
        val run = run(); run.birds.inventory(emptyList())
        run.catch("Bluebird", 3); run.catch("Parakeet", 3); run.catch("Macaw", 1, false)
        assertTrue(run.birdsComplete(emptySet()))
        assertFalse(run.birdsComplete(emptySet(), personalOnly = true))
        run.catch("Macaw", 1)
        assertTrue(run.birdsComplete(emptySet(), personalOnly = true))
        run.partyFullClear("Party > Friend: fd")
        assertFalse(run.missing(SafariRoster.named("Macaw")!!, true))
        assertFalse(SafariRun(1).birds.allSpawned)
    }
}
