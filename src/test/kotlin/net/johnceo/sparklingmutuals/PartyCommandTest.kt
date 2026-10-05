package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.PartyCommand
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
        assertTrue(clients[0].shouldClaim(200))
        clients.forEach { it.observeClaim(members[0], 200) }
        assertFalse(clients[1].shouldClaim(1000))
        assertFalse(clients[2].shouldClaim(1800))
        assertEquals(listOf(true, false, false), clients.map { it.shouldRespond(1200) })
        assertFalse(clients[0].shouldRespond(1300), "An election must not execute twice")
    }

    @Test
    fun `a mod user responds even if earlier party members do not have the mod`() {
        val client = ResponderElection(members[2], members.reversed(), 0)
        assertFalse(client.shouldClaim(1799))
        assertTrue(client.shouldClaim(1800))
        client.observeClaim(members[2], 1800)
        assertFalse(client.shouldRespond(2799))
        assertTrue(client.shouldRespond(2800))
    }

    @Test
    fun `delayed simultaneous claims select the same winner`() {
        val clients = members.take(2).map { ResponderElection(it, members, 0) }
        clients.forEach { it.observeClaim(members[1], 1000) }
        clients.forEach { it.observeClaim(members[0], 1200) }
        assertEquals(listOf(true, false), clients.map { it.shouldRespond(2200) })
    }

    @Test
    fun `outsiders cannot take the election and abandoned requests expire`() {
        val client = ResponderElection(members[0], members, 0)
        client.observeClaim("00000000-0000-0000-0000-000000000099", 100)
        assertTrue(client.shouldClaim(200))
        assertFalse(client.shouldClaim(30_001))
        assertFalse(client.shouldRespond(30_001))
    }
}
