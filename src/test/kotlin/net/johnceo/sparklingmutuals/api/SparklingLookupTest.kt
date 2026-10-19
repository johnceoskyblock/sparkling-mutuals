package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.commands.SparklingLookup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingLookupTest {
    @Test fun `discovery fallback accepts absent profile data but preserves actual failures`() {
        val lookup = net.johnceo.sparklingmutuals.commands.SafariLookup
        assertTrue(lookup.discoveriesOrEmpty { throw ApiFailure.dataUnavailable() }.isEmpty())
        assertEquals(setOf("GAZER"), lookup.discoveriesOrEmpty { setOf("GAZER") })
        for (failure in listOf(ApiFailure.unavailable(), ApiFailure.malformedData(),
            ApiFailure(ApiFailureKind.INVALID_KEY, "Invalid key"), ApiFailure(ApiFailureKind.RATE_LIMIT, "Rate limited"))) {
            assertSame(failure, assertThrows(ApiFailure::class.java) { lookup.discoveriesOrEmpty { throw failure } })
        }
    }
    @Test fun `valid members with no Safari or discovery fields have zero sparklings and need all species`() {
        for (json in listOf("{}", """{"safari":null}""", """{"safari":{}}""",
            """{"safari":{"tickets":{"basic":2}}}""", """{"safari":{"discovered_sparkling_critters":null}}""")) {
            val member = JsonParser.parseString(json).asJsonObject
            val profile = SafariProfile.from(member)
            assertTrue(SafariProfile.discoveriesFrom(member).isEmpty())
            assertTrue(profile.caught.isEmpty())
            assertEquals(37, profile.missing.size)
            assertEquals(0L, profile.totalCaptured)
            val state = net.johnceo.sparklingmutuals.safari.PartySparklingState()
            val roster = setOf("experienced", "new-player")
            state.select(roster)
            assertTrue(state.accept(roster, mapOf("experienced" to setOf("GAZER"), "new-player" to profile.discoveries)))
            assertTrue(state.ready, "An empty discovery set is successful data, not a permanent loading state")
            assertTrue(net.johnceo.sparklingmutuals.safari.SafariRoster.all.all { state.needs(it.name) })
        }
    }
    @Test fun `malformed discovery data remains an API failure instead of silently becoming zero catches`() {
        for (json in listOf("""{"safari":false}""", """{"safari":{"discovered_sparkling_critters":{}}}""",
            """{"safari":{"discovered_sparkling_critters":[null]}}""")) {
            assertEquals(ApiFailureKind.MALFORMED_DATA,
                assertThrows(ApiFailure::class.java) { SafariProfile.from(JsonParser.parseString(json).asJsonObject) }.kind)
        }
    }
    @Test fun `profile reads discoveries tickets essence and hunting xp`() {
        val member = JsonParser.parseString("""{"safari":{"discovered_sparkling_critters":["MANTIS_SHRIMP","GEMZIE"],
            "tickets":{"basic":162,"economy":4},"total_captured_sparkling_critters":30},
            "player_data":{"experience":{"SKILL_HUNTING":175.0}},"currencies":{"essence":{"SAFARI":{"current":12212}}}}""").asJsonObject
        val profile = SafariProfile.from(member)
        assertEquals(listOf("Gemzie", "Mantis Shrimp"), profile.caught)
        assertEquals(35, profile.missing.size)
        assertEquals(mapOf("basic" to 162L, "economy" to 4L, "premium" to 0L, "first_class" to 0L), profile.tickets)
        assertEquals(12212L, profile.essence)
        assertEquals(30L, profile.totalCaptured)
        assertEquals(2.0, profile.huntingLevel!!, 1e-9)
    }
    @Test fun `the shared API's response fills the whole card`() {
        // Produced by Shiny Hunter's Worker trimming code; the cloud serves only the selected profile's member.
        val body = JsonParser.parseString(javaClass.getResource("/cloud/profile.json")!!.readText()).asJsonObject
        val member = body.getAsJsonArray("profiles")[0].asJsonObject.getAsJsonObject("members")
            .getAsJsonObject("0123456789abcdef0123456789abcdef")
        val profile = SafariProfile.from(member)
        assertEquals(setOf("SOLSNATCHER", "GAZER"), profile.discoveries)
        assertEquals(39L, profile.tickets["basic"])
        assertEquals(12212L, profile.essence)
        assertEquals(3150000.5, profile.huntingXp)
        assertNotNull(profile.huntingLevel)
    }
    @Test fun `stats Hypixel leaves out show as unknown instead of zero`() {
        val member = JsonParser.parseString("""{"safari":{"discovered_sparkling_critters":[]},
            "player_data":{"experience":{}},"currencies":{"essence":{}}}""").asJsonObject
        val profile = SafariProfile.from(member)
        assertNull(profile.huntingXp)
        assertNull(profile.huntingLevel)
        assertNull(profile.essence)
    }
    @Test fun `hunting levels follow the skill table and overflow past sixty`() {
        assertEquals(0.5, SafariProfile.levelFromXp(25.0), 1e-9)
        assertEquals(60.0, SafariProfile.levelFromXp(111_672_425.0), 1e-9)
        assertEquals(61.0, SafariProfile.levelFromXp(111_672_425.0 + 7_600_000), 1e-9)
    }
    @Test fun `columns group by biome from most common to rarest`() {
        val columns = SparklingLookup.columns(listOf("Macaw", "Bluebird", "Foxtrot", "Parakeet", "Wumpa", "Gemzie"))
        assertEquals(listOf("Foxtrot", "Bluebird", "Parakeet", "Macaw"), columns[0])
        assertEquals(listOf("Gemzie"), columns[1])
        assertEquals(listOf("Wumpa"), columns[2])
        assertTrue(columns[3].isEmpty())
    }
    @Test fun `shared lists only what everyone has`() {
        assertEquals(listOf("Bluebird", "Gemzie"),
            SparklingLookup.shared(listOf(listOf("Gemzie", "Bluebird", "Macaw"), listOf("Bluebird", "Gemzie"))))
    }
}
