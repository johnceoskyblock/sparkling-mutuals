package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariFloorDropsTest {
    @Test fun `cavern hides floor drops after all three distinct gems were found in either mode`() {
        val state = FloorDropState()
        state.visit(SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.CAVERN, false, true))
        state.record("FLOOR DROP! Purple Gem", SafariBiome.CAVERN)
        state.record("FLOOR DROP! Purple Gem", SafariBiome.CAVERN)
        state.record("FLOOR DROP! Lime Gem", SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.CAVERN, true, true))
        state.record("FLOOR DROP! Orange Gem", SafariBiome.CAVERN)
        assertFalse(state.enabled(SafariBiome.CAVERN, true, true))
        assertFalse(state.enabled(SafariBiome.CAVERN, false, true))
        state.visit(null); state.visit(SafariBiome.CAVERN)
        assertFalse(state.enabled(SafariBiome.CAVERN, true, true))
        state.reset()
        assertTrue(state.enabled(SafariBiome.CAVERN, true, true))
    }
    @Test fun `icy unique runs hide drops immediately and full clears hide them after two pickaxes`() {
        val state = FloorDropState()
        state.visit(SafariBiome.ICY)
        assertFalse(state.enabled(SafariBiome.ICY, false, true))
        assertTrue(state.enabled(SafariBiome.ICY, true, true))
        state.record("FLOOR DROP! Icebreaker", SafariBiome.ICY)
        assertTrue(state.enabled(SafariBiome.ICY, true, true))
        state.record("FLOOR DROP! Icebreaker", SafariBiome.ICY)
        assertFalse(state.enabled(SafariBiome.ICY, true, true))
        assertFalse(state.enabled(SafariBiome.ICY, false, true))
    }
    @Test fun `forced choices last for the biome visit and then restore automatic rules`() {
        val state = FloorDropState()
        state.visit(SafariBiome.ICY)
        state.force(true)
        assertTrue(state.enabled(SafariBiome.ICY, false, false))
        state.visit(SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.CAVERN, true, false))
        state.force(false)
        assertFalse(state.enabled(SafariBiome.CAVERN, true, true))
        state.visit(null)
        state.visit(SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.CAVERN, true, false))
        state.inventory(listOf("§aPurple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 1))
        assertFalse(state.enabled(SafariBiome.CAVERN, true, true))
        state.force(true)
        assertTrue(state.enabled(SafariBiome.CAVERN, true, true))
        state.visit(SafariBiome.FOREST); state.visit(SafariBiome.CAVERN)
        assertFalse(state.enabled(SafariBiome.CAVERN, true, true))
    }
    @Test fun `forest and haunted remain under normal config control regardless of mode`() {
        val state = FloorDropState()
        for (biome in listOf(SafariBiome.FOREST, SafariBiome.HAUNTED)) for (fullClear in listOf(false, true)) {
            state.visit(biome)
            assertTrue(state.enabled(biome, fullClear, true))
            assertFalse(state.enabled(biome, fullClear, false))
        }
    }
    @Test fun `quoted chat and unrelated pickups do not satisfy floor drop requirements`() {
        val state = FloorDropState()
        state.visit(SafariBiome.ICY)
        state.record("Party > Friend: FLOOR DROP! Icebreaker", SafariBiome.ICY)
        state.record("You bought an Icebreaker", SafariBiome.ICY)
        state.record("FLOOR DROP! Icebreaker", SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.ICY, true, true))
        state.visit(SafariBiome.CAVERN)
        state.record("Party > Friend: FLOOR DROP! Purple Gem", SafariBiome.CAVERN)
        state.record("FLOOR DROP! Lime Gem", SafariBiome.CAVERN)
        state.record("FLOOR DROP! Orange Gem", SafariBiome.CAVERN)
        assertTrue(state.enabled(SafariBiome.CAVERN, true, true))
    }
    @Test fun `inventory and pickup message cannot double count one Icebreaker`() {
        val state = FloorDropState()
        state.visit(SafariBiome.ICY)
        state.record("FLOOR DROP! Icebreaker", SafariBiome.ICY)
        state.inventory(listOf("§ficebreaker" to 1, "Diamond Pickaxe" to 2))
        assertTrue(state.enabled(SafariBiome.ICY, true, true))
        state.inventory(listOf("§fIcebreaker" to 1, "Icebreaker" to 1))
        assertFalse(state.enabled(SafariBiome.ICY, true, true))
        state.inventory(emptyList())
        state.visit(null); state.visit(SafariBiome.ICY)
        assertFalse(state.enabled(SafariBiome.ICY, true, true))
        state.reset()
        assertTrue(state.enabled(SafariBiome.ICY, true, true))
    }
}
