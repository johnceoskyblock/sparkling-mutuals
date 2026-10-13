package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.contest.HoneyHiveTracker
import net.johnceo.sparklingmutuals.contest.HoneyHiveRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HoneyHiveTrackerTest {
    @Test fun `a fresh click at the refill boundary upgrades an earlier empty click`() {
        val times = mutableMapOf("a" to 200L)
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("a", 300)
        assertTrue(tracker.chat("HIVE! Honey!", 400))
        assertEquals(400 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `empty clicks cannot steal a later full hives successful loot`() {
        val times = mutableMapOf("a" to 9000L)
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("b", 200)
        tracker.chat("HIVE! Honey!", 300)
        assertEquals(9000L, times["a"])
        assertEquals(300 + HoneyHiveRules.REFILL_MILLIS, times["b"])
    }
    @Test fun `new provisional loot overrides older saved Queen Bee readiness when profile arrives`() {
        val times = mutableMapOf<String, Long>()
        val full = mutableSetOf("a")
        val tracker = HoneyHiveTracker(mutableMapOf())
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.promote(times, full)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertFalse("a" in full)
    }
    @Test fun `pre-fix timers are cleared once and subsequent restarts retain independent profile deadlines`() {
        val saved = java.util.Properties().apply {
            setProperty("refill.player.profile-a.a", "100000")
            setProperty("refill.player.profile-a.b", "200000")
            setProperty("refill.player.profile-b.a", "300000")
            setProperty("refill.player.profile-a.-730,132,184", "400000")
        }
        assertTrue(HoneyHiveRules.savedRefills(saved, 0).isEmpty())
        saved.setProperty("tracking.version", "2")
        saved.setProperty("full.player.profile-a.c", "1")
        saved.setProperty("full.player.unknown.a", "1")
        val restored = HoneyHiveRules.savedRefills(saved, 150000)
        assertEquals(mapOf("b" to 200000L), restored["player.profile-a"])
        assertEquals(mapOf("a" to 300000L), restored["player.profile-b"])
        assertEquals(mapOf("player.profile-a" to setOf("c")), HoneyHiveRules.savedQueenFull(saved))
        assertTrue(HoneyHiveRules.parse("-730 132 184 -").isEmpty())
    }
    @Test fun `Queen Bee allows another loot immediately and that deadline survives tracker recreation`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 300)
        tracker.click("a", 400); tracker.chat("HIVE! Honey!", 500)
        val restarted = HoneyHiveTracker(times)
        assertFalse(restarted.chat("HIVE! Honey!", 700))
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `a repeat click before delayed Queen Bee chat is retained for its later loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 300)
        tracker.click("a", 350); tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        tracker.chat("HIVE! Honey!", 500)
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `Queen Bee stays full after delayed empty server state until a new confirmed loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        tracker.observe("a", null, 0, 3000)
        tracker.observe("a", "Refill in: 59m 59s", 0, 4000)
        assertNull(times["a"])
    }
    @Test fun `profile promotion retains queued click and Queen Bee correlation`() {
        val provisional = mutableMapOf<String, Long>()
        val actual = mutableMapOf("older" to 9000L)
        val queen = mutableSetOf<String>()
        val tracker = HoneyHiveTracker(provisional, queen)
        tracker.click("a", 100)
        tracker.promote(actual, queen)
        tracker.chat("HIVE! Honey!", 200)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, actual["a"])
        val newActual = mutableMapOf<String, Long>()
        tracker.promote(newActual, queen)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 300)
        assertNull(newActual["a"])
        tracker.resetSession()
        HoneyHiveTracker(newActual, queen).observe("a", null, 0, 9000)
        assertNull(newActual["a"], "Queen Bee readiness must survive reconnects")
    }
    @Test fun `rapid clicks consume distinct hives in order and Queen Bee targets confirmed loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100)
        tracker.click("b", 200)
        assertTrue(tracker.chat("HIVE! You found honey!", 300))
        assertEquals(300 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertNull(times["b"])
        assertTrue(tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400))
        assertNull(times["a"])
        tracker.chat("HIVE! More honey!", 500)
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["b"])
        assertNull(times["a"])
    }
    @Test fun `duplicate clicks cannot consume another hives message and party text cannot loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("a", 110); tracker.click("b", 200)
        assertFalse(tracker.chat("Party > Foo: HIVE! Honey!", 250))
        tracker.chat("HIVE! Honey!", 300); tracker.chat("HIVE! Honey!", 400)
        assertEquals(setOf("a", "b"), times.keys)
    }
    @Test fun `unmatched or expired chat never guesses a nearby hive`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        assertFalse(tracker.chat("HIVE! Honey!", 0))
        tracker.click("a", 10)
        assertFalse(tracker.chat("HIVE! Honey!", 10_000))
        assertTrue(times.isEmpty())
    }
    @Test fun `full server state and countdown override persisted state after chat grace`() {
        val times = mutableMapOf("a" to 9_000_000L)
        val tracker = HoneyHiveTracker(times)
        tracker.observe("a", "Click to loot!", 0, 1_000)
        assertNull(times["a"])
        tracker.observe("a", "Refill in: 12m 34s", 5, 2_000)
        assertEquals(2_000 + 754_000, times["a"])
        tracker.observe("a", null, 5, 3_000)
        assertNull(times["a"])
    }
    @Test fun `stale server state cannot immediately undo loot or Queen Bee`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.observe("a", "Click to loot!", 5, 300)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        tracker.observe("a", "Refill in: 59m 59s", 0, 500)
        assertNull(times["a"])
    }
    @Test fun `empty block observations do not slide the one hour deadline forever`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.observe("a", null, 0, 100)
        tracker.observe("a", null, 0, 20_000)
        assertEquals(100 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        tracker.observe("a", null, 0, 100 + HoneyHiveRules.REFILL_MILLIS)
        assertEquals(HoneyHiveRules.state(100 + HoneyHiveRules.REFILL_MILLIS, times["a"]), net.johnceo.sparklingmutuals.contest.HiveState.FULL)
    }
    @Test fun `server countdown understands seconds and minutes without accepting unrelated text`() {
        assertEquals(34_000L, HoneyHiveTracker.countdownMillis("Refill in: 34s"))
        assertEquals(120_000L, HoneyHiveTracker.countdownMillis("Refill in: 2m"))
        assertNull(HoneyHiveTracker.countdownMillis("Other hive refills in 2m"))
    }
}
