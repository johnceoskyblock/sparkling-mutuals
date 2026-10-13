package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.hud.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class SmallAlertsTest {
    @Test fun `inventory and warp alerts remain independent and expire without a vanilla title`() {
        val alerts = TimedHudAlerts()
        alerts.show(SafariHud.GEMS, "Gems", -1, 100)
        alerts.show(SafariHud.WARP, "Warp", -1, 200)
        assertEquals(setOf(SafariHud.GEMS, SafariHud.WARP), alerts.active(300).keys)
        assertEquals(setOf(SafariHud.WARP), alerts.active(4100).keys)
        assertTrue(alerts.active(4200).isEmpty())
    }
    @Test fun `same alert replaces itself and clearing inventory does not dismiss warp`() {
        val alerts = TimedHudAlerts()
        alerts.show(SafariHud.INCENSE, "Old", -1, 0)
        alerts.show(SafariHud.INCENSE, "New", 123, 2000)
        assertEquals(HudNotice("New", 123, 6000), alerts.active(4001)[SafariHud.INCENSE])
        alerts.show(SafariHud.WARP, "Warp", -1, 4000)
        alerts.clear(SafariHud.INCENSE)
        assertEquals(setOf(SafariHud.WARP), alerts.active(4001).keys)
    }
    @Test fun `small alert layouts persist positions and support fifteen percent scale`() {
        val properties = Properties()
        for (hud in listOf(SafariHud.GEMS, SafariHud.BIRD_FOOD, SafariHud.INCENSE, SafariHud.WARP)) {
            val layout = hud.layout
            layout.load(Properties(), hud.name.lowercase())
            assertEquals(1.5f, layout.scale)
            layout.move(100.0, 80.0, 800, 600, 120, 23)
            layout.scale = .15f
            layout.save(properties, hud.name.lowercase())
            layout.reset()
            assertEquals(1.5f, layout.scale)
            layout.load(properties, hud.name.lowercase())
            assertEquals(.15f, layout.scale)
            assertEquals(100, layout.bounds(800, 600, 120, 23).x)
            assertEquals(80, layout.bounds(800, 600, 120, 23).y)
            layout.reset()
        }
    }
}
