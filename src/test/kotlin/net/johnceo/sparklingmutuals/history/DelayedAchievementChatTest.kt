package net.johnceo.sparklingmutuals.history

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DelayedAchievementChatTest {
    @Test fun `achievement lines wait until catch block has finished and keep their order`() {
        val queue=DelayedAchievementChat<String>()
        queue.add(listOf("name","description"),1000)
        assertTrue(queue.drain(1499).isEmpty())
        queue.postpone(2000)
        queue.add(listOf("second name","second description"),2100)
        assertTrue(queue.drain(2599).isEmpty())
        assertEquals(listOf("name","description","second name","second description"),queue.drain(2600))
        assertTrue(queue.drain(5000).isEmpty())
    }

    @Test fun `disconnect or disabling achievements cancels pending announcements`() {
        val queue=DelayedAchievementChat<String>()
        queue.add(listOf("old session"),1000)
        queue.clear()
        assertTrue(queue.drain(5000).isEmpty())
        queue.add(listOf("new session"),6000)
        assertEquals(listOf("new session"),queue.drain(6500))
    }
}
