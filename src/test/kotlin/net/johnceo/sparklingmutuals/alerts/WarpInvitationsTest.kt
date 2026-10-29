package net.johnceo.sparklingmutuals.alerts

import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WarpInvitationsTest {
    @Test fun `one invitation every two seconds skips members and stops at warp`() {
        val queue = WarpInvitations()
        queue.start(23000)
        queue.add("Foxtrot", listOf("AlreadyHere", "First", "Second", "Third"))
        assertEquals("First", queue.next(1000, setOf("alreadyhere")))
        assertNull(queue.next(2999, emptySet()))
        assertEquals("Second", queue.next(3000, setOf("FIRST")))
        assertNull(queue.next(23000, emptySet()))
        assertNull(queue.next(25000, emptySet()))
    }
    @Test fun `repeated detections and shared names cannot restart invitations`() {
        val queue = WarpInvitations()
        queue.start(23000)
        queue.add("Foxtrot", listOf("First", "Second"))
        assertEquals("First", queue.next(0, emptySet()))
        queue.add("Foxtrot", listOf("First", "Second"))
        queue.add("Flitter", listOf("FIRST", "Third"))
        assertEquals("Second", queue.next(2000, emptySet()))
        assertEquals("Third", queue.next(4000, emptySet()))
        assertNull(queue.next(6000, emptySet()))
        queue.stop()
        queue.add("Shyworm", listOf("Late"))
        assertNull(queue.next(7000, emptySet()))
    }
    @Test fun `new run clears invitations and deduplication`() {
        val queue = WarpInvitations()
        queue.start(23000)
        queue.add("Foxtrot", listOf("First", "Old"))
        assertEquals("First", queue.next(0, emptySet()))
        queue.start(50000)
        queue.add("Foxtrot", listOf("First"))
        assertEquals("First", queue.next(24000, emptySet()))
        assertNull(queue.next(26000, emptySet()))
    }
    @Test fun `warp lists preserve order and only accept IGNs`() {
        WarpList.load(Properties())
        WarpList.set("Mantis Shrimp", "First, Second FIRST; third_3 /p @bad more-than-sixteen-letters")
        val saved = Properties().also(WarpList::save)
        WarpList.load(saved)
        assertEquals(listOf("First", "Second", "third_3"), WarpList.names("Mantis Shrimp"))
        assertEquals(26, WarpList.critters.size)
        WarpList.load(Properties())
    }
}
