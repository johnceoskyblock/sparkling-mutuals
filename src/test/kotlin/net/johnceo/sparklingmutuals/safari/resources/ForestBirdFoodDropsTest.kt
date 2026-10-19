package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.esp.EspDrop
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForestBirdFoodDropsTest {
    private fun group(id: Int, type: FloorDropType?) =
        PredictedFloorDrop(id, 0.42, 50.78, 40.52, SafariBiome.FOREST, type)

    @Test fun `food and unknown markers block while complete non-food groups do not`() {
        val generic = group(100, FloorDropType.GENERIC)
        val worm = group(200, FloorDropType.WORM)
        val unknown = group(300, null)
        val raw = listOf(generic.marker(), worm.marker(), unknown.marker(), EspDrop(400, 0, 50, 40))
        assertEquals(3, ForestBirdFoodDrops.remaining(raw, listOf(generic, worm, unknown)))
        assertEquals(4, ForestBirdFoodDrops.remaining(raw, emptyList()), "incomplete triples cannot establish non-food")
        generic.actual = FloorPickup(FloorDropType.SEEDS, 1)
        assertEquals(4, ForestBirdFoodDrops.remaining(raw, listOf(generic, worm, unknown)), "actual food outranks prediction")
        assertEquals(1, ForestBirdFoodDrops.remaining(listOf(EspDrop(201, 1, 51, 41)), listOf(worm)), "IDs bind boundary-straddling members")
    }

    @Test fun `logged nine pickups and ten captures complete all modes despite eight generic rewards`() {
        val predictor = FloorDropPredictor()
        fun displays(slots: IntRange) = slots.flatMap { slot ->
            (0..2).map { member -> FloorDisplay(2262 + slot * 3 + member, 0.42, 50.78, 30.52 + slot, "minecraft:string") }
        }
        predictor.scan(displays(0..16), 0)
        val remaining = predictor.scan(displays(9..16), 10000)
        assertEquals(8, remaining.size)
        assertTrue(remaining.all { it.type == FloorDropType.GENERIC })
        val markers = remaining.map { it.marker() }
        assertEquals(0, ForestBirdFoodDrops.remaining(markers, remaining))
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(listOf("Shining Coin" to 1), 0)
        val state = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
        }
        repeat(3) { for (food in listOf("Wriggleworm", "Yogi Berry", "Bag of Seeds")) {
            run.recordBirdFood("FLOOR DROP! You found $food on the ground!", SafariBiome.FOREST, 10000)
        } }
        repeat(7) { run.record(SafariCatch(SafariRoster.named("Bluebird")!!)) }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Parakeet")!!)) }
        run.inventoryEvidence(listOf("Shining Coin" to 1), 20000)
        run.scanBirdSources(ForestBirdFoodDrops.remaining(markers, remaining))
        val birds = listOf("Bluebird", "Parakeet", "Macaw").map { SafariRoster.named(it)!! }
        for (mode in SafariMode.entries) assertEquals(3, run.completed(mode, birds, state), mode.name)
        // A remaining real food still blocks every mode, even after enough captures.
        val food = group(999, FloorDropType.BERRY)
        run.scanBirdSources(ForestBirdFoodDrops.remaining(markers + food.marker(), remaining + food))
        for (mode in SafariMode.entries) assertEquals(0, run.completed(mode, birds, state), mode.name)
    }
}
