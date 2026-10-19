package net.johnceo.sparklingmutuals.safari.resources

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EarlyStartingResourcesTest {
    @Test fun `showing a local title does not create a party starting contribution`() {
        val resources = SafariResources()
        resources.inventory(listOf("Shining Coin" to 1), true)
        resources.noticeSent("Local", "[SM] Use: 1x Shining Coin")
        assertTrue(resources.coinContributions().isEmpty(), "a local title is presentation, not a received report")
    }

    @Test fun `early absolute mixed report is ingested before any biome clear`() {
        val resources = SafariResources()
        assertTrue(resources.partyNotice("Party > Friend: [SM] Starting items: 1x Shining Coin; 1x Yogi Berry"))
        assertEquals(1, resources.coinContributions()["friend"])
        assertEquals(1, resources.foodContributions()["friend" to FloorDropType.BERRY])
        assertTrue(resources.partyNotice("Party > Friend: [SM] Starting items: 1x Shining Coin; 1x Yogi Berry"))
        assertTrue(resources.partyNotice("Party > Friend: [SM] Use: 1x Shining Coin"))
        assertEquals(1, resources.coinContributions()["friend"])
    }

    @Test fun `used original duplicate grants survive a later floor pickup`() {
        val resources = SafariResources()
        resources.headStart("HEAD START! You started with a Bag of Seeds!")
        resources.inventory(listOf("Bag of Seeds" to 2), true)
        resources.inventory(emptyList(), true)
        resources.pickup("FLOOR DROP! You found 1x Bag of Seeds on the ground!")
        resources.inventory(listOf("Bag of Seeds" to 1), true)
        assertEquals(2, resources.startingCount(FloorDropType.SEEDS))
    }
}
