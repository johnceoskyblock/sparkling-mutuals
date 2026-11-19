package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import kotlin.math.round
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HudRasterTest {
    @Test fun `font pixels remain whole screen pixels at every GUI scale`() {
        for (ratio in listOf(1f,2f,3f,4f,5f)) for (scale in listOf(.15f,.6f,.85f,1f,1.2f,1.5f,2f)) {
            val aligned = HudRaster.scale(scale,ratio,3f)
            assertEquals(round(aligned*ratio),aligned*ratio,.00001f)
            assertTrue(aligned > 0 && aligned <= 3f)
        }
        assertEquals(.83f,HudRaster.scale(.83f,0f,3f))
    }

    @Test fun `aligned drawing bounds and editor movement agree without overwriting saved scale`() {
        val layout = HudLayout(.5f,.5f,.83f)
        val bounds = layout.bounds(640,360,100,40,3f)
        assertEquals(67,bounds.width)
        assertEquals(27,bounds.height)
        assertEquals(2f/3f,layout.effectiveScale(640,360,100,40))
        layout.move(80.0,90.0,640,360,100,40)
        val moved = layout.bounds(640,360,100,40)
        assertEquals(80,moved.x)
        assertEquals(90,moved.y)
        val saved = Properties().apply {layout.save(this,"test")}
        assertEquals("0.83",saved.getProperty("hud.test.scale"))
        assertEquals(.83f,layout.effectiveScale(640,360,100,40,0f))
    }

    @Test fun `pixel alignment never grows a panel beyond the viewport`() {
        val layout = HudLayout(0f,0f,1.2f)
        val bounds = layout.bounds(120,60,100,50,2f)
        assertTrue(bounds.width <= 120 && bounds.height <= 60)
        val tiny = layout.bounds(40,20,100,50,1f)
        assertTrue(tiny.width <= 40 && tiny.height <= 20)
    }
}
