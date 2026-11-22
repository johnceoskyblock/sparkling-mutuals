package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.SafariProfile
import net.johnceo.sparklingmutuals.history.SafariSessionStats
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariSparklingReplyTest {
    @Test fun `capture counts share the profile dex and handle an unavailable total`() {
        val profile = SafariProfile(setOf("FOXTROT", "DOOMSPIRAL", "MANTIS_SHRIMP", "UNKNOWN"),
            emptyMap(), null, null, 14)
        val command = PartyCommand(PartyCommandKind.SPARKLING, "Friend")
        val reply = SafariSparklingReply.captures("Friend", profile)
        assertEquals("Friend: Total: 14, Unique: 3, Duplicate: 11", reply)
        assertTrue(command.requiresApiKey)
        assertTrue(command.matchesResponse(reply))
        assertFalse(command.matchesResponse(SafariSparklingReply.captures("Other", profile)))
        assertEquals("Friend: Total: N/A, Unique: 3, Duplicate: N/A",
            SafariSparklingReply.captures("Friend", profile.copy(totalCaptured = null)))
        assertEquals("Friend: Total: 0, Unique: 0, Duplicate: 0",
            SafariSparklingReply.captures("Friend", profile.copy(discoveries = emptySet(), totalCaptured = 0)))
        assertEquals(0L, profile.copy(totalCaptured = 2).duplicateSparklings)
    }

    @Test fun `a matching sparkling lookup shares one response and keeps other player requests`() {
        val history = PartyResponseHistory()
        history.record("Friend: Total: 18, Unique: 3, Duplicate: 15", "one")
        val command = PartyCommand(PartyCommandKind.SPARKLING, "Friend")
        val response = "Friend: Total: 17, Unique: 3, Duplicate: 14"
        assertTrue(history.alreadyAnswered(command, response, 0, listOf("one", "two")))
        assertFalse(history.alreadyAnswered(command, response, history.cursor, listOf("one", "two")))
        assertFalse(history.alreadyAnswered(command.copy(ign = "Other"), response, 0, listOf("one", "two")))
    }

    @Test fun `each mod client can reply with its own session rate even if counts match`() {
        val request = PartyCommand.fromChat("Party > Friend: !RATES")!!
        val stats = SafariSessionStats(103, 108_000, 3)
        val reply = SafariSparklingReply.rates(stats)
        assertEquals("Session sparklings: 3 (34.3 runs/sparkling)", reply)
        assertEquals("Session sparklings: 0", SafariSparklingReply.rates(stats.copy(sparklings = 0)))
        val history = PartyResponseHistory()
        history.record(reply, "one")

        val command = request.command.forResponder("Other")
        assertEquals("Other", command.ign)
        assertTrue(command.personalResult)
        assertFalse(command.requiresApiKey)
        assertTrue(command.matchesResponse(reply))
        assertFalse(history.alreadyAnswered(command, reply, 0, listOf("one", "two")))
    }
}
