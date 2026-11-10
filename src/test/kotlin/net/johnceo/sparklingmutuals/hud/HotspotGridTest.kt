package net.johnceo.sparklingmutuals.hud
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class HotspotGridTest {
 @Test fun `four players form two rows of two separate cards within the panel`() {
  val grid=HotspotGrid(143,4)
  val boxes=(0..3).map(grid::cell)
  assertEquals(boxes[0].y,boxes[1].y);assertEquals(boxes[2].y,boxes[3].y)
  assertTrue(boxes[1].x>boxes[0].x+boxes[0].width)
  assertTrue(boxes[2].y>boxes[0].y+boxes[0].height)
  assertEquals(boxes[0].x,boxes[2].x)
  assertTrue(boxes.all { it.x>=0 && it.y>=0 && it.x+it.width<=143 && it.y+it.height<=grid.height })
 }
 @Test fun `small parties do not reserve an empty second row`() {
  assertEquals(HotspotGrid(143,1).height,HotspotGrid(143,2).height)
  assertTrue(HotspotGrid(143,3).height>HotspotGrid(143,2).height)
 }
 @Test fun `biomes have distinct established colors and unknown stays neutral`() {
  assertEquals(4,SafariBiome.entries.map(HotspotGrid::biomeColor).toSet().size)
  assertEquals(0xFF55FF55.toInt(),HotspotGrid.biomeColor(SafariBiome.FOREST))
  assertEquals(HudRow.GRAY,HotspotGrid.biomeColor(null))
 }
}
