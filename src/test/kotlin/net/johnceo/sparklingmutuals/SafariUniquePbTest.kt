package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class SafariUniquePbTest {
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
        assertTrue(CommandHelp.localLines().any { it.contains("/sparkling fc") })
    }
}
