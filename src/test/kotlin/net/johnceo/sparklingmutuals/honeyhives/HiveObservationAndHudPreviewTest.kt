package net.johnceo.sparklingmutuals.honeyhives

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HiveObservationAndHudPreviewTest {
    @TempDir lateinit var dir: Path
    @Test fun `alert previews retain identity across different title ordering custom text and saved layout`() {
        ConfigManager.init(dir)
        ConfigManager.runCompleteTitle = "Finished custom safari"
        val previews = SmallAlerts.previews()
        val expected = mapOf(SafariHud.GEMS to "ALL GEMS READY!", SafariHud.BIRD_FOOD to "ALL BIRD FOOD COLLECTED!",
            SafariHud.INCENSE to "ALL INCENSE READY!", SafariHud.RUN_COMPLETE to "Finished custom safari", SafariHud.WARP to "WARP REMINDER")
        expected.forEach { (hud, title) ->
            val panel = previews.getValue(hud)
            assertEquals(title, panel.title)
            hud.layout.x = .1f; hud.layout.y = .2f; hud.layout.scale = 1.9f
            val bounds = hud.layout.bounds(1000, 600, panel.width(String::length), panel.height)
            assertEquals(100, bounds.x); assertEquals(120, bounds.y)
            assertTrue(bounds.contains(101.0, 121.0))
        }
        ConfigManager.save(); ConfigManager.init(dir)
        expected.forEach { (hud, title) ->
            assertEquals(title, SmallAlerts.previews().getValue(hud).title)
            assertEquals(1.9f, hud.layout.scale)
        }
    }
    @Test fun `formatted multiline TextDisplay and armor stand label text exposes the screenshot countdown`() {
        assertEquals(listOf("Refill in: 23m 33s"), HoneyHiveLabels.statusLines("§6Honeyhive\n§cRefill in: §a23m §c33s"))
        assertEquals(1_413_000L, HoneyHiveTracker.countdownMillis("§cRefill in: §a23m §c33s"))
        assertEquals(listOf("Click to loot!"), HoneyHiveLabels.statusLines("§6Honeyhive\n§eClick to loot!"))
        assertTrue(HoneyHiveLabels.statusLines("Honeyhive\nCLICK").isEmpty())
    }
    @Test fun `nearby waypoints receive independent status and equidistant or distant labels are rejected`() {
        val a = Hive(0, 10, 0); val b = Hive(3, 10, 0)
        val labels = listOf(HiveLabel(.5, 12.0, .5, "Refill in: 23m 33s"), HiveLabel(3.5, 12.0, .5, "Click to loot!"))
        assertEquals("Refill in: 23m 33s", HoneyHiveLabels.forHive(a, listOf(a,b), labels))
        assertEquals("Click to loot!", HoneyHiveLabels.forHive(b, listOf(a,b), labels))
        val tie = listOf(HiveLabel(2.0, 12.0, .5, "Click to loot!"))
        assertNull(HoneyHiveLabels.forHive(a, listOf(a,b), tie))
        assertNull(HoneyHiveLabels.forHive(b, listOf(a,b), tie))
        assertNull(HoneyHiveLabels.forHive(a, listOf(a,b), listOf(HiveLabel(.5, 18.0, .5, "Click to loot!"))))
    }
    @Test fun `full block and loot label cannot clear a saved personal cooldown`() {
        val times = mutableMapOf("a" to 9000000L)
        val tracker = HoneyHiveTracker(times)
        assertFalse(tracker.observe("a", null, 5, 1000))
        assertEquals(HiveState.EMPTY, tracker.state("a", 1000))
        assertEquals(9000000L, times["a"])
        val hive = Hive(-693, 94, 147)
        assertEquals("Click to loot!", HoneyHiveLabels.forHive(hive, listOf(hive),
            listOf(HiveLabel(-692.5, 93.28, 147.5, "Click to loot!"))))
    }
    @Test fun `provisional block observations do not overwrite saved profile timers`() {
        val temporary = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(temporary, provisional = true)
        tracker.observe("observed", "Refill in: 23m 33s", 0, 1000)
        assertEquals(HiveState.EMPTY, tracker.state("observed", 1000))
        assertNotNull(temporary["observed"])
        tracker.click("looted", 1100); tracker.chat("HIVE! Honey!", 1200)
        val saved = mutableMapOf("observed" to 5000000L)
        tracker.promote(saved, mutableSetOf())
        assertEquals(5000000L, saved["observed"])
        assertEquals(1200 + HoneyHiveRules.REFILL_MILLIS, saved["looted"])
        tracker.observe("observed", null, 5, 3000)
        assertEquals(5000000L, saved["observed"])
    }
    @Test fun `exact Hypixel Queen Bee proc refills only the looted hive and a new loot empties it again`() {
        val times = mutableMapOf("other" to 9_000_000L)
        val queen = mutableSetOf<String>()
        val tracker = HoneyHiveTracker(times, queen)
        tracker.click("hive", 100)
        tracker.chat("HIVE! You found honey!", 200)
        assertEquals(HiveState.EMPTY, HoneyHiveRules.state(200, times["hive"]))
        assertTrue(tracker.chat("QUEEN BEE! The Honeyhive instantly refilled with loot!", 300))
        assertEquals(HiveState.FULL, HoneyHiveRules.state(300, times["hive"]))
        tracker.observe("hive", null, 5, 5000)
        assertEquals(HiveState.FULL, HoneyHiveRules.state(5000, times["hive"]))
        assertEquals(9_000_000L, times["other"])
        tracker.click("hive", 5100); tracker.chat("HIVE! You found honey!", 5200)
        assertEquals(5200 + HoneyHiveRules.REFILL_MILLIS, times["hive"])
        assertFalse("hive" in queen)
    }

}
