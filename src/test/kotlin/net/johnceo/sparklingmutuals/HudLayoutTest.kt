package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.hud.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class HudLayoutTest {
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
