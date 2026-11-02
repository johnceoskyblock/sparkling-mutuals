package net.johnceo.sparklingmutuals.safari.automation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DowntimeAnnouncementTest {
    private val reminder=DowntimeAnnouncement.TICKET_REMINDER
    @Test fun `only leader in Canyon sends once and waiting does not consume reminder`() {
        val state=DowntimeAnnouncement()
        state.queue(reminder,0,0)
        assertNull(state.poll(1000,reminder,false,true))
        assertNull(state.poll(2000,reminder,true,false))
        assertEquals("[Reminder] Everyone get your ticket!",state.poll(3000,reminder,true,true))
        state.queue(reminder,4000,0)
        assertNull(state.poll(5000,reminder,true,true))
        state.clear();state.queue(reminder,6000,0)
        assertEquals(reminder,state.poll(6500,reminder,true,true))
    }
    @Test fun `resolved requests and echoed reminders suppress pending message`() {
        val state=DowntimeAnnouncement()
        state.queue(reminder,0,0)
        assertNull(state.poll(500,null,true,true))
        state.queue(reminder,1000,0);state.observe(reminder)
        assertNull(state.poll(1500,reminder,true,true))
    }
}
