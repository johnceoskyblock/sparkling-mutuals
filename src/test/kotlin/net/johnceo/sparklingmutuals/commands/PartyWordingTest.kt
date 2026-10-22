package net.johnceo.sparklingmutuals.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyWordingTest {
    @Test fun `party achievement names and copied downtime reasons avoid detection keywords`() {
        for ((source, expected) in listOf("First Sparkle" to "First Shiny", "Sparkle Storm" to "Shiny Storm",
            "Max Sparklings" to "Max Shinies", "Same Old Sparkle" to "Same Old Shiny",
            "checking SPARKLING and sparkling critters" to "checking SHINY and shiny critters")) {
            assertEquals(expected, PartyWording.shiny(source))
        }
    }

    @Test fun `every mod responder keeps their own named streak reply`() {
        val request = PartyCommand.fromChat("Party > NonModUser: !since")!!.command
        val alice = request.forResponder("Alice")
        val bob = request.forResponder("Bob")
        val history = PartyResponseHistory()
        history.record("Alice: 3 runs since last shiny: Macaw caught by Friend.", "alice")
        assertTrue(history.hasReply(alice, 0, listOf("alice", "bob")))
        assertFalse(history.hasReply(bob, 0, listOf("alice", "bob")))
        assertTrue(bob.matchesResponse("Bob: 7 runs since last shiny: Gazer caught by Friend."))
        assertFalse(PartyCommand(PartyCommandKind.SINCE_UNIQUE, "Bob")
            .matchesResponse("Bob: 7 runs since last shiny: Gazer caught by Friend."))
    }
}
