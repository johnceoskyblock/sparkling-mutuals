package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContactMetadataQueueTest {
    @Test fun `repeated updates coalesce without moving the oldest identity behind later work`() {
        val queue=ContactMetadataQueue()
        assertTrue(queue.add(ContactDiscovery("first",1)))
        assertTrue(queue.add(ContactDiscovery("second",2)))
        repeat(20){assertTrue(queue.add(ContactDiscovery("first",3)))}
        assertEquals(2,queue.size)
        assertEquals(listOf(ContactDiscovery("first",3)),queue.drain(1))
        assertEquals(listOf(ContactDiscovery("second",2)),queue.drain(16))
        assertEquals(0,queue.size)
    }
    @Test fun `overflow is explicit and a batch never exceeds sixteen identities`() {
        val queue=ContactMetadataQueue()
        repeat(64){assertTrue(queue.add(ContactDiscovery("$it",1)))}
        assertFalse(queue.add(ContactDiscovery("overflow",1)))
        assertTrue(queue.add(ContactDiscovery("0",2)))
        val first=queue.drain(16)
        assertEquals(16,first.size);assertEquals(ContactDiscovery("0",2),first.first())
        assertEquals(48,queue.size)
        assertTrue(queue.add(ContactDiscovery("overflow",1)))
        queue.reset();assertEquals(0,queue.size);assertTrue(queue.drain(16).isEmpty())
    }
}
