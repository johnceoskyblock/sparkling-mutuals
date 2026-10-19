package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartingResourceProtocolTest {
    private val scope = "1234567890abcdef1234567890abcdef"
    @Test fun `absolute protocol preserves quantities canonical ordering and explicit none`() {
        val mixed = StartingItemReport(mapOf(FloorDropType.BERRY to 1, FloorDropType.COIN to 1), scope)
        assertEquals("[SM] Starting items: 1x Shining Coin; 1x Yogi Berry [run:$scope]", mixed.text)
        assertEquals(mixed, StartingItemReport.parse(mixed.text))
        assertEquals(2, StartingItemReport.parse("[SM] Starting items: 2x Bag of Seeds")!!.quantities[FloorDropType.SEEDS])
        assertTrue(StartingItemReport.parse("[SM] Starting items: none")!!.quantities.isEmpty())
        for (bad in listOf("", "0x Shining Coin", "65x Shining Coin", "1x Icebreaker", "1x Shining Coin; 2x Shining Coin", "none; 1x Shining Coin"))
            assertNull(StartingItemReport.parse("[SM] Starting items: $bad"), bad)
    }
    @Test fun `missing report is not zero and duplicate legacy self reports overlap`() {
        val ledger = StartingResourceLedger()
        assertFalse(ledger.complete)
        ledger.roster(setOf("self", "friend"))
        ledger.report("self", mapOf(FloorDropType.SEEDS to 2), complete = true)
        assertFalse(ledger.complete)
        ledger.report("friend", emptyMap(), complete = true)
        assertTrue(ledger.complete)
        val revision = ledger.revision(SafariBiome.FOREST)
        ledger.report("SELF", mapOf(FloorDropType.SEEDS to 2), complete = true)
        ledger.report("self", mapOf(FloorDropType.SEEDS to 1), complete = false)
        assertEquals(2, ledger.total(FloorDropType.SEEDS))
        assertEquals(revision, ledger.revision(SafariBiome.FOREST))
        ledger.report("friend", mapOf(FloorDropType.BERRY to 1), complete = false)
        assertEquals(3, ledger.foodTotal)
        assertTrue(ledger.revision(SafariBiome.FOREST) > revision)
        ledger.roster(setOf("self", "friend", "late"))
        assertFalse(ledger.complete)
        ledger.roster(null)
        assertFalse(ledger.complete)
    }
    @Test fun `only exact hotspot starts once at plus two seconds with live owner and scope`() {
        val announcement = StartingResourceAnnouncement()
        assertFalse(announcement.trigger("Player entered Critter Safari!", 0))
        assertFalse(announcement.trigger("Party > Friend: HOTSPOT! Your Hunting Hotspot is the Forest Biome!", 0))
        assertTrue(announcement.trigger("HOTSPOT! Your Hunting Hotspot is the Forest Biome!", 5000))
        assertFalse(announcement.trigger("HOTSPOT! Your Hunting Hotspot is the Icy Biome!", 6000))
        var count = 0
        val report = StartingItemReport(mapOf(FloorDropType.SEEDS to 2), scope)
        assertFalse(announcement.dispatch(6999, true, report) { count++; true })
        assertFalse(announcement.dispatch(7000, false, report) { count++; true })
        assertFalse(announcement.dispatch(7000, true, report.copy(scope = null)) { count++; true })
        assertTrue(announcement.dispatch(7000, true, report) { count++; true })
        assertFalse(announcement.dispatch(9000, true, report) { count++; true })
        assertEquals(1, count)
    }
    @Test fun `failed early send stays owned and cannot dispatch after cancellation`() {
        val announcement = StartingResourceAnnouncement()
        announcement.trigger("HOTSPOT! Your Hunting Hotspot is the Haunted Biome!", 0)
        val report = StartingItemReport(emptyMap(), scope)
        assertFalse(announcement.dispatch(2000, true, report) { false })
        announcement.cancel()
        assertFalse(announcement.dispatch(3000, true, report) { error("stale run") })
    }
}
