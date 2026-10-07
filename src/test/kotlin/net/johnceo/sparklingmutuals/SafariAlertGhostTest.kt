package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariAlertGhostTest {
    @Test fun `sparkling alert has one heading with location below it`() {
        val panel = SparklingAlert.panel("Flitter", "Cavern 1 2 3")
        assertEquals("SPARKLING Flitter!", panel.title)
        assertEquals(listOf("Cavern 1 2 3"), panel.rows.map { it.label })
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
        assertNull(memory.caught("Chuckwalla", 10201))
        memory.reset(); assertFalse(memory.hidden(first))
    }
}
