package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.party.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariPartyRefreshTest {
    private val members = listOf("00000000-0000-0000-0000-000000000001",
        "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000003",
        "00000000-0000-0000-0000-000000000004")
    @Test fun `all ranks start network immediately and publish every half second after readiness`() {
        val clients = members.reversed().map { ResponderElection(it.uppercase(), members + members[0], 0) }
        clients.forEach { assertTrue(it.shouldStartLookup(0)); it.lookupReady(10_000) }
        assertEquals(listOf(true, false, false, false), clients.map { it.canPublish(10_000) })
        assertEquals(listOf(true, true, false, false), clients.map { it.canPublish(10_500) })
        assertEquals(listOf(true, true, true, false), clients.map { it.canPublish(11_000) })
        assertEquals(listOf(true, true, true, true), clients.map { it.canPublish(11_500) })
        assertFalse(clients[0].shouldStartLookup(11_500))
    }
    @Test fun `slow API readiness cannot consume the fallback window`() {
        val client = ResponderElection(members[0], members, 0)
        assertTrue(client.shouldStartLookup(0))
        assertFalse(client.canPublish(50_000))
        client.lookupReady(50_000)
        assertFalse(client.canPublish(51_499))
        assertTrue(client.canPublish(51_500))
        client.observeResponse(null)
        assertFalse(client.canPublish(52_000))
    }
    @Test fun `duplicate suppression compares the actual result within the chat body`() {
        val history = PartyResponseHistory()
        val result = "Missing Timesave Sparklings for Alice: Rockmite, Honeybug"
        history.record("Missing Timesave Sparklings for Alice: Rockmite", "friend")
        assertFalse(history.containsResult(result, 0, listOf("me", "friend")))
        history.record("[SM] $result", "friend")
        assertTrue(history.containsResult(result, 0, listOf("me", "friend")))
        assertFalse(history.containsResult(result, history.cursor, listOf("me", "friend")))
        assertFalse(history.containsResult(result, 0, listOf("me")))
        history.record(result, null)
        assertTrue(history.containsResult(result, 0, listOf("me")))
        assertFalse(history.containsResult("Bob's Doomspiral PB: 1:00.000", 0, listOf("me")))
    }
    @Test fun `a confirmed solo roster includes only self then follows joins leaves and disband`() {
        val party = PartyRosterState()
        assertNull(party.withLocal("me"))
        party.update(emptyList())
        assertEquals(setOf("me"), party.withLocal("me"))
        val state = PartySparklingState()
        state.select(party.withLocal("me")!!)
        assertTrue(state.accept(setOf("me"), mapOf("me" to setOf("FOXTROT"))))
        assertFalse(state.needs("Foxtrot"))
        party.invalidate()
        assertNull(party.withLocal("me"))
        party.update(listOf("me", "friend"))
        state.select(party.withLocal("me")!!)
        assertFalse(state.accept(setOf("me"), mapOf("me" to setOf("FOXTROT"))))
        assertTrue(state.accept(setOf("me", "friend"), mapOf("me" to setOf("FOXTROT"), "friend" to emptySet())))
        assertTrue(state.needs("Foxtrot"))
        party.update(emptyList())
        state.select(party.withLocal("me")!!)
        assertTrue(state.accept(setOf("me"), mapOf("me" to setOf("FOXTROT"))))
        assertFalse(state.needs("Foxtrot"))
    }
    @Test fun `only server membership notifications refresh the party roster`() {
        for (line in listOf("[MVP+] Alice joined the party.", "[VIP] Bob has left the party.",
            "[MVP+] Alice has been removed from the party.", "You have joined [MVP+] Alice's party!",
            "You left the party.", "[MVP+] Alice has disbanded the party!",
            "You have been kicked from the party by [MVP+] Alice",
            "The party was disbanded because all invites expired and the party was empty."))
            assertTrue(PartyRosterSignals.changed(line), line)
        for (line in listOf("Party > Alice: Bob joined the party.", "Guild > Alice: You left the party.",
            "[MVP+] Alice: You have joined my party!", "CAPTURE! You caught a Macaw", "Alice invited you to join their party!"))
            assertFalse(PartyRosterSignals.changed(line), line)
    }
}
