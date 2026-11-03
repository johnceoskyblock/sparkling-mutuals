package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardSaleSidebarLayoutTest {
    @Test fun `panel fits the usable right margin across supported window sizes`() {
        for((width,height,right) in listOf(Triple(960,540,568),Triple(640,360,408),Triple(800,240,488),Triple(480,270,240))) {
            val box=ShardSaleSidebarLayout.bounds(width,height,right,37)!!
            assertTrue(box.x>right);assertTrue(box.x+box.width<=width-4)
            assertTrue(box.y>=4);assertTrue(box.y+box.height<=height-4)
            assertTrue(box.scale in .75f..1f);assertTrue(box.rows in 1..12)
            assertEquals(37-box.rows,box.clampScroll(1000,37));assertEquals(0,box.clampScroll(-5,37))
        }
    }
    @Test fun `insufficient space hides instead of overlapping inventory or unreadably shrinking`() {
        for((width,height,right) in listOf(Triple(320,240,248),Triple(640,24,408),Triple(600,300,590),Triple(0,300,0)))
            assertNull(ShardSaleSidebarLayout.bounds(width,height,right,37))
    }
    @Test fun `top ten panel has compact heading and rows without helper bands`() {
        val box=ShardSaleSidebarLayout.bounds(640,360,408,10)!!
        assertTrue(box.height<=150,"No owned-count, session or profile footer bands")
        assertEquals(0,box.rowAt(box.x+8.0,box.y+20*box.scale.toDouble()))
    }
    @Test fun `empty and short lists keep scroll clamped and pointer rows bounded`() {
        for(count in listOf(0,1,5,37)) {
            val box=ShardSaleSidebarLayout.bounds(960,540,568,count)!!
            assertEquals(0,box.clampScroll(10,count.coerceAtMost(box.rows)))
            assertFalse(box.contains(box.x-1.0,box.y+10.0));assertFalse(box.contains(box.x+box.width.toDouble(),box.y+10.0))
            assertTrue(box.contains(box.x+1.0,box.y+1.0))
            assertNull(box.rowAt(box.x+1.0,box.y+1.0))
            assertEquals(0,box.rowAt(box.x+1.0,box.y+(ShardSaleSidebarLayout.HEADER+1)*box.scale.toDouble()))
            assertNull(box.rowAt(box.x+1.0,box.y+box.height-1.0))
        }
    }
}
