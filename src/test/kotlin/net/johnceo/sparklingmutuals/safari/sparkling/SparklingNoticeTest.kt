package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingNoticeTest {
    @Test fun `particle first can announce unknown then the identified species independently`() {
        val id = UUID.randomUUID()
        val memory = SparklingMemory()
        val queue = SparklingNotices()
        assertTrue(memory.alert(id, true, SparklingAlert.UNKNOWN))
        assertTrue(memory.announce(id, true, SparklingAlert.UNKNOWN))
        queue.add(SparklingNotice(id, SparklingAlert.UNKNOWN, "Haunted 0 50 -60", unknown = true))
        assertEquals("SPARKLING Critter Unknown! (Haunted 0 50 -60)", queue.next()!!.message())
        assertFalse(memory.alert(id, true, SparklingAlert.UNKNOWN))
        assertFalse(memory.announce(id, true, SparklingAlert.UNKNOWN))
        assertTrue(memory.alert(id, true, "Gazer"))
        assertTrue(memory.announce(id, true, "Gazer"))
        queue.add(SparklingNotice(id, "Gazer", "Haunted 0 50 -60"))
        assertEquals("SPARKLING Gazer! (Haunted 0 50 -60)", queue.next()!!.message())
        assertNull(queue.next())
    }

    @Test fun `identification cancels only matching unsent unknown notices`() {
        val points = SparklingParticleLocations()
        val first = points.observe(true, true, 0.0, 50.0, -60.0, 0)!!
        val other = points.observe(true, true, 10.0, 50.0, -60.0, 0)!!
        val queue = SparklingNotices()
        queue.add(SparklingNotice(first.id, SparklingAlert.UNKNOWN, "first", unknown = true))
        queue.add(SparklingNotice(other.id, SparklingAlert.UNKNOWN, "other", unknown = true))
        queue.add(SparklingNotice(first.id, "Gazer", "first"))
        queue.identify(points.identify(0.0, 49.0, -60.0))
        assertEquals(other.id, queue.next()!!.id)
        assertEquals("Gazer", queue.next()!!.name)
        assertNull(queue.next())
    }

    @Test fun `named scan retires same tick particles before any unknown notification`() {
        val points = SparklingParticleLocations()
        val point = points.observe(true, true, 0.0, 50.0, -60.0, 0)!!
        assertEquals(listOf(point.id), points.identify(0.0, 49.0, -60.0))
        assertTrue(points.unknown().isEmpty())
        points.observe(true, true, 0.1, 50.0, -60.0, 100)
        assertTrue(points.unknown().isEmpty())
    }
}
