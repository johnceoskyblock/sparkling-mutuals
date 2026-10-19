package net.johnceo.sparklingmutuals.safari.resources

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import net.johnceo.sparklingmutuals.safari.SafariRun

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
    @Test fun `first resource interaction freezes original quantities before later inventory and pickups`() {
        val run = SafariRun(0)
        run.inventoryEvidence(emptyList(), 0)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 10000)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 10000)
        run.closeStartingInventory()
        run.inventoryEvidence(emptyList(), 10001)
        run.resources.pickup("FLOOR DROP! You found 1x Bag of Seeds on the ground!")
        run.inventoryEvidence(listOf("Bag of Seeds" to 1, "Yogi Berry" to 1), 11000)
        val report = run.startingItems("1234567890abcdef1234567890abcdef")!!
        assertEquals(mapOf(FloorDropType.SEEDS to 2), report.quantities)
        assertFalse(run.inStartingInventoryWindow(11000))
    }
    @Test fun `late first snapshot after departure cannot fabricate an empty or complete report`() {
        val run = SafariRun(0)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 10000)
        run.inventoryEvidence(listOf("Shining Coin" to 1), 12001)
        assertNull(run.startingItems("1234567890abcdef1234567890abcdef"))
    }
    @Test fun `silent grant update immediately after HOTSPOT is captured before use`() {
        val run = SafariRun(0)
        run.inventoryEvidence(emptyList(), 0)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 5001)
        run.inventoryEvidence(emptyList(), 5002)
        assertEquals(mapOf(FloorDropType.SEEDS to 2), run.startingItems("1234567890abcdef1234567890abcdef")!!.quantities)
    }
    @Test fun `an oversized initial quest inventory is uncertain rather than a set of random grants`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Bag of Seeds" to 20), 0)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
        assertNull(run.startingItems("1234567890abcdef1234567890abcdef"))
    }
    @Test fun `an unrelated boat interaction cannot close the not yet departed grant window`() {
        val run = SafariRun(0)
        run.inventoryEvidence(emptyList(), 0)
        run.closeStartingInventory()
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 5001)
        assertEquals(2, run.startingItems("1234567890abcdef1234567890abcdef")!!.quantities[FloorDropType.SEEDS])
    }
    @Test fun `a known First Class bonus cannot become none when its grant was not witnessed`() {
        val run = SafariRun(0)
        run.entry.ticket = net.johnceo.sparklingmutuals.safari.SafariTicketType.FIRST_CLASS
        run.inventoryEvidence(emptyList(), 0)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
        assertNull(run.startingItems("1234567890abcdef1234567890abcdef"))
    }
    @Test fun `two witnessed non applicable grants produce explicit none`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Purple Gem" to 1, "Soothing Incense" to 1), 0)
        run.startingMessage("HOTSPOT! Your Hunting Hotspot is the Cavern Biome!", 5000)
        assertTrue(run.startingItems("1234567890abcdef1234567890abcdef")!!.text.contains("Starting items: none"))
    }
}
