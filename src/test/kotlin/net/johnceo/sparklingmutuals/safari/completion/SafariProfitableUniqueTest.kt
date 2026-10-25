package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.resources.FloorDropState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariProfitableUniqueTest : net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture() {
    @Test fun `ESP matches the command top ten and refreshes for prices and sell preference without a fetch`() {
        val ranking = net.johnceo.sparklingmutuals.commands.ProfitableCritters
        val quote = net.johnceo.sparklingmutuals.history.BazaarPrices
        prices(SafariRoster.all.associate { quote.shardId(it.name) to when (it.name) {
            "Scrappy", "Rockmite", "Troodon" -> doubleArrayOf(1e12, 1e12)
            "Wumpa" -> doubleArrayOf(1e9, 1.0)
            "Nozzlenose" -> doubleArrayOf(1.0, 1e9)
            else -> doubleArrayOf(100.0, 100.0)
        } })
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
        }
        for (instaSell in listOf(false, true)) {
            ConfigManager.runCoinsInstaSell = instaSell
            val top = ranking.cachedTop()
            assertEquals(if (instaSell) "Wumpa" else "Nozzlenose", top.first().name)
            assertEquals(10, top.size)
            assertSame(top, ranking.cachedTop())
            val expected = top.map { it.name }.toSet()
            for (critter in SafariRoster.all) {
                val run = SafariRun(0).apply { record(SafariCatch(critter)) }
                assertEquals(critter.name in expected, SafariEspRules.neededForRun(critter.name, false, run, false, true), critter.name)
                assertEquals(critter.name in expected, SafariEspRules.neededForSparkling(critter.name, false, party, true, run, java.util.UUID.randomUUID(), 0), critter.name)
                assertFalse(SafariEspRules.profitableEsp(critter.name, false))
            }
        }
        prices(emptyMap())
        assertTrue(ranking.cachedTop().isEmpty())
        assertFalse(SafariEspRules.profitableEsp("Wumpa", true))
    }
    @TempDir lateinit var dir: Path
    @Test fun `Unique profitable toggle keeps only the top ten profitable species after first capture`() {
        val profitable = net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture.profitable
        for (critter in SafariRoster.all) {
            val run = SafariRun(0)
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            run.record(SafariCatch(critter, false))
            assertEquals(critter.name in profitable,
                SafariEspRules.neededForRun(critter.name, false, run, false, true), critter.name)
            assertFalse(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, true, false))
        }
    }
    @Test fun `profitable option defaults on and Modes subsetting persists without changing full clear`() {
        ConfigManager.init(dir)
        assertTrue(ConfigManager.profitableShardEsp)
        val settings = SafariSettings()
        settings.modes.unique.profitable = false
        settings.apply(); ConfigManager.init(dir)
        assertFalse(ConfigManager.profitableShardEsp)
        SafariFullClear.setEnabled(true); SafariFullClear.setEnabled(false)
        assertTrue(SafariEspConfig.mobs.getValue("Chuckwalla").enabled)
        assertEquals("[SM] Unique run mode on.", SafariFullClear.modeMessage())
        SafariFullClear.setEnabled(true)
        assertEquals("[SM] Full clear mode on.", SafariFullClear.modeMessage())
    }
    @Test fun `profitable highlights survive label and motion gaps only for current entities`() {
        for (sparkling in listOf(false, true)) for (name in net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture.profitable) {
            assertTrue(SafariEspRules.renderCurrent(name, sparkling, true, false, false, true))
            assertFalse(SafariEspRules.renderCurrent(name, sparkling, false, true, true, true))
        }
        assertFalse(SafariEspRules.renderCurrent("Hideonwall", true, true, false, false, false))
        assertFalse(SafariEspRules.renderCurrent("Flitter", true, true, false, false, true))
    }
    @Test fun `profitable Sparkling toggle preserves all top ten species after catches and checks`() {
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
        }
        for (name in net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture.profitable) {
            val run = SafariRun(0); val id = java.util.UUID.randomUUID()
            run.record(SafariCatch(SafariRoster.named(name)!!))
            assertTrue(SafariEspRules.neededForSparkling(name, false, party, true, run, id, 0))
            assertTrue(SafariEspRules.neededForSparkling(name, false, party, true, run, id, 60000))
            assertFalse(SafariEspRules.neededForSparkling(name, false, party, false, run, id, 60000))
        }
    }
    @Test fun `nine Forest pickups hide unique drops for run while starting food alone cannot`() {
        val run = SafariRun(0); val floor = FloorDropState()
        repeat(2) { run.recordBirdFood("FLOOR DROP! Bag of Seeds", null) }
        assertTrue(floor.enabled(SafariBiome.FOREST, false, true, run.birdFoodsComplete))
        repeat(3) { run.recordBirdFood("FLOOR DROP! Bag of Seeds"); run.recordBirdFood("FLOOR DROP! Wriggleworm") }
        repeat(2) { run.recordBirdFood("FLOOR DROP! Yogi Berry") }
        assertTrue(floor.enabled(SafariBiome.FOREST, false, true, run.birdFoodsComplete))
        run.recordBirdFood("FLOOR DROP! Yogi Berry")
        assertFalse(floor.enabled(SafariBiome.FOREST, false, true, run.birdFoodsComplete))
        floor.visit(null); floor.visit(SafariBiome.FOREST)
        assertFalse(floor.enabled(SafariBiome.FOREST, false, true, run.birdFoodsComplete))
        assertTrue(floor.enabled(SafariBiome.FOREST, true, true, run.birdFoodsComplete))
        assertTrue(floor.enabled(SafariBiome.HAUNTED, false, true, run.birdFoodsComplete))
        assertTrue(floor.enabled(SafariBiome.FOREST, false, true, SafariRun(1000).birdFoodsComplete))
    }
}
