package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariBirdLedgerTest {
    @Test fun `early visitors and party reports never add local food demand`() {
        val run=net.johnceo.sparklingmutuals.safari.SafariRun(1000)
        run.localPlayer("Local")
        run.observeEarlyBirdVisitor("Friend",6.0,51.0,2000)
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 2x Bag of Seeds")
        run.birdFoodLeftToUse
        assertEquals(9,run.birds.feedingTarget)
    }
    @Test fun `bird food scan covers forty horizontal blocks`() {
        val zone = net.johnceo.sparklingmutuals.safari.completion.ForestCompletionZones
        assertTrue(zone.inBirdScanRadius(46.0, 51.0))
        assertFalse(zone.inBirdScanRadius(46.01, 51.0))
        assertTrue(zone.nearBirdScanRadius(66.0, 51.0))
        assertFalse(zone.nearBirdScanRadius(66.01, 51.0))
    }
    private fun ready() = SafariBirdLedger().apply { inventory(emptyList()); floor(0) }
    @Test fun `server spawn lines record food uses and Macaw pairs without granting captures`() {
        val birds=ready()
        repeat(9) { assertTrue(birds.spawn("Two Macaws were attracted to the Birdfeeder!")) }
        assertEquals(18,birds.spawnedCount("Macaw"))
        assertTrue(birds.feedingResolved)
        assertFalse(birds.capturesComplete { 0 })
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
    @Test fun `party notices do not invalidate a retained source check`() {
        val birds=ready();birds.scan(0,9);birds.report()
        birds.contributions("me",mapOf(("friend" to FloorDropType.BERRY) to 2))
        assertEquals(9,birds.feedingTarget);assertTrue(birds.remoteReady)
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
        assertEquals(11, birds.feedingTarget)
    }
    @Test fun `all foods alert still needs three of each local floor pickup and fires once`() {
        val birds = ready()
        repeat(3) { for (food in listOf("Yogi Berry", "Wriggleworm", "Bag of Seeds")) birds.pickup("FLOOR DROP! $food", SafariBiome.FOREST) }
        assertFalse(birds.alert(SafariBiome.FOREST, false))
        assertTrue(birds.alert(SafariBiome.FOREST, true))
        assertFalse(birds.alert(SafariBiome.FOREST, true))
    }
}
