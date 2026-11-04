package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HudSnappingTest {
    @Test fun `snaps to peer edges and centers using the closest alignment`() {
        val peers = listOf(HudBounds(120, 60, 80, 70), HudBounds(240, 180, 70, 40))
        assertEquals(HudSnapping.Position(120, 130, false, false, 120, 130),
            HudSnapping.position(123.0, 132.0, 640, 360, 40, 30, peers))
        assertEquals(HudSnapping.Position(140, 80, false, false, 160, 95),
            HudSnapping.position(142.0, 78.0, 640, 360, 40, 30, peers))
        assertEquals(HudSnapping.Position(270, 173, false, false, 310, null),
            HudSnapping.position(273.0, 173.0, 640, 360, 40, 30, peers))
    }

    @Test fun `drag aligns to centers and edges inside SafariUtils thresholds only`() {
        assertEquals(HudSnapping.Position(270,155,true,true),HudSnapping.position(275.0,150.0,640,360,100,50))
        assertEquals(HudSnapping.Position(276,149,false,false),HudSnapping.position(276.0,149.0,640,360,100,50))
        assertEquals(HudSnapping.Position(0,310,false,false),HudSnapping.position(3.0,307.0,640,360,100,50))
        assertEquals(HudSnapping.Position(540,0,false,false),HudSnapping.position(537.0,3.0,640,360,100,50))
        assertEquals(4,HudSnapping.position(4.0,80.0,640,360,100,50).x)
    }

    @Test fun `snap uses rendered panel dimensions and clamps oversized or offscreen panels`() {
        val snapped=HudSnapping.position(80.0,55.0,320,180,160,80)
        assertEquals(HudSnapping.Position(80,50,true,true),snapped)
        assertEquals(0,HudSnapping.position(-100.0,10000.0,320,180,400,200).x)
        assertEquals(0,HudSnapping.position(-100.0,10000.0,320,180,400,200).y)
    }

    @Test fun `snapped positions and single pixel nudges survive normalized storage exactly`() {
        for((width,height) in listOf(320 to 180,640 to 360,853 to 480,1920 to 1080)) {
            val layout=HudLayout(0f,0f)
            for(x in 0..width-100) {
                layout.move(x.toDouble(),20.0,width,height,100,50)
                assertEquals(x,layout.bounds(width,height,100,50).x,"$width pixel $x")
            }
            val snap=HudSnapping.position((width-100)/2.0+3, (height-50)/2.0+3,width,height,100,50)
            layout.move(snap.x.toDouble(),snap.y.toDouble(),width,height,100,50)
            val properties=Properties().also { layout.save(it,"test") }
            val restored=HudLayout(0f,0f).apply { load(properties,"test") }.bounds(width,height,100,50)
            assertEquals(snap.x,restored.x)
            assertEquals(snap.y,restored.y)
        }
    }
}
