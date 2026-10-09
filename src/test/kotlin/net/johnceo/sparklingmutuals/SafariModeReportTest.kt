package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariModeReportTest {
    @TempDir lateinit var dir: Path
    @Test fun `unique preset shows every uncaught species even with all party discoveries`() {
        ConfigManager.init(dir)
        SafariFullClear.select(SafariMode.FULL_CLEAR)
        SafariFullClear.select(SafariMode.UNIQUE)
        val party = PartySparklingState().apply {
            select(setOf("me"))
            accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name.uppercase().replace(' ', '_') }.toSet()))
        }
        val run = SafariRun(0)
        for (critter in SafariRoster.all) {
            assertTrue(SafariEspConfig.entityEnabled(critter.name), critter.name)
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, false, false, party), critter.name)
            run.record(SafariCatch(critter))
            assertFalse(SafariEspRules.neededForRun(critter.name, false, run, false, false, party), critter.name)
        }
    }
    @Test fun `full clear cavern waits for three gems held together rather than discoveries or pickup messages`() {
        val floor = FloorDropState()
        for (gem in listOf("Purple Gem", "Lime Gem", "Orange Gem")) {
            floor.record("FLOOR DROP! $gem", SafariBiome.CAVERN)
            floor.inventory(listOf(gem to 1))
        }
        assertTrue(floor.enabled(SafariBiome.CAVERN, true, true, partyGemzieComplete = true, gemzieCaught = true))
        floor.inventory(listOf("Purple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 0))
        assertTrue(floor.enabled(SafariBiome.CAVERN, true, true))
        floor.inventory(listOf("\u00a7aPurple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 1))
        assertFalse(floor.enabled(SafariBiome.CAVERN, true, true))
        floor.inventory(emptyList())
        assertFalse(floor.enabled(SafariBiome.CAVERN, true, true))
        floor.reset()
        assertTrue(floor.enabled(SafariBiome.CAVERN, true, true))
    }
    @Test fun `full clear missing species stays until quota and absence are both confirmed`() {
        val run = SafariRun(0)
        val critter = SafariRoster.named("Driftling")!!
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        repeat(2) { run.record(SafariCatch(critter)) }
        assertTrue(run.missing(critter, true))
        run.record(SafariCatch(critter))
        assertFalse(run.missing(critter, true))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertTrue(run.missing(critter, true))
    }
}
