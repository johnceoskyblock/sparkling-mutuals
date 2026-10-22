package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartingResourcePartyReminderTest {
    private val all = SafariRoster.all.map { it.name }.toSet()
    private fun state() = PartySparklingState().apply {
        val members = setOf("alice", "bob")
        select(members)
        accept(members, mapOf("alice" to all - setOf("Macaw", "Gimmiegold"), "bob" to all - "Gimmiegold"))
    }
    @Test fun `actual report arms one followup naming the right players per critter`() {
        val run = SafariRun(0)
        run.resources.inventory(listOf("Shining Coin" to 1, "Bag of Seeds" to 2), true)
        val party = state()
        val names = mapOf("alice" to "Alice", "bob" to "Bob")
        val sent = mutableListOf<String>()
        assertFalse(run.partyResourceReminders.dispatch(10000, party, names::get, sent::add))
        run.partyResourceReminders.afterBiomeReport(10000)
        assertFalse(run.partyResourceReminders.dispatch(11499, party, names::get, sent::add))
        assertTrue(run.partyResourceReminders.dispatch(11500, party, names::get, sent::add))
        val message = sent.single()
        assertTrue(message.startsWith("Need to use: "))
        assertTrue(message.contains("2x Bag of Seeds")); assertTrue(message.contains("1x Shining Coin"))
        assertTrue(message.contains("Alice still needs Shiny Macaw"))
        assertTrue(message.contains("Alice, Bob still need Shiny Gimmiegold"))
        assertTrue(message.endsWith("Please wait."))
        assertFalse(run.resources.partyNotice("Party > Alice: $message"), "Reminder must not count the starting items twice")
        run.partyResourceReminders.afterBiomeReport(18000)
        assertFalse(run.partyResourceReminders.dispatch(20000, party, names::get, sent::add))
        run.partyResourceReminders.afterBiomeReport(30000)
        assertFalse(run.partyResourceReminders.dispatch(32000, party, names::get, sent::add))
        assertEquals(1, sent.size, "later biome clears must not repeat the reminder")
        assertFalse(SafariRun(20000).partyResourceReminders.dispatch(30000, party, names::get, sent::add))
    }
    @Test fun `dispatch waits for names and API then resamples unused quantities and current requirements`() {
        val run = SafariRun(0)
        run.resources.inventory(listOf("Shining Coin" to 2, "Bag of Seeds" to 2), true)
        run.partyResourceReminders.afterBiomeReport(0)
        val sent = mutableListOf<String>()
        assertFalse(run.partyResourceReminders.dispatch(1500, PartySparklingState(), { it }, sent::add))
        assertFalse(run.partyResourceReminders.dispatch(1500, state(), { null }, sent::add))
        run.resources.inventory(listOf("Shining Coin" to 1), false)
        assertTrue(run.partyResourceReminders.dispatch(2000, state(), { it }, sent::add))
        assertTrue(sent.single().startsWith("Need to use: 1x Shining Coin;"))
        assertFalse(sent.single().contains("Bag of Seeds"))
        run.partyResourceReminders.afterBiomeReport(3000)
        val party = state()
        party.accept(setOf("alice", "bob"), mapOf("alice" to all, "bob" to all))
        assertFalse(run.partyResourceReminders.dispatch(4500, party, { it }, sent::add))
        assertEquals(1, sent.size)
    }
    @Test fun `zero-discovery player is named and long reminders respect party message length`() {
        val roster = (1..5).map { "player_$it" }.toSet()
        val party = PartySparklingState().apply {
            select(roster)
            accept(roster, roster.associateWith { id -> if (id == "player_1") emptySet() else all -
                listOf("Bluebird", "Parakeet", "Macaw", "Gimmiegold")[id.last().digitToInt() - 2] })
        }
        val run = SafariRun(0)
        run.resources.inventory(listOf("Shining Coin" to 2, "Bag of Seeds" to 2, "Yogi Berry" to 2, "Wriggleworm" to 2), true)
        run.partyResourceReminders.afterBiomeReport(0)
        val sent = mutableListOf<String>()
        for (now in 1500L..15000L step 1500L)
            run.partyResourceReminders.dispatch(now, party, { "LongPlayerName_" + it.last() }, sent::add)
        assertTrue(sent.isNotEmpty())
        assertTrue(sent.all { it.length <= 252 })
        assertTrue(sent.joinToString(" ").contains("LongPlayerName_1"))
        assertTrue(sent.joinToString(" ").contains("Gimmiegold"))
        val early = run.resources.startingNotice(party)!!.startingTexts()
        assertTrue(early.all { it.length <= 252 })
        val receiver = SafariResources()
        early.forEach { assertTrue(receiver.partyNotice("Party > Reporter: $it")) }
        assertEquals(6, receiver.foodContributions().values.sum(), "Repeated quantities across named chunks overlap")
        assertEquals(2, receiver.coinContributions()["reporter"])
    }
    @Test fun `shared local clear arms each clients unused items even when a peer sent the report`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { run.record(SafariCatch(it)) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), true)
        run.resources.inventory(listOf("Shining Coin" to 1), true)
        val party = state()
        run.partyFullClear("Party > OtherModUser: id")
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, party))
        run.completion.armSharedClearReminder(run, SafariBiome.ICY, SafariMode.UNIQUE, party, 10000)
        val sent = mutableListOf<String>()
        assertTrue(run.partyResourceReminders.dispatch(11500, party, { it }, sent::add))
        assertTrue(sent.single().contains("1x Shining Coin"))
        run.completion.armSharedClearReminder(run, SafariBiome.ICY, SafariMode.UNIQUE, party, 20000)
        assertFalse(run.partyResourceReminders.dispatch(22000, party, { it }, sent::add))
    }

    @Test fun `destination report waits for still held needed starting items`() {
        val run = SafariRun(0)
        val party = state()
        assertTrue(run.canReportBiome(SafariBiome.HAUNTED, party))
        run.resources.inventory(listOf("Shining Coin" to 1), true)
        assertTrue(run.hasUnusedStartingForBiome(SafariBiome.HAUNTED, party))
        assertFalse(run.canReportBiome(SafariBiome.HAUNTED, party))
        run.resources.inventory(emptyList(), false)
        assertTrue(run.canReportBiome(SafariBiome.HAUNTED, party))
    }

}
