package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.SparklingMemory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SparklingMemoryTest {
    @Test fun `alerts and announcements are independent and happen once each per run`() {
        val memory = SparklingMemory()
        val id = UUID.randomUUID()
        assertFalse(memory.alert(id, false))
        assertTrue(memory.announce(id, true))
        assertTrue(memory.alert(id, true))
        assertFalse(memory.alert(id, true))
        assertFalse(memory.announce(id, true))
        memory.reset()
        assertTrue(memory.alert(id, true))
        assertTrue(memory.announce(id, true))
    }
}
