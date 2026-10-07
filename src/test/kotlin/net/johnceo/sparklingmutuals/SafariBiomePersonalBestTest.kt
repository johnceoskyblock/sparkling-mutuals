package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class SafariBiomePersonalBestTest {
    @Test fun `biome bests improve independently and survive reload`() {
        val bests = SafariPersonalBests()
        SafariBiome.entries.forEachIndexed { index, biome ->
            val now = 62000L + index
            assertEquals("[SM] New ${biome.label} PB: 1:01.${index.toString().padStart(3, '0')}!", bests.recordBiome(biome, SafariRun(1000), now))
            assertNull(bests.recordBiome(biome, SafariRun(1000), now))
            assertNull(bests.recordBiome(biome, SafariRun(1000), now + 1))
        }
        val properties = Properties().also(bests::save)
        val restored = SafariPersonalBests().also { it.load(properties) }
        SafariBiome.entries.forEach { assertEquals(bests.time(it.label), restored.time(it.label)) }
        assertNull(bests.recordBiome(SafariBiome.FOREST, SafariRun(1000), 999))
        assertNull(bests.recordBiome(SafariBiome.FOREST, SafariRun(1000).also { it.endedAt = 1500 }, 2000))
    }

    @Test fun `biome replies are scoped to each responder`() {
        SafariBiome.entries.forEach { biome ->
            val command = PartyCommand.fromChat("Party > Test: !PB ${biome.label.uppercase()}")!!.command.forResponder("Player")
            assertTrue(command.matchesResponse("Player's ${biome.label} PB: 1:02.250"))
            assertFalse(command.matchesResponse("Other's ${biome.label} PB: 1:02.250"))
            assertTrue(command.matchesResponse("Player's ${biome.label} PB: Not recorded yet"))
        }
    }

    @Test fun `lookup formatting switches between timesaves and all species`() {
        assertEquals(listOf("All Birds"), SafariLookup.formatDiscoveries(setOf("BLUEBIRD", "PARAKEET", "MACAW"), true))
        assertEquals(listOf("Bluebird", "Parakeet", "Macaw"), SafariLookup.formatDiscoveries(setOf("BLUEBIRD", "PARAKEET", "MACAW"), false))
        assertEquals(37, SafariLookup.formatMissing(emptySet(), false).size)
        val command = PartyCommand(PartyCommandKind.MISSING, "Player")
        assertTrue(command.matchesResponse("Missing Sparklings for Player: Mantis Shrimp, Foxtrot"))
        assertFalse(command.matchesResponse("Missing Sparklings for Player: lookup failed"))
        assertTrue(PartyCommand(PartyCommandKind.MUTUALS).matchesResponse("Mutual Sparkling Critters: Flitter, Cavernfish"))
    }
    @Test fun `all species chunks fit party chat and preserve every species`() {
        for (prefix in listOf("Missing Sparklings for LongPlayerName16: ", "Mutual Sparkling Critters: ")) {
            val names = SafariLookup.formatMissing(emptySet(), false)
            val chunks = PartyReplyChunks.split(prefix + names.joinToString(", "))
            assertTrue(chunks.size > 1)
            assertTrue(chunks.all { it.length + 4 <= 256 })
            assertEquals(names, chunks.flatMap { it.substringAfter(": ").split(", ") })
            val command = if (prefix.startsWith("Missing")) PartyCommand(PartyCommandKind.MISSING, "LongPlayerName16") else PartyCommand(PartyCommandKind.MUTUALS)
            chunks.forEach { assertTrue(command.matchesResponse(it), it) }
            assertFalse(command.matchesResponse(chunks[0].substringBefore(": ") + ": Lookup failed"))
            assertFalse(command.matchesResponse(chunks[0].replace("[1/", "[0/")))
        }
        val short = "Mutual Timesave Sparkling Critters: Honeybug"
        assertEquals(listOf(short), PartyReplyChunks.split(short))
    }

}
