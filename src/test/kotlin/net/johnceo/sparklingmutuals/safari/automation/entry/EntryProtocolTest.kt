package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class EntryProtocolTest {
    private val token = "a".repeat(32)
    private val event = "b".repeat(32)

    @Test fun `round trip all canonical species and the three message shapes`() {
        val query = EntryMessage.Query(token, "main", "mini88A")
        val none = EntryMessage.None("Player_1", token, "alpha", "mini88A")
        assertEquals(query, SparklingEntryProtocol.parse(SparklingEntryProtocol.encode(query)))
        assertEquals(none, SparklingEntryProtocol.parse(SparklingEntryProtocol.encode(none)))
        SafariRoster.all.forEach { critter ->
            val message = EntryMessage.Live("Player_1", token, "main", "mini88A", critter.name, 123, event)
            assertEquals(message, SparklingEntryProtocol.parse(SparklingEntryProtocol.encode(message)))
        }
        assertTrue(SparklingEntryProtocol.encode(EntryMessage.Live("P", token, "main", "mini1", "Mantis Shrimp", 0, event)).contains("MANTIS_SHRIMP"))
    }

    @Test fun `strict fields reject malformed or ambiguous coordination input`() {
        val good = "SMSE1 R Player_1 $token main mini88A LIVE BLUEBIRD 0 $event"
        assertNotNull(SparklingEntryProtocol.parse(good))
        listOf(
            "$good extra", "$good\n", "$good\r", " $good", good.replace("SMSE1", "SMSE2"),
            good.replace(" main ", " beta "), good.replace("mini88A", "server.example.com"),
            good.replace("Player_1", "x".repeat(17)), good.replace("BLUEBIRD", "UNKNOWN"),
            good.replace("BLUEBIRD", "Bluebird"), good.replace(" 0 ", " -1 "),
            good.replace(" 0 ", " 5001 "), good.replace(" 0 ", " 999999999999999999999999 "),
            good.replace(token, token.uppercase()), good.replace(token, "a".repeat(31)),
            good.replace(" main ", "\tmain "), good.replace(" main ", "  main "),
            "x".repeat(1025), "Party > Player_1: $good"
        ).forEach { assertNull(SparklingEntryProtocol.parse(it), it) }
    }

    @Test fun `wire constructors and parser share bounds`() {
        assertThrows(IllegalArgumentException::class.java) { EntryMessage.Query("bad", "main", "mini1") }
        assertThrows(IllegalArgumentException::class.java) { EntryMessage.None("", token, "main", "mini1") }
        assertThrows(IllegalArgumentException::class.java) { EntryMessage.Live("P", token, "main", "mini1", "unknown", 0, event) }
        assertThrows(IllegalArgumentException::class.java) { EntryMessage.Live("P", token, "main", "mini1", "Bluebird", Long.MAX_VALUE, event) }
        assertNotNull(SparklingEntryProtocol.parse("SMSE1 Q $token main ${"s".repeat(64)}"))
        assertNull(SparklingEntryProtocol.parse("SMSE1 Q $token main ${"s".repeat(65)}"))
    }

    @Test fun `only exact genuine party notice is a named sighting hint`() {
        assertEquals("Leader" to "Bluebird", SparklingEntryProtocol.hint("§9Party > §b[MVP+] Leader: §6SPARKLING Bluebird!"))
        assertEquals("Leader" to "Mantis Shrimp", SparklingEntryProtocol.hint("Party > Leader: sparkling mantis shrimp!"))
        listOf(
            "Leader: SPARKLING Bluebird!", "Guild > Leader: SPARKLING Bluebird!",
            "From Leader: SPARKLING Bluebird!", "Party > Leader: quote SPARKLING Bluebird!",
            "Party > Leader: SPARKLING Bluebird! (Forest 1 2 3)",
            "Party > Leader: CAPTURE! You caught a SPARKLING Bluebird!",
            "SPARKLING! Leader caught a SPARKLING Bluebird!", "Party > Leader: SPARKLING unknown!",
            "Party > Leader: SPARKLING Bluebird!\nSMSE1 Q $token main mini1"
        ).forEach { assertNull(SparklingEntryProtocol.hint(it), it) }
    }

    @Test fun `context snapshots member identities and rejects invalid identity scopes`() {
        val self = UUID.randomUUID(); val leader = UUID.randomUUID()
        val mutable = mutableSetOf(self, leader)
        val party = EntryParty(leader, mutable, 1)
        mutable.clear()
        assertEquals(setOf(self, leader), party.members)
        val context = EntryContext(self, "Player_1", 1, 1, 1, "main", "mini88A", party)
        assertEquals(context, context.copy(party = EntryParty(leader, setOf(self, leader), 1)))
        assertThrows(IllegalArgumentException::class.java) { EntryParty(leader, setOf(self), 1) }
        assertThrows(IllegalArgumentException::class.java) { context.copy(account = UUID.randomUUID()) }
        assertThrows(IllegalArgumentException::class.java) { context.copy(world = -1) }
    }
}
