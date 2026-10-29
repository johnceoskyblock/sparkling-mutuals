package net.johnceo.sparklingmutuals.safari

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DoomspiralThrowTimerTest {
    private val trigger = "Something stirs in the Haunted Biome..."
    @TempDir lateinit var directory: Path
    @Test fun `server ticks count to throw cue and then expire`() {
        val timer = DoomspiralCountdown()
        assertNull(timer.remaining)
        assertTrue(timer.trigger(trigger, true, 75, 3))
        assertEquals(72, timer.remaining)
        repeat(71) { timer.serverTick() }
        assertEquals(1, timer.remaining)
        timer.serverTick()
        assertEquals(0, timer.remaining)
        repeat(39) { timer.serverTick() }
        assertEquals(0, timer.remaining)
        timer.serverTick()
        assertNull(timer.remaining)
    }
    @Test fun `only exact system trigger inside Safari arms the timer`() {
        val timer = DoomspiralCountdown()
        assertFalse(timer.trigger(trigger, false, 75, 3))
        for (raw in listOf("Party > Someone: $trigger", "Guild > Someone: $trigger", "[SM] $trigger", "$trigger extra"))
            assertFalse(timer.trigger(raw, true, 75, 3))
        assertTrue(timer.trigger("\u00a75$trigger", true, 75, 3))
    }
    @Test fun `duplicate messages or capture cannot restart a timer until a new run`() {
        val timer = DoomspiralCountdown()
        timer.trigger(trigger, true, 75, 3)
        repeat(10) { timer.serverTick() }
        assertFalse(timer.trigger(trigger, true, 75, 3))
        assertEquals(62, timer.remaining)
        timer.clear()
        assertNull(timer.remaining)
        assertFalse(timer.trigger(trigger, true, 75, 3))
        timer.reset()
        assertTrue(timer.trigger(trigger, true, 75, 3))
    }
    @Test fun `settings persist calibration and HUD style is available`() {
        ConfigManager.init(directory)
        assertTrue(ConfigManager.doomspiralThrowTimer)
        assertEquals(98, ConfigManager.doomspiralEmergenceTicks)
        assertEquals(4, ConfigManager.doomspiralThrowLeadTicks)
        val settings = SafariSettings()
        settings.safari.capture.doomTimer = false
        settings.safari.capture.doomTicks = 80
        settings.safari.capture.doomLead = 4
        settings.apply()
        ConfigManager.init(directory)
        assertFalse(ConfigManager.doomspiralThrowTimer)
        assertEquals(80, ConfigManager.doomspiralEmergenceTicks)
        assertEquals(4, ConfigManager.doomspiralThrowLeadTicks)
        assertTrue(AppearanceConfig.panels.containsKey("doom_throw"))
        assertEquals("THROW NOW", DoomspiralThrowTimer.panel(0).rows.single().label)
    }
}
