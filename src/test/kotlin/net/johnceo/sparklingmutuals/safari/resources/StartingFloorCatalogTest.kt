package net.johnceo.sparklingmutuals.safari.resources

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartingFloorCatalogTest {
    private fun layout(coins: Int = 4): List<PredictedFloorDrop> {
        val types = listOf(SafariBiome.CAVERN to listOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE).let { it + it },
            SafariBiome.FOREST to List(3) { FloorDropType.WORM } + List(3) { FloorDropType.BERRY } + List(3) { FloorDropType.SEEDS },
            SafariBiome.HAUNTED to List(5) { FloorDropType.INCENSE } + List(coins) { FloorDropType.COIN },
            SafariBiome.ICY to List(5) { FloorDropType.ICEBREAKER })
        return types.flatMap { (biome, sequence) -> sequence.map { biome to it } }.mapIndexed { i, (biome, type) ->
            PredictedFloorDrop(100 + i * 3, 0.0, 60.0, 0.0, biome, type)
        }
    }
    private fun active(groups: List<PredictedFloorDrop>) = groups.flatMap { drop ->
        (0..2).map { drop.id + it to UUID(0, (drop.id + it).toLong()) }
    }.toMap()
    @Test fun `complete observed phase catalog derives variable coin count and retains scope through unload`() {
        val catalog = StartingFloorCatalog()
        val groups = layout(4)
        catalog.observe(groups, active(groups))
        assertTrue(catalog.complete)
        assertEquals(4, catalog.coinIds.size)
        assertEquals(9, catalog.foodIds.size)
        val scope = catalog.scope!!
        assertTrue(catalog.acceptsScope(scope))
        catalog.observe(groups, emptyMap())
        assertTrue(catalog.complete)
        assertEquals(4, catalog.coinIds.size)
        assertTrue(catalog.acceptsScope(scope))
        assertFalse(StartingFloorCatalog().acceptsScope(scope), "old-run display UUID is not a current context")
    }
    @Test fun `partial unknown or missing prefix never closes initial coverage`() {
        val groups = layout()
        val catalog = StartingFloorCatalog()
        catalog.observe(groups.drop(1), active(groups.drop(1)))
        assertFalse(catalog.complete, "cannot anchor to a later first visible group")
        catalog.observe(groups, active(groups).minus(groups.first().id + 1))
        assertFalse(catalog.complete, "a decorative or partial triple is not a verified slot")
        catalog.observe(groups, active(groups))
        assertTrue(catalog.complete)
        catalog.observe(groups, active(groups) + (9999 to UUID.randomUUID()))
        assertFalse(catalog.complete, "a newly observed unclassified reward marker reopens coverage")
    }
    @Test fun `unknown and generic decorative triples cannot register coin opportunities`() {
        val catalog = StartingFloorCatalog()
        val group = PredictedFloorDrop(1, 0.0, 0.0, 0.0, SafariBiome.HAUNTED, FloorDropType.COIN)
        catalog.observe(listOf(group), emptyMap())
        assertTrue(catalog.coinIds.isEmpty())
        catalog.observe(listOf(group), active(listOf(group)))
        assertTrue(catalog.coinIds.isEmpty(), "typed triple without complete phase provenance is still provisional")
        assertFalse(catalog.complete)
    }
    @Test fun `missing last Haunted coin cannot hide in the phase boundary gap`() {
        val groups = layout(4)
        val missing = groups.last { it.biome == SafariBiome.HAUNTED }
        val partial = groups.filterNot { it.id == missing.id }
        val catalog = StartingFloorCatalog()
        catalog.observe(partial, active(partial))
        assertFalse(catalog.complete)
    }
    @Test fun `new actual coin slot raises demand without relabelling generic rewards as coins`() {
        val groups = layout(4)
        val catalog = StartingFloorCatalog()
        catalog.observe(groups, active(groups))
        val extra = PredictedFloorDrop(1000, 0.0, 60.0, -50.0, SafariBiome.HAUNTED, FloorDropType.GENERIC,
            actual = FloorPickup(FloorDropType.COIN, 1))
        catalog.observe(groups + extra, active(groups + extra))
        assertEquals(5, catalog.coinIds.size)
    }
}
