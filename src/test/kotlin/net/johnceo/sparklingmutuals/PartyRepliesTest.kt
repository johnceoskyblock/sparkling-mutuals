package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.api.*
import net.johnceo.sparklingmutuals.commands.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyRepliesTest {
    private val tickets = PartyCommand(PartyCommandKind.TICKETS, "JohnCEO")
    private val result = "JohnCEO: Basic (233), Economy (0), Premium (0), First-Class (5)"
    @Test fun `missing key skips network and produces a local message only`() {
        val reply = PartyLookupResult.fetch(tickets, "") { fail<String>("Must not start a lookup without a key") }
        assertFalse(reply.successful)
        assertNull(reply.partyMessage)
        assertTrue(reply.text.contains("No Hypixel API key"))
        assertFalse(tickets.matchesResponse(reply.text))
        val help = PartyLookupResult.fetch(PartyCommand(PartyCommandKind.HELP), "") { CommandHelp.partyReply() }
        assertTrue(help.successful)
        assertNotNull(help.partyMessage)
    }
    @Test fun `every lookup failure stays local but successful results are publishable`() {
        for (failure in listOf(ApiFailure(ApiFailureKind.INVALID_KEY, "Invalid key"),
            ApiFailure.unavailable(), ApiFailure.dataUnavailable(), ApiFailure(ApiFailureKind.RATE_LIMIT, "Request limit"))) {
            val reply = PartyLookupResult.fetch(tickets, "key") { throw failure }
            assertFalse(reply.successful)
            assertNull(reply.partyMessage)
            assertTrue(reply.text.contains(failure.message!!))
        }
        val unexpected = PartyLookupResult.fetch(tickets, "key") { throw IllegalStateException("private detail") }
        assertNull(unexpected.partyMessage)
        assertFalse(unexpected.text.contains("private detail"))
        assertEquals(result, PartyLookupResult.fetch(tickets, "key") { result }.partyMessage)
    }
    @Test fun `chat results received during lookup prevent a second reply without blocking future requests`() {
        val history = PartyResponseHistory()
        val before = history.cursor
        history.record("Safari Tickets for JohnCEO: No Hypixel API key is configured.", null)
        assertFalse(history.hasReply(tickets, before, listOf("first", "second")))
        history.record(result, "second")
        assertTrue(history.hasReply(tickets, before, listOf("first", "second")))
        assertFalse(history.hasReply(tickets, history.cursor, listOf("first", "second")))
        assertFalse(history.hasReply(PartyCommand(PartyCommandKind.TICKETS, "Other"), before, listOf("second")))
        history.clear()
        assertFalse(history.hasReply(tickets, 0, listOf("second")))
    }
    @Test fun `errors and outsiders cannot cancel a healthy responder but unseen party members can`() {
        val history = PartyResponseHistory()
        history.record(result, "outsider")
        assertFalse(history.hasReply(tickets, 0, listOf("member")))
        history.record(result, null)
        assertTrue(history.hasReply(tickets, 0, listOf("member")))
        for (kind in listOf(PartyCommandKind.MISSING, PartyCommandKind.MUTUALS)) {
            val command = PartyCommand(kind, "JohnCEO")
            assertFalse(command.matchesResponse(command.failureResult("No API key configured")))
            assertFalse(command.matchesResponse(command.failureResult("The API is unavailable")))
            assertTrue(command.matchesResponse(command.failureResult("None")))
            assertTrue(command.matchesResponse(command.failureResult("Rockmite, All Birds")))
        }
    }
}
