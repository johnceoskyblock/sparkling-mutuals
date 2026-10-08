package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.EspMotion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class EspMotionTest {
    @Test fun `stationary nearby critter stops blocking completion and movement restores it`() {
        val state = EspMotion(); val id = UUID.randomUUID()
        state.observe(id, "Driftling", 1.0, 2.0, 3.0, 900.0, 0)
        assertTrue(state.current(id, 1999))
        assertFalse(state.current(id, 2000))
        state.observe(id, "Driftling", 1.1, 2.0, 3.0, 900.0, 2100)
        assertTrue(state.current(id, 2100))
        assertFalse(state.current(id, 4100))
        state.reset(); assertTrue(state.current(id, 5000))
    }
    @Test fun `time outside thirty blocks cannot retire a distant critter`() {
        val state = EspMotion(); val id = UUID.randomUUID()
        state.observe(id, "Macaw", 0.0, 0.0, 0.0, 901.0, 0)
        assertTrue(state.current(id, 10000))
        state.observe(id, "Macaw", 0.0, 0.0, 0.0, 900.0, 10000)
        assertTrue(state.current(id, 11999)); assertFalse(state.current(id, 12000))
        state.observe(id, "Macaw", 0.0, 0.0, 0.0, 901.0, 12001)
        assertFalse(state.current(id, 12001))
        state.observe(id, "Macaw", 1.0, 0.0, 0.0, 901.0, 13000)
        assertTrue(state.current(id, 13000))
    }
    @Test fun `only requested moving species use stationary detection`() {
        val state = EspMotion()
        val moving = listOf("Driftling", "Foxtrot", "Bluebird", "Parakeet", "Macaw", "Solsnatcher",
            "Litterbug", "Tepid", "Mantis Shrimp", "Nozzlenose", "Gemzie", "Shuddersquid")
        for (name in moving + listOf("Rockmite", "Hideonfloor", "Hideyho", "Gazer", "Chuckwalla")) {
            val id = UUID.randomUUID()
            state.observe(id, name, 0.0, 0.0, 0.0, 1.0, 0)
            assertEquals(name !in moving, state.current(id, 2000), name)
        }
    }
}
