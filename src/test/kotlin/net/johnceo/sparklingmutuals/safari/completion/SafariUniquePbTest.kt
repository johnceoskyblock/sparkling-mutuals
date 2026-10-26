package net.johnceo.sparklingmutuals.safari.completion

import java.util.Properties
import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.commands.PartyCommand
import net.johnceo.sparklingmutuals.commands.PartyCommandKind
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariUniquePbTest {
    @Test fun `observed unique records attribute solo and party captures and survive reload`() {
        val bests = SafariPersonalBests()
        for (biome in SafariBiome.entries) {
            val solo = SafariRun(1000)
            biome.critters.dropLast(1).forEach { solo.record(SafariCatch(it, false, "Friend")) }
            assertFalse(bests.recordObservedUnique(biome, solo, 3000))
            solo.record(SafariCatch(biome.critters.last(), false, "Friend"))
            assertTrue(bests.recordObservedUnique(biome, solo, 4000))
            assertFalse(bests.recordObservedUnique(biome, solo, 5000))
            assertTrue(bests.trackedResponse("friend", biome.label).endsWith("0:03.000"))
            val mixed = SafariRun(1000)
            biome.critters.forEachIndexed { index, critter ->
                mixed.record(SafariCatch(critter, index == 0, if (index == 0) null else "Friend"), self = "Me")
            }
            assertTrue(bests.recordObservedUnique(biome, mixed, 3500))
            assertTrue(bests.trackedResponse("Friend", biome.label, true).endsWith("0:02.500"))
            assertTrue(bests.trackedResponse("Me", biome.label, true).endsWith("0:02.500"))
            val query = PartyCommand.fromChat("Party > Me: !pb ${biome.label.lowercase()} Friend party")!!.command
            assertTrue(query.trackedPlayer); assertTrue(query.duo)
            assertTrue(query.matchesResponse(bests.trackedResponse("Friend", biome.label, true)))
        }
        val restored = SafariPersonalBests().apply { load(Properties().also(bests::save)) }
        assertEquals(bests.trackedResponse("Friend", "Forest"), restored.trackedResponse("friend", "Forest"))
        assertEquals(bests.trackedResponse("Friend", "Forest", true), restored.trackedResponse("friend", "Forest", true))
    }
    @Test fun `each biome requires every unique personally even if loot shares fill the display`() {
        for (biome in SafariBiome.entries) {
            val run = SafariRun(1000)
            val bests = SafariPersonalBests()
            biome.critters.forEach { run.record(SafariCatch(it, false)) }
            assertNull(bests.recordUnique(biome, run, 30000))
            biome.critters.dropLast(1).forEach { run.record(SafariCatch(it)) }
            assertNull(bests.recordUnique(biome, run, 40000))
            run.record(SafariCatch(biome.critters.last()))
            assertEquals("[SM] New ${biome.label} Unique Run PB: 0:44.000!", bests.recordUnique(biome, run, 45000))
            assertEquals(44000L, bests.time(biome.label, BiomePbType.UNIQUE))
            assertNull(bests.time(biome.label))
            assertNull(bests.recordUnique(biome, run, 46000))
        }
    }
    @Test fun `unique PB needs no full clear counts or structure scan but refuses ended runs`() {
        val run = SafariRun(1000)
        SafariBiome.CAVERN.critters.forEach { run.record(SafariCatch(it)) }
        assertFalse(SafariFullClear.eligible(run, SafariBiome.CAVERN, BiomeClearEvidence(observed = true)))
        val bests = SafariPersonalBests()
        assertNull(bests.recordUnique(SafariBiome.CAVERN, run, 999))
        assertTrue(run.uniqueBiomeClears.isEmpty())
        assertNotNull(bests.recordUnique(SafariBiome.CAVERN, run, 5000))
        val ended = SafariRun(1000).apply {
            SafariBiome.CAVERN.critters.forEach { record(SafariCatch(it)) }; endedAt = 4000
        }
        assertNull(bests.recordUnique(SafariBiome.CAVERN, ended, 5000))
    }
    @Test fun `full clear and unique PBs persist independently with the selected reply type`() {
        val bests = SafariPersonalBests()
        bests.load(Properties().apply { setProperty("pb.forestMillis", "90000") })
        assertEquals(BiomePbType.FULL_CLEAR, bests.biomeType)
        assertEquals("Player's Forest Full Clear PB: 1:30.000", bests.response("Player", "Forest"))
        val run = SafariRun(1000).apply { SafariBiome.FOREST.critters.forEach { record(SafariCatch(it)) } }
        assertNotNull(bests.recordUnique(SafariBiome.FOREST, run, 31000))
        assertEquals(BiomePbType.UNIQUE, bests.toggleBiomeType())
        assertEquals("Player's Forest Unique Run PB: 0:30.000", bests.response("Player", "Forest"))
        val saved = Properties().also(bests::save)
        val restored = SafariPersonalBests().also { it.load(saved) }
        assertEquals(BiomePbType.UNIQUE, restored.biomeType)
        assertEquals(90000L, restored.time("Forest"))
        assertEquals(30000L, restored.time("Forest", BiomePbType.UNIQUE))
        assertEquals(BiomePbType.FULL_CLEAR, restored.toggleBiomeType())
        assertEquals("Player's Forest Full Clear PB: 1:30.000", restored.response("Player", "Forest"))
    }
    @Test fun `typed biome replies stay scoped and creature PB replies keep their format`() {
        val command = PartyCommand(PartyCommandKind.PB_CAVERN).forResponder("Player")
        for (type in listOf("Full Clear", "Unique Run")) {
            assertTrue(command.matchesResponse("Player's Cavern $type PB: 1:02.250"))
            assertTrue(command.matchesResponse("Player's Cavern $type PB: Not recorded yet"))
            assertFalse(command.matchesResponse("Other's Cavern $type PB: 1:02.250"))
        }
        assertFalse(command.matchesResponse("Player's Cavern Fake PB: 1:02.250"))
        val bests = SafariPersonalBests()
        bests.toggleBiomeType()
        assertEquals("Player's Doomspiral PB: Not recorded yet", bests.response("Player", "Doomspiral"))
        assertTrue(CommandHelp.localLines().any { it.contains("/unique") })
        assertFalse(CommandHelp.localLines().any { it.contains("/sparkling fc") })
    }
}
