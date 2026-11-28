package net.johnceo.sparklingmutuals.safari.esp

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class RecatchWaypointTest {
    @Test fun `only a confirmed throw pins the body and escape has a bounded followup`() {
        val previous = ConfigManager.recatchWaypoint
        ConfigManager.recatchWaypoint = true
        try {
            val memory = EspCaptureMemory()
            val id = UUID.randomUUID()
            val position = Vec3(1.0, 40.0, 2.0)
            memory.observeBodies(listOf(EspPityBody(id, "Foxtrot", position, named = true)), 100)
            memory.throwingAt(120, id, "Foxtrot")
            assertTrue(memory.recatchPins(120).isEmpty())
            memory.threw("Foxtrot", 150)
            assertEquals(position, memory.recatchPins(150).single().position)
            memory.escaped("Foxtrot", 500)
            assertEquals("Foxtrot", memory.recatchPins(8499).single().species)
            assertTrue(memory.recatchPins(8500).isEmpty())

            memory.aimed(id, "Foxtrot", 9000, confirmedThrow = true)
            assertEquals(position, memory.recatchPins(9000).single().position)
            memory.caught("Foxtrot", 9200)
            assertTrue(memory.recatchPins(9200).isEmpty())
        } finally {
            ConfigManager.recatchWaypoint = previous
        }
    }
}
