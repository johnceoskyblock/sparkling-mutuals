package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingRatesTest {
    @Test fun `Unique visits every biome and includes one shared manual species but all needed checks`() {
        val shared = SparklingRates.estimate(party(), mode = SafariMode.UNIQUE)
        assertEquals(SafariBiome.entries, shared.biomes)
        assertEquals(all, shared.boat.keys + shared.passive.keys + shared.manual.keys)
        assertTrue(shared.manual.values.all { it == 1.0 })
        assertEquals(1.0, shared.manual["Rockmite"])
        assertEquals(1.0, shared.manual["Snoozle"])
        assertEquals(1.0, shared.manual["Bluebird"])
        val needed = SparklingRates.estimate(party("Rockmite", "Macaw"), mode = SafariMode.UNIQUE)
        assertEquals(CritterSpawnAverages.lazyParty["Rockmite"], needed.manual["Rockmite"])
        for (bird in listOf("Bluebird", "Parakeet", "Macaw"))
            assertEquals(CritterSpawnAverages.lazyParty[bird], needed.manual[bird])
        assertTrue(needed.checkedPerRun > shared.checkedPerRun)
        assertTrue(SparklingRates.breakdown(shared).startsWith("Mode: Unique"))
    }
    @Test fun `Full Clear uses all full party spawns without discovery data or double counting boat species`() {
        val result = SparklingRates.estimate(PartySparklingState(), mode = SafariMode.FULL_CLEAR)
        assertEquals(all, result.boat.keys + result.passive.keys + result.manual.keys)
        assertEquals(SafariBiome.entries, result.biomes)
        assertTrue((result.boat.keys intersect result.manual.keys).isEmpty())
        assertTrue((result.boat.keys intersect result.passive.keys).isEmpty())
        assertEquals(CritterSpawnAverages.fullParty.values.sum(), result.checkedPerRun, 1e-8)
        assertEquals(149.435288032, result.checkedPerRun, 1e-8)
        assertEquals(CritterSpawnAverages.fullParty["Gimmiegold"], result.manual["Gimmiegold"])
        assertTrue(SparklingRates.breakdown(result).contains("Full Party spawn averages"))
    }
    private val all = SafariRoster.all.map { it.name }.toSet()
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("me", "friend"))
        accept(setOf("me", "friend"), mapOf("me" to all, "friend" to all - missing.toSet()))
    }

    @Test fun `all checks match the spreadsheet baseline and boat species are counted once`() {
        val result = SparklingRates.estimate(party(*all.toTypedArray()))
        assertEquals(SafariBiome.entries, result.biomes)
        assertEquals(all, result.boat.keys + result.passive.keys + result.manual.keys)
        assertTrue((result.boat.keys intersect result.passive.keys).isEmpty())
        assertTrue((result.passive.keys intersect result.manual.keys).isEmpty())
        assertEquals(144.994135774, result.checkedPerRun, 1e-8)
        assertEquals(14.1247091758, result.runsPerSparkling!!, 1e-8)
        assertEquals("[SM] 144.99 critters checked per run. Your party should find a sparkling every 14.12 runs.", SparklingRates.message(result))
    }

    @Test fun `shared biomes and manual sources disappear but boat skins remain even when shared`() {
        val result = SparklingRates.estimate(party("Doomspiral"))
        assertEquals(listOf(SafariBiome.HAUNTED), result.biomes)
        assertEquals(setOf("Doomspiral"), result.manual.keys)
        assertEquals(setOf("Areita", "Bloodbat", "Duplico", "Litterbug", "Solsnatcher", "Hideonwall", "Hideyho"), result.passive.keys)
        assertEquals(setOf("Flitter", "Shyworm", "Driftling", "Chuckwalla", "Mantis Shrimp", "Troodon"), result.boat.keys)
        assertEquals(55.5, result.checkedPerRun, 1e-10)
        assertTrue(SparklingRates.breakdown(result).contains("Manual checks / spawns"))
    }

    @Test fun `needing any bird includes all random bird spawns but shared birds require no spawning`() {
        val birds = setOf("Bluebird", "Parakeet", "Macaw")
        val macaw = SparklingRates.estimate(party("Macaw"))
        assertEquals(listOf(SafariBiome.FOREST), macaw.biomes)
        assertEquals(birds, macaw.manual.keys)
        assertEquals(58.146913552, macaw.checkedPerRun, 1e-9)
        val honeybug = SparklingRates.estimate(party("Honeybug"))
        assertEquals(setOf("Honeybug"), honeybug.manual.keys)
        assertTrue(honeybug.passive.keys.intersect(birds).isEmpty())
        assertEquals(52.0, honeybug.checkedPerRun, 1e-10)
    }

    @Test fun `only boat visible needs do not require biome entry and missing data is not guessed`() {
        for (needed in SparklingRates.boatDetected + "") {
            val result = if (needed.isEmpty()) SparklingRates.estimate(party()) else SparklingRates.estimate(party(needed))
            assertTrue(result.biomes.isEmpty())
            assertTrue(result.passive.isEmpty()); assertTrue(result.manual.isEmpty())
            assertEquals(27.5, result.checkedPerRun, 1e-10)
            assertTrue(SparklingRates.message(result).endsWith("74.47 runs."))
        }
        assertThrows(IllegalArgumentException::class.java) { SparklingRates.estimate(PartySparklingState()) }
    }
}
