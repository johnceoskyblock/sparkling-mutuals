package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.PartyCommand
import net.johnceo.sparklingmutuals.commands.PartyCommandKind
import net.johnceo.sparklingmutuals.commands.ResponderElection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyCommandTest {
    @Test
    fun `only exact party commands trigger lookups`() {
        assertEquals("!mutuals", PartyCommand.fromChat("Party > [MVP+] Farmer: !MuTuAl")?.command?.text)
        assertEquals("!tickets JohnCEO", PartyCommand.fromChat("Party > Farmer: !TICKET JohnCEO")?.command?.text)
        assertEquals("!commands", PartyCommand.fromChat("Party > [VIP] Farmer: !commands")?.command?.text)
        for (line in listOf("Guild > Farmer: !mutuals", "Party > Farmer: I typed !mutuals",
            "Party > Farmer: !tickets Farmer extra", "Party > Farmer: !missing", "!commands")) {
            assertNull(PartyCommand.fromChat(line), line)
        }
    }

    @Test
    fun `request fingerprints distinguish command senders and arguments`() {
        val one = PartyCommand.fromChat("Party > Farmer: !tickets JohnCEO")!!
        assertEquals(one.token, PartyCommand.fromChat("Party > Farmer: !TICKET JohnCEO")!!.token)
        assertNotEquals(one.token, PartyCommand.fromChat("Party > Other: !tickets JohnCEO")!!.token)
        assertNotEquals(one.token, PartyCommand.fromChat("Party > Farmer: !tickets Other")!!.token)
    }

    private val members = listOf("00000000-0000-0000-0000-000000000001",
        "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000003")

    @Test
    fun `one updated mod responds when all party members have it`() {
        val clients = members.map { ResponderElection(it, members, 0) }
        assertEquals(listOf(true, false, false), clients.map { it.shouldStartLookup(200) })
        clients.forEach { it.observeResponse(members[0]) }
        assertEquals(listOf(false, false, false), clients.map { it.shouldStartLookup(1800) })
        assertFalse(clients[0].canPublish(1800), "A reply must cancel even a lookup already running")
        assertFalse(clients[0].shouldStartLookup(1900), "A request must not execute twice")
    }

    @Test
    fun `a mod user responds even if earlier party members do not have the mod`() {
        val client = ResponderElection(members[2], members.reversed(), 0)
        assertFalse(client.shouldStartLookup(1799))
        assertTrue(client.shouldStartLookup(1800))
        assertTrue(client.canPublish(2800))
    }

    @Test
    fun `another member final reply cancels a scheduled response`() {
        val clients = members.take(2).map { ResponderElection(it, members, 0) }
        clients.forEach { it.observeResponse(members[1]) }
        assertEquals(listOf(false, false), clients.map { it.shouldStartLookup(2200) })
    }

    @Test
    fun `outsiders cannot take the election and abandoned requests expire`() {
        val client = ResponderElection(members[0], members, 0)
        client.observeResponse("00000000-0000-0000-0000-000000000099")
        assertTrue(client.shouldStartLookup(200))
        val abandoned = ResponderElection(members[0], members, 0)
        assertFalse(abandoned.shouldStartLookup(30_001))
        assertFalse(abandoned.canPublish(30_001))
    }

    @Test
    fun `all three result formats match without accepting checking announcements`() {
        val commands = listOf(PartyCommand(PartyCommandKind.MISSING, "JohnCEO"),
            PartyCommand(PartyCommandKind.TICKETS, "JohnCEO"), PartyCommand(PartyCommandKind.MUTUALS))
        val results = listOf("Missing Timesave Sparklings for JohnCEO: None",
            "JohnCEO: Basic (233), Economy (0), Premium (0), First-Class (5)",
            "Mutual Timesave Sparkling Critters: Honeybug")
        commands.forEachIndexed { index, command ->
            assertTrue(command.matchesResponse(results[index]))
            assertFalse(command.matchesResponse("[SM] Checking 9da57aacef4d ${members[0]}: ${command.text}"))
            results.forEachIndexed { other, result ->
                if (index != other) assertFalse(command.matchesResponse(result))
            }
        }
    }

    @Test
    fun `responses for another target do not cancel a request`() {
        val missing = PartyCommand(PartyCommandKind.MISSING, "JohnCEO")
        assertFalse(missing.matchesResponse("Missing Timesave Sparklings for JohnFounder: None"))
        assertTrue(missing.matchesResponse("Missing Timesave Sparklings for johnceo: None"))
        val tickets = PartyCommand(PartyCommandKind.TICKETS, "JohnCEO")
        assertFalse(tickets.matchesResponse("JohnFounder: Basic (1), Economy (0), Premium (0), First-Class (0)"))
    }

    @Test
    fun `failures are single contextual replies without an SM prefix`() {
        for (kind in listOf(PartyCommandKind.MISSING, PartyCommandKind.TICKETS, PartyCommandKind.MUTUALS)) {
            val command = PartyCommand(kind, "JohnCEO")
            val reply = command.failureResult("The Hypixel API key is invalid.")
            assertFalse(reply.contains("[SM]"))
            assertTrue(command.matchesResponse(reply))
        }
    }

    @Test
    fun `a remote party member absent from tab list still cancels a lookup`() {
        val client = ResponderElection(members[0], members, 0)
        assertTrue(client.shouldStartLookup(200))
        client.observeResponse(null)
        assertFalse(client.canPublish(500))
    }
}
