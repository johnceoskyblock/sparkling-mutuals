package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IncenseDropSelectionTest {
    private fun drop(id: Int, x: Double, y: Double = 47.0, type: FloorDropType = FloorDropType.INCENSE) =
        PredictedFloorDrop(id, x, y, -27.0, SafariBiome.HAUNTED, type)
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
    }
    @Test fun `rank four incense by three dimensional waypoint distance with stable tie break`() {
        val state = FloorDropState()
        val known = listOf(drop(12, 4.0, 70.0), drop(9, 6.0), drop(3, 2.0), drop(6, 5.0), drop(15, 8.0), drop(18, 4.0, type = FloorDropType.COIN))
        assertEquals(listOf(6, 3, 9, 15), state.incenseDropIds(known).toList())
        known.first { it.id == 6 }.also { it.live = false; it.actual = FloorPickup(FloorDropType.INCENSE, 1) }
        assertEquals(setOf(6, 3, 9, 15), state.incenseDropIds(known))
        assertFalse(12 in state.incenseDropIds(known))
    }
    @Test fun `starting incense reduces nearest slots one for one in every mode and consumption never expands selection`() {
        val known = (1..5).map { drop(it * 3, 4.0 + it) }
        for (starting in 0..2) for (mode in SafariMode.entries) {
            val state = FloorDropState()
            if (starting > 0) state.startingGrant(FloorDropType.INCENSE)
            state.inventory(listOf("Soothing Incense" to starting), initial = true)
            assertEquals(when (starting) { 0 -> setOf(3, 6, 9, 12); 1 -> setOf(3, 6, 9); else -> setOf(3, 6) }, state.incenseDropIds(known))
            state.inventory(emptyList(), initial = false)
            state.visit(SafariBiome.FOREST); state.visit(SafariBiome.HAUNTED)
            assertEquals(when (starting) { 0 -> setOf(3, 6, 9, 12); 1 -> setOf(3, 6, 9); else -> setOf(3, 6) }, state.incenseDropIds(known))
            assertTrue(state.needed(FloorDropType.INCENSE, mode, party()))
            state.inventory(listOf("Soothing Incense" to 4), initial = false)
            state.inventory(emptyList(), initial = false)
            assertFalse(state.needed(FloorDropType.INCENSE, mode, party()))
            state.reset()
            assertEquals(setOf(3, 6, 9, 12), state.incenseDropIds(known))
        }
    }
    @Test fun `later floor pickups do not become starting items or reduce the four slot selection`() {
        val state = FloorDropState()
        val known = (1..5).map { drop(it * 3, 4.0 + it) }
        state.inventory(emptyList(), initial = true)
        state.record("FLOOR DROP! You found Soothing Incense!", SafariBiome.HAUNTED)
        state.inventory(listOf("Soothing Incense" to 1), initial = false)
        assertEquals(setOf(3, 6, 9, 12), state.incenseDropIds(known))
    }
}
