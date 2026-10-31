package net.johnceo.sparklingmutuals.alerts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WarpContestReminderTest {
    @Test fun `new contest waits for reformed party and sends once`() {
        val reminder=WarpContestReminder()
        assertFalse(reminder.poll(10,true,true,4))
        assertFalse(reminder.poll(11,true,true,1))
        assertFalse(reminder.poll(11,true,null,3))
        assertTrue(reminder.poll(11,true,true,3))
        assertFalse(reminder.poll(11,true,true,3))
        assertFalse(reminder.poll(12,true,false,3))
        assertFalse(reminder.poll(12,true,true,3))
        assertFalse(reminder.poll(13,false,true,3))
        assertFalse(reminder.poll(13,true,true,3))
    }
}
