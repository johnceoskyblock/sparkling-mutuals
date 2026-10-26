package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HudLayoutTest {
    @Test fun `curated layouts retain physical placement and size across GUI scales and persistence`() {
        val profile = net.johnceo.sparklingmutuals.config.FreshInstallDefaults.main()
        for ((id, size) in mapOf("progress" to (150 to 98), "captures" to (150 to 166),
            "missing" to (180 to 140), "sparklings" to (220 to 45), "gems" to (104 to 23),
            "bird_food" to (175 to 23), "incense" to (145 to 23), "run_complete" to (100 to 23), "warp" to (100 to 23))) {
            val layout = HudLayout(0f, 0f).apply { load(profile, id) }
            val reference = layout.bounds(640, 360, size.first, size.second)
            val saved = Properties().also { layout.save(it, id) }
            val restored = HudLayout(0f, 0f).apply { load(saved, id) }
            for (guiScale in 1..6) {
                val width = 1920 / guiScale; val height = 1080 / guiScale
                val actual = restored.bounds(width, height, size.first, size.second)
                assertEquals(reference.x * 3.0, actual.x * guiScale.toDouble(), guiScale + 3.0, id)
                assertEquals(reference.y * 3.0, actual.y * guiScale.toDouble(), guiScale + 3.0, id)
                assertEquals(reference.width * 3.0, actual.width * guiScale.toDouble(), guiScale + 3.0, id)
                assertTrue(actual.x >= 0 && actual.y >= 0 && actual.x + actual.width <= width && actual.y + actual.height <= height, id)
            }
        }
        val oversized = HudLayout(0f, 0f).apply { load(profile, "progress") }.bounds(200, 100, 900, 900)
        assertTrue(oversized.width <= 200 && oversized.height <= 100)
    }
    @Test fun `oversized panels clamp safely on small windows`() {
        val layout = HudLayout(.8f, .8f).apply { scale = 2f }
        assertEquals(HudBounds(0, 0, 300, 200), layout.bounds(100, 80, 150, 100))
        layout.move(80.0, -20.0, 100, 80, 150, 100)
        assertEquals(0f, layout.x)
        assertEquals(0f, layout.y)
    }
    @Test fun `saved positions survive reload and resolution changes`() {
        val layout = HudLayout(.1f, .2f)
        layout.move(120.0, 60.0, 600, 300, 100, 50)
        layout.scale = 1.5f
        val properties = Properties()
        layout.save(properties, "progress")
        val restored = HudLayout(0f, 0f)
        restored.load(properties, "progress")
        val bounds = restored.bounds(1200, 600, 100, 50)
        assertEquals(240, bounds.x)
        assertEquals(120, bounds.y)
        assertEquals(150, bounds.width)
        assertTrue(bounds.contains(241.0, 121.0))
        assertFalse(bounds.contains(391.0, 121.0))
    }
    @Test fun `invalid saved floats use safe defaults`() {
        val properties = Properties().apply {
            setProperty("hud.missing.x", "NaN")
            setProperty("hud.missing.y", "Infinity")
            setProperty("hud.missing.scale", "NaN")
        }
        val layout = HudLayout(.3f, .2f)
        layout.load(properties, "missing")
        assertEquals(.3f, layout.x)
        assertEquals(.2f, layout.y)
        assertEquals(1f, layout.scale)
    }
}
