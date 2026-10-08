package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariProfitableUniqueTest {
    @TempDir lateinit var dir: Path
    @Test fun `profitable species keep unique ESP after first capture only while option is enabled`() {
        val profitable = setOf("Hideonfloor", "Hideonwall", "Chuckwalla", "Fluffling", "Mantis Shrimp")
        for (critter in SafariRoster.all) {
            val run = SafariRun(0)
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            run.record(SafariCatch(critter, false))
            assertEquals(critter.name in profitable, SafariEspRules.neededForRun(critter.name, false, run, false, true))
            assertFalse(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, true, false))
        }
    }
    @Test fun `profitable option defaults on and General subsetting persists without changing full clear`() {
        ConfigManager.init(dir)
        assertTrue(ConfigManager.profitableShardEsp)
        val settings = SafariSettings()
        settings.general.unique.profitable = false
        settings.apply(); ConfigManager.init(dir)
        assertFalse(ConfigManager.profitableShardEsp)
        SafariFullClear.setEnabled(true); SafariFullClear.setEnabled(false)
        assertTrue(SafariEspConfig.mobs.getValue("Chuckwalla").enabled)
        assertEquals("[SM] Unique run mode on.", SafariFullClear.modeMessage())
        SafariFullClear.setEnabled(true)
        assertEquals("[SM] Full clear mode on.", SafariFullClear.modeMessage())
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
