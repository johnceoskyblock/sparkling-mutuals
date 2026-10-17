package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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
    @Test fun `capsule and escape UUID replacements do not repeat species notifications`() {
        val memory = SparklingMemory()
        val original = UUID.randomUUID()
        assertTrue(memory.alert(original, true, "Mantis Shrimp"))
        assertTrue(memory.announce(original, true, "Mantis Shrimp"))
        repeat(3) {
            val replacement = UUID.randomUUID()
            assertFalse(memory.alert(replacement, true, "Mantis Shrimp"))
            assertFalse(memory.announce(replacement, true, "Mantis Shrimp"))
        }
        assertTrue(memory.alert(UUID.randomUUID(), true, "Gazer"))
        assertTrue(memory.announce(UUID.randomUUID(), true, "Gazer"))
        memory.reset()
        assertTrue(memory.alert(UUID.randomUUID(), true, "Mantis Shrimp"))
        assertTrue(memory.announce(UUID.randomUUID(), true, "Mantis Shrimp"))
    }

}
