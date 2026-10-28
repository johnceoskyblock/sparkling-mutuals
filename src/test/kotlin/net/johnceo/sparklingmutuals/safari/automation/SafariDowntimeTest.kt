package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SafariDowntimeTest {
    @BeforeEach fun clear() { ConfigManager.downtimeCommand = true; SafariDowntime.reset() }
    @AfterEach fun restore() { ConfigManager.downtimeCommand = true; SafariDowntime.reset() }

    @Test fun `Torrhus leader warp starts other contest holds together without readiness and preserves manual reasons`() {
        SafariDowntime.chat("Party > Food: !dt food", false, "Leader")
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Leader", listOf("Leader", "Friend", "NoMod", "Food"))
        val confirmation = "SkyBlock Party Warp (3 players)"
        assertFalse(SafariDowntime.partyWarp("Party > Leader: $confirmation", true, true, "Leader", 0))
        SafariDowntime.partyWarp(confirmation, false, true, "Leader", 0)
        SafariDowntime.partyWarp(confirmation, true, false, "Leader", 0)
        SafariDowntime.releaseContestTravel(20000)
        assertEquals("Food, Leader, Friend, NoMod", SafariDowntime.holders())
        SafariDowntime.partyWarp(confirmation, true, true, "Leader", 30000)
        SafariDowntime.partyWarp(confirmation, true, true, "Leader", 40000)
        SafariDowntime.chat("Party > Friend: Ticket earned!", false, "Leader")
        SafariDowntime.releaseContestTravel(49999)
        assertEquals("Food, Leader, NoMod", SafariDowntime.holders())
        SafariDowntime.releaseContestTravel(50000)
        assertEquals("Food, Leader", SafariDowntime.holders())
        assertEquals("Food (food), Leader (contest)", SafariDowntime.waitingFor())
        SafariDowntime.chat("Party > Leader: r", false, "Leader")
        assertEquals("Food", SafariDowntime.holders())
    }

    @Test fun `party warp deadline survives resolving a contest member name`() {
        val id = "a".repeat(32)
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Leader", listOf("Leader", id))
        SafariDowntime.partyWarp("SkyBlock Party Warp (1 player)", true, true, "Leader", 1000)
        SafariDowntime.pruneVerified(mapOf("friend" to id), setOf(id))
        SafariDowntime.releaseContestTravel(21000)
        assertEquals("Leader", SafariDowntime.holders())
        assertTrue(SafariDowntime.localNotice().startsWith("[SM] Contest:"))
        assertTrue(SafariDowntime.localNotice().contains("Leader (contest)"))
    }

    @Test fun `contest members release individually twenty seconds after their Torrhus travel`() {
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", "SharkboardMC", "Friend"))
        SafariDowntime.chat("Party > Leader: r", false, "Me")
        assertTrue(SafariDowntime.travel(" \u00BB SharkboardMC is traveling to Torrhus Canyon FOLLOW", 1000))
        SafariDowntime.travel("\u00BB Friend is traveling to Torrhus Canyon FOLLOW", 6000)
        SafariDowntime.travel("\u00BB SharkboardMC is traveling to Torrhus Canyon FOLLOW", 2000)
        SafariDowntime.transfer()
        SafariDowntime.releaseContestTravel(20999)
        assertEquals("SharkboardMC, Friend", SafariDowntime.holders())
        SafariDowntime.releaseContestTravel(21000)
        assertEquals("Friend", SafariDowntime.holders())
        assertTrue(SafariDowntime.entryHold)
        SafariDowntime.releaseContestTravel(26000)
        assertFalse(SafariDowntime.entryHold)
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `travel cannot release other reasons quoted notices or members without a matching notice`() {
        SafariDowntime.chat("Party > Friend: !dt food", false, "Me")
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", "Friend"))
        for (text in listOf("Party > Leader: \u00BB Leader is traveling to Torrhus Canyon FOLLOW",
            "Leader: \u00BB Leader is traveling to Torrhus Canyon FOLLOW",
            "\u00BB Leader is traveling to The Garden FOLLOW")) assertFalse(SafariDowntime.travel(text, 0))
        SafariDowntime.travel("\u00BB Outsider is traveling to Torrhus Canyon FOLLOW", 0)
        SafariDowntime.travel("\u00BB Friend is traveling to Torrhus Canyon FOLLOW", 0)
        SafariDowntime.releaseContestTravel(20000)
        assertEquals("Friend, Leader", SafariDowntime.holders())
        SafariDowntime.travel("\u00BB Leader is traveling to Torrhus Canyon FOLLOW", 20000)
        SafariDowntime.releaseContestTravel(40000)
        assertEquals("Friend", SafariDowntime.holders(), "Non-contest downtime still needs its own readiness")
    }
    @Test fun `new contest and party reset cannot inherit an old trip timer`() {
        SafariDowntime.chat("Party > Friend: !dt contest", false, "Me")
        SafariDowntime.travel("\u00BB Friend is traveling to Torrhus Canyon FOLLOW", 0)
        SafariDowntime.chat("Party > Friend: Ticket earned!", false, "Me")
        SafariDowntime.chat("Party > Friend: !dt contest", false, "Me")
        SafariDowntime.releaseContestTravel(30000)
        assertTrue(SafariDowntime.entryHold)
        SafariDowntime.travel("\u00BB Friend is traveling to Torrhus Canyon FOLLOW", 30000)
        SafariDowntime.reset()
        SafariDowntime.chat("Party > Friend: !dt contest", false, "Me")
        SafariDowntime.releaseContestTravel(60000)
        assertTrue(SafariDowntime.entryHold)
    }

    @Test fun `readiness accepts clear confirmations but ignores unrelated r words questions and negation`() {
        for (body in listOf("r", "R!", "r/", "/r", "rrr", "rrt", "rt", "er", "ready", "rdy", "I'm ready", "im ready now!", "okay, ready to go!", "we are finally ready", "Ready when you are", "I'm all ready!")) {
            assertTrue(SafariDowntime.readyMessage(body), body)
            SafariDowntime.chat("Party > Friend: !dt food", false, "Me")
            SafariDowntime.chat("Party > Friend: $body", false, "Me")
            assertFalse(SafariDowntime.entryHold, body)
        }
        for (body in listOf("nr", "NR!", "nr/", "brb", "red", "random", "party", "ranger", "already", "not ready", "I'm not ready", "almost ready", "are you ready?", "ready?", "I was ready", "I will be ready", "ready or not", "my friend is ready"))
            assertFalse(SafariDowntime.readyMessage(body), body)
    }
    @Test fun `leader contest waits for every member and local notice lists remaining players`() {
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", "Me", "Friend"))
        assertEquals("Leader, Me, Friend", SafariDowntime.holders())
        assertTrue(SafariDowntime.localNotice().contains("wait for Leader (contest), Me (contest), Friend (contest) to say r"))
        SafariDowntime.chat("Party > Leader: r", false, "Me")
        assertTrue(SafariDowntime.entryHold)
        assertEquals("Me, Friend", SafariDowntime.holders())
        SafariDowntime.chat("Party > Friend: Ticket earned!", false, "Me")
        assertTrue(SafariDowntime.entryHold)
        SafariDowntime.chat("Party > Outsider: r", false, "Me")
        assertTrue(SafariDowntime.entryHold)
        SafariDowntime.chat("Party > Me: ticket earned", false, "Me")
        assertFalse(SafariDowntime.entryHold)
    }
    @Test fun `unresolved contest member stays held until name resolves or they leave the party`() {
        val id = "a".repeat(32)
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", id))
        SafariDowntime.chat("Party > Leader: r", false, "Me")
        assertTrue(SafariDowntime.entryHold)
        SafariDowntime.pruneVerified(mapOf("friend" to id), setOf(id))
        assertEquals("friend", SafariDowntime.holders())
        SafariDowntime.chat("Party > Friend: r/", false, "Me")
        assertFalse(SafariDowntime.entryHold)
        SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", id))
        SafariDowntime.chat("Party > Leader: r", false, "Me")
        SafariDowntime.pruneVerified(emptyMap(), emptySet())
        assertFalse(SafariDowntime.entryHold)
    }
    @Test fun `contest request preserves another members existing downtime reason`() {
        SafariDowntime.chat("Party > Friend: !dt food", false, "Me")
        val summary = SafariDowntime.chat("Party > Leader: !dt contest", false, "Me", listOf("Leader", "Friend", "Me"))
        assertTrue(summary!!.contains("Friend (food)"))
        assertFalse(SafariDowntime.ticketEarned("Friend"))
        assertTrue(SafariDowntime.localNotice().contains("Friend (food), Leader (contest), Me (contest)"))
    }
    @Test fun `any ticket mention clears only its senders downtime request`() {
        for (message in listOf("ticket", "got my TICKET", "two tickets ready", "Ticket earned!", "First-Class Ticket earned!")) {
            SafariDowntime.reset()
            SafariDowntime.chat("Party > Farmer: !dt food", false, "Me")
            SafariDowntime.chat("Party > Friend: !dt contest", false, "Me")
            SafariDowntime.chat("Party > Farmer: $message", false, "Me")
            assertEquals("Friend", SafariDowntime.holders())
            assertTrue(SafariDowntime.entryHold)
            SafariDowntime.chat("Party > Friend: r", false, "Me")
            assertFalse(SafariDowntime.entryHold)
        }
    }
    @Test fun `ticket earned releases only the senders active contest hold`() {
        SafariDowntime.chat("Party > Farmer: !dt contest", false, "Me")
        SafariDowntime.chat("Party > Steve: !dt contest", false, "Me")
        SafariDowntime.chat("Party > Alex: !dt food", false, "Me")
        SafariDowntime.chat("Party > Farmer: Ticket earned!", false, "Me")
        assertTrue(SafariDowntime.holding)
        assertEquals("Steve, Alex", SafariDowntime.holders())
        assertTrue(SafariDowntime.ticketEarned("sTeVe"))
        assertEquals("Alex", SafariDowntime.holders())
        SafariDowntime.chat("Party > Alex: Ticket earned!", false, "Me")
        assertFalse(SafariDowntime.holding, "Ticket earned is ready for any reason")
        SafariDowntime.chat("Party > Alex: r", false, "Me")
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `local ticket cancels a banked contest request before run end and party messages release the last hold`() {
        SafariDowntime.chat("Party > Me: !dt CoNtEsT", true, "Me")
        assertTrue(SafariDowntime.ticketEarned("me"))
        assertNull(SafariDowntime.leftSafari("Me"))
        assertFalse(SafariDowntime.holding)
        SafariDowntime.chat("Party > Friend: !dt contest", false, "Me")
        SafariDowntime.chat("Guild > Friend: Ticket earned!", false, "Me")
        assertTrue(SafariDowntime.holding)
        SafariDowntime.chat("Party > Friend: Ticket earned!", false, "Me")
        assertFalse(SafariDowntime.holding)
        assertEquals("", SafariDowntime.holders())
    }
    @Test fun `a request in the Safari waits for the run to end and any client can announce it`() {
        assertNull(SafariDowntime.chat("Party > [MVP+] Farmer: !dt food", inSafari = true, self = "Other"))
        assertFalse(SafariDowntime.holding)
        assertEquals("Downtime requested: Farmer (food)", SafariDowntime.leftSafari("Other"))
        assertTrue(SafariDowntime.holding)
        SafariDowntime.reset()
        SafariDowntime.chat("Party > Farmer: !dt food", inSafari = true, self = "Farmer")
        assertEquals("Downtime requested: Farmer (food)", SafariDowntime.leftSafari("Farmer"))
    }
    @Test fun `asked outside the Safari it holds straight away`() {
        assertEquals("Downtime requested: Farmer (no reason)", SafariDowntime.chat("Party > Farmer: !DT", inSafari = false, self = "Farmer"))
        assertTrue(SafariDowntime.holding)
    }
    @Test fun `a requester ready before the run ends cancels their pending downtime`() {
        SafariDowntime.chat("Party > Farmer: !dt food", inSafari = true, self = "Farmer")
        SafariDowntime.chat("Party > farmer: R", inSafari = true, self = "Farmer")
        assertNull(SafariDowntime.leftSafari("Farmer"))
        assertFalse(SafariDowntime.holding)
        assertEquals("", SafariDowntime.holders())
    }
    @Test fun `a pending resume only cancels its own request`() {
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = true, self = "Me")
        SafariDowntime.chat("Party > Steve: !dt", inSafari = true, self = "Me")
        SafariDowntime.chat("Party > Alex: r", inSafari = true, self = "Me")
        SafariDowntime.chat("Party > Farmer: r", inSafari = true, self = "Me")
        assertEquals("Steve", SafariDowntime.holders())
        assertFalse(SafariDowntime.holding)
        SafariDowntime.leftSafari("Me")
        assertTrue(SafariDowntime.holding)
    }
    @Test fun `the hold lasts until every requester says r`() {
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = true, self = "Me")
        SafariDowntime.chat("Party > Steve: !dt brb", inSafari = true, self = "Me")
        SafariDowntime.leftSafari("Me")
        SafariDowntime.chat("Party > Alex: r", inSafari = false, self = "Me")
        assertTrue(SafariDowntime.holding, "r from someone who didn't ask changes nothing")
        SafariDowntime.chat("Party > farmer: r", inSafari = false, self = "Me")
        assertTrue(SafariDowntime.holding)
        assertEquals("Steve", SafariDowntime.holders())
        SafariDowntime.chat("Party > Steve: R", inSafari = false, self = "Me")
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `a requester leaving the party takes their request with them`() {
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = true, self = "Me")
        SafariDowntime.chat("Party > Steve: !dt", inSafari = true, self = "Me")
        SafariDowntime.leftSafari("Me")
        SafariDowntime.chat("[MVP+] Farmer has left the party.", inSafari = false, self = "Me")
        assertTrue(SafariDowntime.holding)
        assertEquals("Steve", SafariDowntime.holders())
        SafariDowntime.chat("[VIP] Steve has been removed from the party.", inSafari = false, self = "Me")
        assertFalse(SafariDowntime.holding, "nobody is left who could say r")
        // Pending requests (run still going) are dropped the same way.
        SafariDowntime.chat("Party > Alex: !dt", inSafari = true, self = "Me")
        SafariDowntime.chat("Alex was removed from your party because they disconnected", inSafari = true, self = "Me")
        assertNull(SafariDowntime.leftSafari("Me"))
        assertFalse(SafariDowntime.holding)
        // Someone who never asked leaving changes nothing.
        SafariDowntime.chat("Party > Alex: !dt", inSafari = false, self = "Me")
        SafariDowntime.chat("Bob has left the party.", inSafari = false, self = "Me")
        assertTrue(SafariDowntime.holding)
    }
    @Test fun `disbanding, leaving or joining another party drops every request`() {
        listOf("The party was disbanded because all invites expired and the party was empty.",
            "[MVP++] Farmer has disbanded the party!", "You left the party.",
            "You have been kicked from the party by [MVP+] Farmer", "You have joined [MVP+] Steve's party!").forEach { line ->
            SafariDowntime.chat("Party > Farmer: !dt", inSafari = false, self = "Me")
            SafariDowntime.chat("Party > Steve: !dt", inSafari = true, self = "Me")
            assertTrue(SafariDowntime.holding)
            SafariDowntime.chat(line, inSafari = false, self = "Me")
            assertFalse(SafariDowntime.holding, line)
            assertEquals("", SafariDowntime.holders(), line)
        }
    }
    @Test fun `turning the setting off releases an active hold and drops banked requests`() {
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = false, self = "Me")
        assertTrue(SafariDowntime.holding)
        ConfigManager.downtimeCommand = false
        assertFalse(SafariDowntime.holding, "an active hold stops blocking as soon as the setting is off")
        SafariDowntime.chat("Party > Someone: hi", inSafari = false, self = "Me")
        ConfigManager.downtimeCommand = true
        assertFalse(SafariDowntime.holding, "it does not come back when the setting is turned on again")
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = true, self = "Me")
        ConfigManager.downtimeCommand = false
        assertNull(SafariDowntime.leftSafari("Me"), "the run ending while off does not start a hold")
        ConfigManager.downtimeCommand = true
        assertNull(SafariDowntime.leftSafari("Me"), "and the banked request is gone, not saved for later")
        assertFalse(SafariDowntime.holding)
        // r is ignored while off, so the hold cannot be left half-released either.
        SafariDowntime.chat("Party > Farmer: !dt", inSafari = false, self = "Me")
        ConfigManager.downtimeCommand = false
        assertNull(SafariDowntime.chat("Party > Farmer: r", inSafari = false, self = "Me"))
        ConfigManager.downtimeCommand = true
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `only party chat counts`() {
        assertNull(SafariDowntime.chat("Guild > Farmer: !dt", inSafari = false, self = "Farmer"))
        assertNull(SafariDowntime.chat("Party > Farmer: !dtx", inSafari = false, self = "Farmer"))
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `return announcement includes all requesters and their latest reasons in one message`() {
        SafariDowntime.chat("Party > Alex: !dt food", true, "Me")
        SafariDowntime.chat("Party > Steve: !dt contest", true, "Me")
        SafariDowntime.chat("Party > Bob: !dt", true, "Me")
        assertEquals("Downtime requested: Alex (food); Steve (contest); Bob (no reason)", SafariDowntime.leftSafari("Me"))
        assertNull(SafariDowntime.leftSafari("Me"))
    }

    @Test fun `transfer retains active and banked requests and confirmed roster removes only departed UUIDs`() {
        SafariDowntime.chat("Party > Farmer: !dt food", true, "Me")
        SafariDowntime.transfer()
        assertTrue(SafariDowntime.holding, "banked request becomes the next-entry hold on transfer")
        assertEquals("Downtime requested: Farmer (food)", SafariDowntime.leftSafari("Farmer"))
        assertNull(SafariDowntime.leftSafari("Farmer"), "announce once")
        assertEquals("Farmer", SafariDowntime.holders())
        SafariDowntime.leftSafari("Me")
        assertTrue(SafariDowntime.holding)
        SafariDowntime.chat("Party > Steve: !dt contest", false, "Me")
        SafariDowntime.pruneVerified(mapOf("farmer" to "a", "steve" to "b"), setOf("b"))
        assertEquals("Steve", SafariDowntime.holders())
        assertTrue(SafariDowntime.holding)
        SafariDowntime.chat("Party > Steve: r", false, "Me")
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `case variants update one request and quoted readiness never releases somebody else`() {
        SafariDowntime.chat("Party > Farmer: !dt food", false, "Me")
        assertEquals("Downtime requested: Farmer (brb)", SafariDowntime.chat("Party > fArMeR: !dt brb", false, "Me"))
        assertEquals("Farmer", SafariDowntime.holders())
        SafariDowntime.chat("Party > Steve: Farmer r", false, "Me")
        SafariDowntime.chat("Party > Steve: Farmer Ticket earned!", false, "Me")
        assertTrue(SafariDowntime.holding)
        assertFalse(SafariDowntime.ticketEarned("Farmer"), "local scoreboard is only applicable to contest requests")
        SafariDowntime.chat("Party > FARMER: Ticket earned!", false, "Me")
        assertFalse(SafariDowntime.holding)
    }
    @Test fun `Torrhus interaction holds banked readiness before the run exit tick`() {
        SafariDowntime.chat("Party > Farmer: !dt food", true, "Me")
        assertFalse(SafariDowntime.holding)
        assertTrue(EntryGuardDecision.block(EntryStage.TORRHUS, SafariDowntime.entryHold, false, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, SafariDowntime.entryHold, false, true, false))
    }
}
