package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GimmiegoldCoinFloorCheckTest {
    private fun live(n: Int) = List(n) { UUID.randomUUID() }
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - "Gimmiegold"))
    }
    @Test fun `initial floor slots persist through unloading and require every pickup`() {
        val check = GimmiegoldSparklingCheck()
        (1..5).forEach { check.floor(it, true) }
        check.inventory(0)
        check.scan(1, live(4), true)
        assertFalse(check.complete)
        (1..4).forEach { check.floor(it, false) }
        check.floor(5, null)
        check.scan(1, live(4), true)
        assertEquals(5, check.floorMinimum)
        assertEquals(1, check.remaining)
        assertFalse(check.complete, "an unloaded last floor drop is not collected")
        check.floor(5, false)
        check.scan(1, live(4), true)
        assertTrue(check.complete)
        check.scan(1, emptyList(), false)
        assertTrue(check.complete, "retain a qualified completed check through travel")
        check.floor(6, true)
        assertFalse(check.complete, "a newly known source reopens the check")
    }
    @Test fun `captured plus currently rendered population cannot use historical UUID checks`() {
        val check = GimmiegoldSparklingCheck()
        (1..4).forEach { check.floor(it, false) }
        check.inventory(0)
        repeat(6) { check.scan(1, live(1), true) }
        assertFalse(check.complete, "many old UUIDs do not establish four current-or-captured critters")
        val id = UUID.randomUUID()
        check.scan(1, listOf(id, id, id), true)
        assertFalse(check.complete)
        check.scan(1, live(3), false)
        assertFalse(check.complete, "preserve the qualifying scan area")
        check.scan(1, live(3), true)
        assertTrue(check.complete)
        check.inventory(1)
        assertFalse(check.complete)
        check.inventory(0)
        check.scan(4, emptyList(), true)
        assertTrue(check.complete)
    }
    @Test fun `party notices add quantities once and do not double count own starting coins`() {
        val check = GimmiegoldSparklingCheck()
        (1..5).forEach { check.floor(it, false) }
        check.inventory(0)
        check.contributions(2, "Me", mapOf("me" to 2, "friend" to 1, "other" to 2))
        assertEquals(10, check.required)
        repeat(3) { check.contributions(2, "Me", mapOf("ME" to 2, "friend" to 1, "other" to 2)) }
        assertEquals(10, check.required)
        check.scan(3, live(6), true)
        assertFalse(check.complete)
        check.scan(3, live(7), true)
        assertTrue(check.complete)
    }
    @Test fun `run routing uses the slot count and accepted combined notices even after hd`() {
        val run = SafariRun(0)
        run.localPlayer("Me")
        run.visitBiome(SafariBiome.HAUNTED)
        run.inventoryEvidence(emptyList(), 0)
        (1..5).forEach { run.gimmiegoldCheck.floor(it, false) }
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        run.scanGimmiegolds(live(3), true)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.partyFullClear("Party > Friend: hd")
        repeat(2) { SafariMessages.forRun("Party > Friend: [SM] Use: 2x Shining Coin + 1x Yogi Berry", run, true) }
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        assertEquals(7, run.gimmiegoldCheck.required)
        run.scanGimmiegolds(live(4), true)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        run.scanGimmiegolds(live(5), true)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertTrue(run.uniqueComplete("Gimmiegold", party()), "ordinary Unique still uses its first catch")
        assertEquals(0, SafariRun(1).gimmiegoldCheck.floorMinimum)
    }
    @Test fun `unknown catalog and unknown inventory do not pass from a report or empty scan`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.HAUNTED)
        run.partyFullClear("Party > Friend: hd")
        repeat(10) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        run.scanGimmiegolds(emptyList(), true)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        run.gimmiegoldCheck.floor(1, false)
        run.scanGimmiegolds(emptyList(), true)
        assertFalse(run.sparklingComplete("Gimmiegold", party()))
        run.inventoryEvidence(emptyList(), 10000)
        run.scanGimmiegolds(emptyList(), true)
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
    }
    @Test fun `outside hd trusts Sparkling checks but cannot invent Unique or Full Clear catches`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        (1..5).forEach { run.gimmiegoldCheck.floor(it, true) }
        run.inventoryEvidence(listOf("Shining Coin" to 1), 0)
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertFalse(run.uniqueComplete("Gimmiegold", party()))
        assertFalse(run.fullClearComplete("Gimmiegold"))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false))
        assertTrue(run.uniqueComplete("Gimmiegold", party()))
        assertFalse(run.fullClearComplete("Gimmiegold"))
        run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "inside Haunted must revalidate the floor requirements")
        run.partyFullClear("Party > Friend: hd")
        assertFalse(run.sparklingComplete("Gimmiegold", party()), "a report received inside cannot waive local sources")
        run.visitBiome(SafariBiome.FOREST)
        run.partyFullClear("Party > Friend: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        run.observeSparkling(UUID.randomUUID(), "Gimmiegold")
        assertTrue(run.sparklingComplete("Gimmiegold", party()), "detected entities cannot override trusted completion")
    }

    @Test fun `outside report keeps the floor-derived Full Clear minimum while Unique needs one catch`() {
        val run = SafariRun(0)
        run.localPlayer("Me")
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(emptyList(), 0)
        (1..5).forEach { run.gimmiegoldCheck.floor(it, true) }
        run.partyFullClear("Party > Friend: hd")
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        assertTrue(run.sparklingComplete("Gimmiegold", party()))
        assertTrue(run.uniqueComplete("Gimmiegold", party()))
        assertFalse(run.fullClearComplete("Gimmiegold"))
        repeat(2) { run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false)) }
        assertTrue(run.fullClearComplete("Gimmiegold"))
    }

}
