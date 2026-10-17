package net.johnceo.sparklingmutuals.safari.automation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DowntimeAnnouncementTest {
    private val message = "Downtime requested: Alex (food); Steve (contest)"
    @Test fun `one client reports while other clients suppress the echoed combined message`() {
        val first = DowntimeAnnouncement()
        val second = DowntimeAnnouncement()
        first.queue(message, 0, 0); second.queue(message, 0, 1)
        assertNull(first.poll(499, message))
        assertEquals(message, first.poll(500, message))
        second.observe(message)
        assertNull(second.poll(1700, message))
        // A client that returns late has already heard the report.
        second.queue(message, 2000, 1)
        assertNull(second.poll(4000, message))
    }
    @Test fun `fallback reports if earlier client is absent and changes never announce stale requests`() {
        val state = DowntimeAnnouncement()
        state.queue(message, 0, 1)
        assertEquals(message, state.poll(1700, message))
        state.clear(); state.queue(message, 0, 0)
        val updated = "Downtime requested: Alex (food)"
        assertNull(state.poll(500, updated))
        assertEquals(updated, state.poll(1000, updated))
        state.clear(); state.queue(message, 0, 0)
        assertNull(state.poll(500, null))
    }
    @Test fun `long reasons share a single chat line retaining all normal-party names`() {
        val names = (1..8).associate { "Player$it" to "Long reason ".repeat(30) }
        val result = DowntimeAnnouncement.format(names)
        assertTrue(result.length <= 252)
        names.keys.forEach { assertTrue(result.contains("$it (")) }
    }
}
