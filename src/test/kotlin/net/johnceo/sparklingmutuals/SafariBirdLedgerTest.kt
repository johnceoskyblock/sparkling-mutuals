package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariBirdLedgerTest {
    private fun foods(birds: SafariBirdLedger) {
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry"))
            birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
    }
    private fun feed(birds: SafariBirdLedger, normal: Int, pairs: Int) {
        repeat(normal) { assertTrue(birds.spawn("A Bluebird was attracted to the Birdfeeder!")) }
        repeat(pairs) { assertTrue(birds.spawn("Two Macaws were attracted to the Birdfeeder!")) }
        birds.inventory(emptyList())
    }
    @Test fun `starting food adds to run demand without replacing Forest pickups`() {
        val birds = SafariBirdLedger()
        birds.pickup("FLOOR DROP! Bag of Seeds", null)
        birds.pickup("FLOOR DROP! Bag of Seeds", null)
        repeat(2) { birds.pickup("FLOOR DROP! Bag of Seeds", SafariBiome.FOREST) }
        repeat(3) { birds.pickup("FLOOR DROP! Wriggleworm", SafariBiome.FOREST); birds.pickup("FLOOR DROP! Yogi Berry", SafariBiome.FOREST) }
        assertFalse(birds.foodsComplete)
        birds.pickup("FLOOR DROP! Bag of Seeds", SafariBiome.FOREST)
        assertTrue(birds.foodsComplete)
        assertEquals(11, birds.collected)
        feed(birds, 9, 2)
        assertEquals(10, birds.requiredCaptures(1, 3))
        assertFalse(birds.sufficient(8, 1, 3))
        assertTrue(birds.sufficient(10, 1, 3))
    }
    @Test fun `nine foods two Macaw pairs and three remaining Macaws require eight catches`() {
        val birds = SafariBirdLedger(); foods(birds); feed(birds, 7, 2)
        assertEquals(9, birds.used)
        assertEquals(8, birds.requiredCaptures(1, 3))
        assertFalse(birds.sufficient(7, 1, 3))
        assertTrue(birds.sufficient(8, 1, 3))
    }
    @Test fun `four Macaw pairs allow six personal catches instead of the old seven minimum`() {
        val birds = SafariBirdLedger(); foods(birds); feed(birds, 5, 4)
        assertEquals(6, birds.requiredCaptures(1, 7))
        assertTrue(birds.sufficient(6, 1, 7))
        assertFalse(birds.sufficient(5, 1, 7))
        assertTrue(birds.hasMacaws(0, 8))
    }
    @Test fun `feeding during collection works without holding all nine food at once`() {
        val birds = SafariBirdLedger()
        repeat(3) { for (food in listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")) {
            birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST)
            birds.inventory(listOf(food to 1))
            birds.spawn("A Parakeet was attracted to the Birdfeeder!")
            birds.inventory(emptyList())
        } }
        assertTrue(birds.foodsComplete)
        assertEquals(9, birds.used)
        assertTrue(birds.sufficient(9, 0, 0))
        assertFalse(birds.sufficient(8, 0, 0))
    }
    @Test fun `missing inventory removal or feeder announcement cannot imply food was used`() {
        val birds = SafariBirdLedger(); foods(birds)
        birds.inventory(emptyList())
        assertEquals(0, birds.used)
        assertFalse(birds.sufficient(9, 0, 0))
        repeat(9) { birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertEquals(9, birds.used)
        val stillHeld = SafariBirdLedger(); foods(stillHeld)
        repeat(9) { stillHeld.spawn("A Bluebird was attracted to the Birdfeeder!") }
        stillHeld.inventory(listOf("Yogi Berry" to 3, "Wriggleworm" to 3, "Bag of Seeds" to 3))
        assertEquals(0, stillHeld.used)
        assertFalse(stillHeld.sufficient(9, 0, 0))
    }
    @Test fun `packet order does not double count food use and held extra food does not erase usage`() {
        val birds = SafariBirdLedger(); foods(birds)
        birds.inventory(emptyList())
        feed(birds, 9, 0)
        repeat(3) { birds.inventory(listOf("§aBag of Seeds" to 1)) }
        assertEquals(9, birds.used)
        assertTrue(birds.sufficient(9, 0, 0))
    }
    @Test fun `only exact server bird messages and food pickups contribute`() {
        val birds = SafariBirdLedger()
        assertFalse(birds.pickup("Party > Friend: FLOOR DROP! Bag of Seeds", SafariBiome.FOREST))
        assertFalse(birds.pickup("FLOOR DROP! Bag of Seeds Shard", SafariBiome.FOREST))
        assertFalse(birds.spawn("Party > Friend: Two Macaws were attracted to the Birdfeeder!"))
        assertFalse(birds.spawn("A Rockmite was attracted to the Birdfeeder!"))
        assertEquals(0, birds.collected)
        assertTrue(birds.spawn("§6Two Macaws were attracted to the Birdfeeder!"))
        assertFalse(birds.sufficient(99, 1, 1))
        assertFalse(SafariBirdLedger().foodsComplete)
    }
    @Test fun `food alert uses Forest pickups even if food was already fed and fires once per run`() {
        val birds = SafariBirdLedger(); foods(birds); feed(birds, 9, 0)
        assertFalse(birds.alert(SafariBiome.FOREST, false))
        assertFalse(birds.alert(SafariBiome.CAVERN, true))
        assertTrue(birds.alert(SafariBiome.FOREST, true))
        assertFalse(birds.alert(null, true))
        assertFalse(birds.alert(SafariBiome.FOREST, true))
        assertFalse(birds.alert(SafariBiome.FOREST, false))
        assertFalse(birds.alert(SafariBiome.FOREST, true))
        assertFalse(SafariBirdLedger().alert(SafariBiome.FOREST, true))
    }
}
