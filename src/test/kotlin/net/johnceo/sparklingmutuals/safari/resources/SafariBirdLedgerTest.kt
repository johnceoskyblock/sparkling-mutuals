package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariBirdLedgerTest {
    @Test fun `early unreported teammate adds one food and a real starting report replaces the estimate`() {
        val run = net.johnceo.sparklingmutuals.safari.SafariRun(1000)
        run.localPlayer("Local")
        run.observeEarlyBirdVisitor("Local", 6.0, 51.0, 2000)
        run.observeEarlyBirdVisitor("FarAway", 66.01, 51.0, 2000)
        run.observeEarlyBirdVisitor("Late", 6.0, 51.0, 31001)
        assertEquals(9, run.birds.feedingTarget)
        repeat(3) { run.observeEarlyBirdVisitor("Friend", 66.0, 51.0, 2000) }
        assertEquals(10, run.birds.feedingTarget)
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 2x Bag of Seeds")
        run.birdFoodLeftToUse // Reconcile retained party reports with inferred food.
        assertEquals(11, run.birds.feedingTarget)
        run.observeEarlyBirdVisitor("Friend", 6.0, 51.0, 3000)
        assertEquals(11, run.birds.feedingTarget)
        run.resources.partyNotice("Party > Reporter: [SM] Starting items: 1x Shining Coin")
        run.observeEarlyBirdVisitor("Reporter", 6.0, 51.0, 3000)
        assertEquals(11, run.birds.feedingTarget)
    }
    @Test fun `bird food scan covers forty horizontal blocks`() {
        val zone = net.johnceo.sparklingmutuals.safari.completion.ForestCompletionZones
        assertTrue(zone.inBirdScanRadius(46.0, 51.0))
        assertFalse(zone.inBirdScanRadius(46.01, 51.0))
        assertTrue(zone.nearBirdScanRadius(66.0, 51.0))
        assertFalse(zone.nearBirdScanRadius(66.01, 51.0))
    }
    private fun ready() = SafariBirdLedger().apply { inventory(emptyList()); floor(0) }
    @Test fun `spawn chat never establishes population or Macaw pairs`() {
        val birds = ready()
        repeat(20) {
            assertFalse(birds.spawn("A Bluebird was attracted to the Birdfeeder!"))
            assertFalse(birds.spawn("Two Macaws were attracted to the Birdfeeder!"))
        }
        assertEquals(9, birds.feedingTarget)
        assertEquals(0, birds.spawnedCount("Macaw"))
        assertFalse(birds.feedingResolved)
    }
    @Test fun `current render and shared captures account for nine food opportunities`() {
        val birds = ready()
        birds.render({ if (it == "Bluebird") 5 else 0 }, listOf("Bluebird", "Parakeet", "Macaw", "Macaw"))
        assertEquals(8, birds.accountedFeedings); assertFalse(birds.feedingResolved)
        birds.render({ if (it == "Bluebird") 5 else 0 }, listOf("Bluebird", "Parakeet", "Parakeet", "Macaw", "Macaw"))
        assertTrue(birds.feedingResolved)
        birds.render({ 0 }, emptyList())
        assertTrue(birds.feedingResolved, "a completed qualified population check survives unloading")
    }
    @Test fun `food still held or on the floor blocks local and remote proof`() {
        val birds = ready(); birds.scan(0, 9); birds.report()
        assertTrue(birds.feedingResolved); assertTrue(birds.remoteReady)
        birds.floor(1)
        assertFalse(birds.feedingResolved); assertFalse(birds.remoteReady)
        birds.floor(-1)
        assertFalse(birds.remoteReady, "an unknown scan cannot erase a known remaining food drop")
        birds.reenter(); assertFalse(birds.remoteReady, "travel must not forget remaining floor food")
        birds.floor(0); birds.inventory(listOf("Yogi Berry" to 1))
        assertFalse(birds.remoteReady)
        birds.inventory(emptyList()); assertTrue(birds.remoteReady)
        birds.floor(-1); assertFalse(birds.feedingResolved, "unknown markers are not an empty floor")
    }
    @Test fun `party notices add quantities once and invalidate older reports`() {
        val birds = ready(); birds.report()
        val notices = mapOf(("friend" to FloorDropType.BERRY) to 2, ("friend" to FloorDropType.SEEDS) to 1)
        repeat(5) { birds.contributions("me", notices) }
        assertEquals(12, birds.feedingTarget); assertFalse(birds.remoteReady)
        birds.scan(0, 11); assertFalse(birds.feedingResolved)
        birds.scan(0, 12); assertTrue(birds.feedingResolved)
        birds.report(); assertTrue(birds.remoteReady)
    }
    @Test fun `starting inventory overlaps grants but adds to the nine floor foods`() {
        val birds = ready(); birds.startingGrant("Yogi Berry")
        birds.startingInventory(listOf("Yogi Berry" to 1), 0)
        repeat(3) { for (food in listOf("Yogi Berry", "Wriggleworm", "Bag of Seeds")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
        assertEquals(10, birds.feedingTarget); assertTrue(birds.foodsComplete)
        birds.scan(0, 9); assertFalse(birds.feedingResolved)
        birds.scan(0, 10); assertTrue(birds.feedingResolved)
    }
    @Test fun `Full Clear uses observed species and one Macaw rather than all remaining Macaws`() {
        val birds = ready()
        birds.render({ 0 }, List(5) { "Bluebird" } + List(8) { "Macaw" })
        assertTrue(birds.feedingResolved)
        assertFalse(birds.capturesComplete { if (it == "Bluebird") 5 else 0 })
        assertTrue(birds.capturesComplete { if (it == "Bluebird") 5 else if (it == "Macaw") 1 else 0 })
        assertFalse(birds.capturesComplete { if (it == "Bluebird") 4 else if (it == "Macaw") 8 else 0 })
    }
    @Test fun `source readiness needs empty food observations rather than personal pickup history`() {
        val birds = ready()
        assertTrue(birds.reportSuppliesReady)
        birds.inventory(listOf("Bag of Seeds" to 1)); assertFalse(birds.reportSuppliesReady)
        birds.inventory(emptyList()); birds.floor(1)
        assertFalse(birds.reportSuppliesReady)
        assertFalse(SafariBirdLedger().reportSuppliesReady, "unknown source evidence is not empty")
    }
    @Test fun `self notice and initial inventory overlap by item type rather than counting twice`() {
        val birds = ready()
        birds.startingInventory(listOf("Yogi Berry" to 2), 0)
        birds.contributions("me", mapOf(("ME" to FloorDropType.BERRY) to 2, ("other" to FloorDropType.SEEDS) to 1))
        assertEquals(12, birds.feedingTarget)
    }
    @Test fun `all foods alert still needs three of each local floor pickup and fires once`() {
        val birds = ready()
        repeat(3) { for (food in listOf("Yogi Berry", "Wriggleworm", "Bag of Seeds")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
        assertFalse(birds.alert(SafariBiome.FOREST, false))
        assertTrue(birds.alert(SafariBiome.FOREST, true))
        assertFalse(birds.alert(SafariBiome.FOREST, true))
    }
}
