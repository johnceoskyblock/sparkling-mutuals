package net.johnceo.sparklingmutuals.safari.resources

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FloorDropMetadataTest {
    private fun triple(id: Int, description: String) = List(3) { FloorDisplay(id+it,-110.0,60.0,50.0,description) }

    @Test fun `uninitialized and decorative displays cannot poison gem anchors`() {
        val predictor = FloorDropPredictor()
        assertTrue(predictor.scan(triple(7,"minecraft:air:Air") + triple(4,"minecraft:player_head:Head")).isEmpty())
        val gems = (0..5).flatMap { triple(10+it*3,"minecraft:string:String") }
        val result = predictor.scan(gems)
        assertEquals(listOf(FloorDropType.LIME,FloorDropType.PURPLE,FloorDropType.ORANGE,
            FloorDropType.LIME,FloorDropType.PURPLE,FloorDropType.ORANGE),result.map { it.type })
        assertEquals(6,predictor.knownDrops.size)
        // An incomplete later view keeps the original anchor, rather than relabeling Orange as Lime.
        assertEquals(FloorDropType.ORANGE,predictor.scan(gems.takeLast(3)).single().type)
    }

    @Test fun `catalog and ID layout distinguish coins from generic Haunted loot`() {
        val predictor = FloorDropPredictor()
        val result = predictor.scan((0..30).flatMap { slot ->
            val (x, y, z) = when (slot) {
                in 0..5 -> Triple(-110.0, 60.0, 50.0)
                in 6..14 -> Triple(-17.58, 65.78, 15.52)
                in 15..24, 30 -> Triple(9.42, 68.78, -93.48)
                else -> Triple(-118.58, 78.78, -105.48)
            }
            List(3) { FloorDisplay(100 + slot * 3 + it, x, y, z, "minecraft:string:String") }
        })
        assertEquals(5, result.count { it.type == FloorDropType.COIN })
        assertEquals(5, result.count { it.type == FloorDropType.ICEBREAKER })
        assertEquals(FloorDropType.GENERIC, result.last().type)
    }

    @Test fun `rejected overlapping triple does not consume a valid gem marker`() {
        val predictor = FloorDropPredictor()
        val stray = FloorDisplay(9,-110.0,60.0,50.0,"minecraft:string:Loading")
        val result = predictor.scan(listOf(stray)+triple(10,"minecraft:string:String"))
        assertEquals(10,result.single().id)
        assertEquals(FloorDropType.LIME,result.single().type)
    }
}
