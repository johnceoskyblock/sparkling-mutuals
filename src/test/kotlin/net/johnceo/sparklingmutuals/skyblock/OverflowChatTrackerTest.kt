package net.johnceo.sparklingmutuals.skyblock

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OverflowChatTrackerTest {
    @Test fun `absolute maxed XP levels up once and ignores repeated packets`() {
        val tracker = OverflowChatTracker()
        assertTrue(tracker.observe("+10 Hunting (4,299,999/0)").isEmpty())
        val change = tracker.observe("§a100 Health   +10 Hunting (4,300,001/0)   100 Mana").single()
        assertEquals(OverflowLevelChange("Hunting", 50, 51), change)
        assertTrue(tracker.observe("+10 Hunting (4,300,001/0)").isEmpty())
        assertTrue(tracker.observe("+10 Combat (120/750)").isEmpty())
        assertTrue(tracker.observe("+10 Social (99,999,999/0)").isEmpty())
        tracker.clear()
        assertTrue(tracker.observe("+10 Hunting (9,999,999/0)").isEmpty())
    }
    @Test fun `format matches level up styling without fabricated rewards`() {
        val change = OverflowLevelChange("Combat", 61, 62)
        assertTrue(change.message(false).contains("LXI➜§3LXII"))
        assertTrue(change.message(true).contains("61➜§362"))
        assertFalse(change.message(true).contains("Flexing"))
        assertFalse(change.message(true).contains("REWARDS"))
    }
}
