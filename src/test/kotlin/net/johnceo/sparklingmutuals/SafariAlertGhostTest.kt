package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariAlertGhostTest {
    @Test fun `two throws before server messages arrive preserve their individual targets`() {
        val memory = EspCaptureMemory()
        val first = java.util.UUID(0, 7)
        val second = java.util.UUID(0, 8)
        memory.sighted(first, "Driftling", 0)
        memory.throwing(0)
        memory.sighted(second, "Driftling", 100)
        memory.throwing(100)
        memory.threw("Driftling", 200)
        memory.threw("Driftling", 300)
        assertEquals(first, memory.caught("Driftling", 400))
        assertEquals(second, memory.caught("Driftling", 500))
    }
    @Test fun `server throw names distinguish a target from other nearby species`() {
        assertEquals("Driftling", SafariEspRules.thrownSpecies("You threw a Critter Capsule at the Driftling!"))
        assertEquals("Mantis Shrimp", SafariEspRules.thrownSpecies("You threw a Masterful Critter Capsule at the Mantis Shrimp!"))
        assertEquals("Driftling", SafariEspRules.escapedSpecies("The Driftling escaped your Critter Capsule!"))
        assertEquals("Flitter", SafariEspRules.escapedSpecies("The Flitter dodged your critter capsule!"))
        assertNull(SafariEspRules.thrownSpecies("Party > Friend: You threw a Critter Capsule at the Driftling!"))
        assertNull(SafariEspRules.thrownSpecies("You threw a Critter Capsule at the Fake Critter!"))
    }
    @Test fun `turning away before delayed server chat still retires the model aimed at during the throw`() {
        val memory = EspCaptureMemory()
        val hit = java.util.UUID(0, 7)
        val other = java.util.UUID(0, 8)
        memory.sighted(hit, "Driftling", 0)
        memory.throwing(0)
        memory.sighted(other, "Driftling", 100)
        memory.threw("Driftling", 500)
        assertEquals(hit, memory.caught("Driftling", 1000))
        assertTrue(memory.hidden(hit))
        assertFalse(memory.hidden(other))
    }
    @Test fun `recent per-species aim survives model removal before server throw message`() {
        val memory = EspCaptureMemory()
        val hit = java.util.UUID(0, 7)
        memory.sighted(hit, "Chuckwalla", 0)
        memory.sighted(java.util.UUID(0, 8), "Flitter", 100)
        memory.threw("Chuckwalla", 500)
        assertEquals(hit, memory.caught("Chuckwalla", 2000))
        assertFalse(memory.hidden(java.util.UUID(0, 8)))
    }
    @Test fun `escaped throw cannot steal a later successful capture`() {
        val memory = EspCaptureMemory()
        val escaped = java.util.UUID(0, 7)
        val caught = java.util.UUID(0, 8)
        memory.aimed(escaped, "Driftling", 0)
        memory.escaped("Driftling", 100)
        memory.aimed(caught, "Driftling", 200)
        assertEquals(caught, memory.caught("Driftling", 1000))
        assertFalse(memory.hidden(escaped))
    }
    @Test fun `count only turns green when confirmed models are retired and no genuine copy remains`() {
        val run = SafariRun(0)
        val memory = EspCaptureMemory()
        val ids = (1L..4L).map { java.util.UUID(0, it) }
        repeat(3) { index ->
            memory.sighted(ids[index], "Driftling", index * 1000L)
            memory.throwing(index * 1000L)
            memory.threw("Driftling", index * 1000L + 100)
            memory.caught("Driftling", index * 1000L + 200)
            run.record(SafariCatch(SafariRoster.named("Driftling")!!))
        }
        run.updateCaptureEvidence(SafariBiome.CAVERN, if (ids.take(3).any { !memory.hidden(it) }) setOf("Driftling") else emptySet(), false)
        assertTrue(run.captureComplete("Driftling"))
        assertFalse(memory.hidden(ids.last()))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertFalse(run.captureComplete("Driftling"))
    }
    @Test fun `old aim and previous run cannot retire an unconfirmed model`() {
        val memory = EspCaptureMemory()
        val id = java.util.UUID(0, 7)
        memory.sighted(id, "Driftling", 0)
        memory.throwing(0)
        memory.threw("Driftling", 4000)
        assertNull(memory.caught("Driftling", 4100))
        memory.sighted(id, "Driftling", 5000)
        memory.throwing(5000)
        memory.reset()
        memory.threw("Driftling", 5100)
        assertNull(memory.caught("Driftling", 5200))
        assertFalse(memory.hidden(id))
    }
    @Test fun `repeated throws refresh the capture association instead of leaving a ghost`() {
        val memory = EspCaptureMemory()
        val id = java.util.UUID(0, 7)
        memory.aimed(id, "Driftling", 0)
        memory.aimed(id, "Driftling", 9000)
        assertEquals(id, memory.caught("Driftling", 15000))
        assertTrue(memory.hidden(id))
    }
    @Test fun `a retired model cannot consume another catch of the same species`() {
        val memory = EspCaptureMemory()
        val first = java.util.UUID(0, 7)
        val second = java.util.UUID(0, 8)
        memory.aimed(first, "Driftling", 0)
        assertEquals(first, memory.caught("Driftling", 100))
        memory.aimed(first, "Driftling", 200)
        memory.aimed(second, "Driftling", 300)
        assertEquals(second, memory.caught("Driftling", 400))
        assertTrue(memory.hidden(second))
    }
    @Test fun `Driftling armor stand capture retires its model without hiding another critter`() {
        val driftling = SafariEspRules.identify(EspEntity("armor_stand",
            texture = "f4c4f8e5fce1ec2d299cb8a395792ecddc497a1d8af86faaa5e20373016c7225"))!!
        val candidates = listOf(EspCaptureCandidate(7, driftling.name, SafariEspRules.captureModel("armor_stand"), false, 9.0, 0.0),
            EspCaptureCandidate(8, "Driftling", true, false, 100.0, 9.0))
        assertEquals(7, SafariEspRules.capturedDisplay("Driftling", candidates))
        val memory = EspCaptureMemory()
        val id = java.util.UUID(0, 7)
        memory.aimed(id, driftling.name, 0)
        assertEquals(id, memory.caught("Driftling", 500))
        assertTrue(memory.hidden(id))
        assertFalse(memory.hidden(java.util.UUID(0, 8)))
    }
    @Test fun `sparkling alert has one heading with location below it`() {
        val panel = SparklingAlert.panel("Flitter", "Cavern 1 2 3")
        assertEquals("SPARKLING!", panel.title)
        assertEquals(listOf("Flitter", "Cavern 1 2 3"), panel.rows.map { it.label })
    }
    @Test fun `zero scale display models cannot retain ESP`() {
        assertFalse(SafariEspRules.modelVisible(0f, 0f, 0f))
        assertFalse(SafariEspRules.modelVisible(.0001f, 0f, 0f))
        assertFalse(SafariEspRules.modelVisible(Float.NaN, 1f, 1f))
        assertTrue(SafariEspRules.modelVisible(1f, 1f, 1f))
        assertTrue(SafariEspRules.modelVisible(-1f, 1f, 1f))
        assertTrue(SafariEspRules.modelVisible(1f, 1f, 0f))
    }
    @Test fun `capture retires only the matching display nearest aim leaving other critters alone`() {
        val candidates = listOf(
            EspCaptureCandidate(1, "Chuckwalla", true, false, 100.0, 25.0),
            EspCaptureCandidate(2, "Chuckwalla", true, false, 400.0, 1.0),
            EspCaptureCandidate(3, "Flitter", true, false, 1.0, 0.0),
            EspCaptureCandidate(4, "Chuckwalla", false, false, 1.0, 0.0),
            EspCaptureCandidate(5, "Chuckwalla", true, false, 6401.0, 0.0))
        assertEquals(2, SafariEspRules.capturedDisplay("Chuckwalla", candidates))
        assertNull(SafariEspRules.capturedDisplay("Gimmiegold", candidates))
        assertNull(SafariEspRules.capturedDisplay("Rockmite", listOf(EspCaptureCandidate(6, "Rockmite", true, true, 1.0, 0.0))))
    }
    @Test fun `capture retirement needs a recent matching throw and resets for another run`() {
        val memory = EspCaptureMemory()
        val first = java.util.UUID(0, 1)
        val other = java.util.UUID(0, 2)
        assertNull(memory.caught("Flitter", 100))
        memory.aimed(first, "Flitter", 100)
        memory.aimed(other, "Chuckwalla", 200)
        assertNull(memory.caught("Gimmiegold", 300))
        assertEquals(first, memory.caught("Flitter", 400))
        assertTrue(memory.hidden(first)); assertFalse(memory.hidden(other))
        assertNull(memory.caught("Flitter", 500))
        assertNull(memory.caught("Chuckwalla", 60201))
        memory.reset(); assertFalse(memory.hidden(first))
    }
}
