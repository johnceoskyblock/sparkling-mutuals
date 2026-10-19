package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class StartingResourceAnnouncementTest {
    @Test fun `member without API profile data makes every mod user report their own unused food and coins`() {
        val all = SafariRoster.all.map { it.name }.toSet()
        val roster = setOf("ModA", "ModB", "NewPlayer")
        val state = PartySparklingState().apply { select(roster) }
        val runs = listOf(SafariRun(0), SafariRun(0))
        runs.forEach { run ->
            run.inventoryEvidence(listOf("Bag of Seeds" to 2, "Shining Coin" to 1), 0)
            run.startingAnnouncement.observeArea(0, 0)
            run.startingAnnouncement.observeArea(2, 100)
            assertFalse(run.startingAnnouncement.dispatch(2100, state) { fail("still loading") })
        }
        val noData = net.johnceo.sparklingmutuals.commands.SafariLookup.discoveriesOrEmpty {
            throw net.johnceo.sparklingmutuals.api.ApiFailure.dataUnavailable()
        }
        assertTrue(state.accept(roster, mapOf("ModA" to all, "ModB" to all, "NewPlayer" to noData)))
        assertTrue(state.ready)
        assertTrue(state.needs("Gimmiegold"))
        assertTrue(listOf("Bluebird", "Parakeet", "Macaw").all(state::needs))
        val sent = mutableListOf<String>()
        assertTrue(runs[0].startingAnnouncement.dispatch(2100, state) { message ->
            sent += message
            SafariMessages.forRun("Party > ModA: $message", runs[1], true)
        })
        assertTrue(runs[1].startingAnnouncement.dispatch(2100, state, sent::add),
            "Another mod user's report must not suppress this client's own starting resources")
        assertEquals(listOf("[SM] Starting items: 2x Bag of Seeds; 1x Shining Coin",
            "[SM] Starting items: 2x Bag of Seeds; 1x Shining Coin"), sent)
        runs.forEach { assertFalse(it.startingAnnouncement.dispatch(4000, state, sent::add)) }
    }
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me"))
        accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }

    @Test fun `two seconds after observed boat departure only needed unused starting items are sent once`() {
        for ((needs, expected) in listOf(
            party("Gimmiegold", "Macaw") to "[SM] Starting items: 2x Bag of Seeds; 1x Shining Coin",
            party("Macaw") to "[SM] Starting items: 2x Bag of Seeds",
            party("Gimmiegold") to "[SM] Starting items: 1x Shining Coin",
            party() to null)) {
            val run = SafariRun(0)
            run.inventoryEvidence(listOf("Bag of Seeds" to 2, "Shining Coin" to 1, "Icebreaker" to 1), 0)
            val sent = mutableListOf<String>()
            val delivery = run.startingAnnouncement
            delivery.observeArea(0, 0)
            assertFalse(delivery.dispatch(10000, needs, sent::add), "time on boat does not trigger sending")
            delivery.observeArea(null, 10001)
            assertFalse(delivery.dispatch(20000, needs, sent::add), "unknown area is not departure")
            delivery.observeArea(2, 20001)
            assertFalse(delivery.dispatch(22000, needs, sent::add))
            delivery.dispatch(22001, needs, sent::add)
            delivery.observeArea(0, 23000)
            delivery.observeArea(4, 24000)
            assertFalse(delivery.dispatch(30000, needs, sent::add))
            assertEquals(listOfNotNull(expected), sent)
            assertFalse(SafariRun(40000).startingAnnouncement.dispatch(100000, needs, sent::add))
        }
    }

    @Test fun `loading discoveries waits rather than broadcasting every resource as missing`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Shining Coin" to 2, "Yogi Berry" to 1), 0)
        run.startingAnnouncement.observeArea(0, 0)
        run.startingAnnouncement.observeArea(3, 100)
        val sent = mutableListOf<String>()
        assertFalse(run.startingAnnouncement.dispatch(2100, PartySparklingState(), sent::add))
        assertTrue(run.startingAnnouncement.dispatch(3000, party("Bluebird"), sent::add))
        assertEquals(listOf("[SM] Starting items: 1x Yogi Berry"), sent)
    }

    @Test fun `dispatch samples unused quantities without announcing later pickups as starting grants`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Shining Coin" to 2), 0)
        run.startingAnnouncement.observeArea(0, 0)
        run.startingAnnouncement.observeArea(4, 4000)
        run.resources.pickup("FLOOR DROP! Bag of Seeds")
        run.inventoryEvidence(listOf("Shining Coin" to 1, "Bag of Seeds" to 1), 6000)
        val sent = mutableListOf<String>()
        assertTrue(run.startingAnnouncement.dispatch(6000, party("Gimmiegold", "Macaw"), sent::add))
        assertEquals(listOf("[SM] Starting items: 1x Shining Coin"), sent)
    }

    @Test fun `failed send is retried but a successfully sent announcement is not repeated`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
        run.startingAnnouncement.observeArea(0, 0)
        run.startingAnnouncement.observeArea(1, 1)
        assertThrows(IllegalStateException::class.java) {
            run.startingAnnouncement.dispatch(2001, party("Macaw")) { error("not sent") }
        }
        assertTrue(run.startingAnnouncement.dispatch(2002, party("Macaw")) {})
        assertFalse(run.startingAnnouncement.dispatch(2003, party("Macaw")) {})
    }

    @Test fun `early mixed reports update both source totals without delayed-notice obligations or echo doubling`() {
        val run = SafariRun(0)
        run.localPlayer("Me")
        run.inventoryEvidence(listOf("Bag of Seeds" to 2, "Shining Coin" to 1), 0)
        val message = "[SM] Starting items: 2x Bag of Seeds; 1x Shining Coin"
        repeat(3) { SafariMessages.forRun("Party > Me: $message", run, true) }
        repeat(3) { SafariMessages.forRun("Party > Friend: $message", run, true) }
        run.birdSpawnsComplete()
        run.scanGimmiegolds(emptyList(), false)
        assertEquals(13, run.birds.feedingTarget, "nine floor plus two local plus two remote")
        assertEquals(2, run.gimmiegoldCheck.required, "local echo is not another coin")
        assertFalse(run.resources.pending(SafariBiome.FOREST))
        assertFalse(run.resources.pending(SafariBiome.HAUNTED))
        // Older reminder format from a sender already counted early must not add another obligation.
        SafariMessages.forRun("Party > Friend: [SM] Use: 2x Bag of Seeds + 1x Shining Coin", run, true)
        assertFalse(run.resources.pending(SafariBiome.FOREST))
        assertFalse(run.resources.pending(SafariBiome.HAUNTED))
        assertEquals(2, run.resources.foodContributions()["friend" to FloorDropType.SEEDS])
    }

    @Test fun `malformed or nonparty early reports cannot partially update either ledger`() {
        for (raw in listOf(
            "Guild > Friend: [SM] Starting items: 2x Bag of Seeds",
            "Party > Friend: [SM] Starting items: 1x Shining Coin; 0x Bag of Seeds",
            "Party > Friend: [SM] Starting items: 1x Shining Coin; 1x Icebreaker",
            "Party > Friend: [SM] Starting items: 1x Bag of Seeds; 1x Bag of Seeds")) {
            val resources = SafariResources()
            assertFalse(resources.partyNotice(raw))
            assertTrue(resources.coinContributions().isEmpty())
            assertTrue(resources.foodContributions().isEmpty())
        }
    }

    @Test fun `early extra demand invalidates old fd and hd without manufacturing captures`() {
        val run = SafariRun(0)
        run.localPlayer("Me")
        run.visitBiome(SafariBiome.CAVERN)
        run.inventoryEvidence(emptyList(), 0)
        (1..4).forEach { run.gimmiegoldCheck.floor(it, false) }
        run.partyFullClear("Party > Clearer: fd")
        run.partyFullClear("Party > Clearer: hd")
        assertTrue(run.sparklingComplete("Bluebird", party("Bluebird")))
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        SafariMessages.forRun("Party > Friend: [SM] Starting items: 2x Bag of Seeds; 1x Shining Coin", run, true)
        assertFalse(run.sparklingComplete("Bluebird", party("Bluebird")))
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        assertEquals(0, run.count("Gimmiegold"))
        run.partyFullClear("Party > Clearer: hd")
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
        run.scanGimmiegolds(List(5) { UUID.randomUUID() }, true)
        assertTrue(run.sparklingComplete("Gimmiegold", party("Gimmiegold")))
    }

    @Test fun `late Haunted entrant can send hd after qualified captures plus renders without an earlier report`() {
        val run = SafariRun(0)
        run.localPlayer("Me")
        run.visitBiome(SafariBiome.CAVERN)
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!, false))
        run.resources.partyNotice("Party > Friend: [SM] Starting items: 1x Shining Coin")
        run.visitBiome(SafariBiome.HAUNTED)
        run.inventoryEvidence(emptyList(), 10000)
        run.updateCaptureEvidence(SafariBiome.HAUNTED, emptySet(), false)
        (1..4).forEach { run.gimmiegoldCheck.floor(it, false) }
        val state = party("Gimmiegold")
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.scanGimmiegolds(List(3) { UUID.randomUUID() }, true)
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.scanGimmiegolds(List(4) { UUID.randomUUID() }, true)
        assertEquals("hd", run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.completion.sent(SafariBiome.HAUNTED)
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
        run.partyFullClear("Party > Friend: hd")
        assertFalse(run.canReportBiome(SafariBiome.HAUNTED), "a queued hd must be cancelled too")
        assertNull(run.completion.announcement(run, SafariBiome.HAUNTED, SafariMode.SPARKLING, state))
    }
}
