package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariLookupBirdsTest {
    private val otherDiscoveries = SafariRoster.all.filterNot { SafariLookup.isBird(it.name) }
        .map { it.name.uppercase().replace(' ', '_') }.toSet()

    @Test fun `timesave missing lists only undiscovered birds individually`() {
        for ((discovered, expected) in listOf(
            emptySet<String>() to "Bluebird, Parakeet, Macaw",
            setOf("MACAW") to "Bluebird, Parakeet",
            setOf("MACAW", "BLUEBIRD") to "Parakeet",
            setOf("MACAW", "BLUEBIRD", "PARAKEET") to "None")) {
            assertEquals(expected, SafariLookup.missingResult(otherDiscoveries + discovered, true))
        }
    }

    @Test fun `timesave mutual lists each shared bird even when others are not shared`() {
        for ((shared, expected) in listOf(
            emptySet<String>() to "None. :skull:",
            setOf("MACAW") to "Macaw",
            setOf("MACAW", "BLUEBIRD") to "Bluebird, Macaw",
            setOf("MACAW", "BLUEBIRD", "PARAKEET") to "Bluebird, Parakeet, Macaw")) {
            val result = SafariLookup.mutualResult(shared, true)
            assertEquals(expected, result)
            assertTrue(PartyCommand(PartyCommandKind.MUTUALS).matchesResponse("Mutual Timesave Shiny Critters: $result"))
        }
    }

    @Test fun `older all birds replies remain recognized for mixed version parties`() {
        assertTrue(PartyCommand(PartyCommandKind.MISSING, "Player")
            .matchesResponse("Missing Timesave Shinies for Player: All Birds"))
        assertTrue(PartyCommand(PartyCommandKind.MUTUALS)
            .matchesResponse("Mutual Timesave Shiny Critters: All Birds"))
    }
}
