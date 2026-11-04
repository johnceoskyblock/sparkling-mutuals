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

    @Test fun `rejected overlapping triple does not consume a valid gem marker`() {
        val predictor = FloorDropPredictor()
        val stray = FloorDisplay(9,-110.0,60.0,50.0,"minecraft:string:Loading")
        val result = predictor.scan(listOf(stray)+triple(10,"minecraft:string:String"))
        assertEquals(10,result.single().id)
        assertEquals(FloorDropType.LIME,result.single().type)
    }
}
