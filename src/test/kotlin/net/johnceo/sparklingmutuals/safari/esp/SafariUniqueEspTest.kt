package net.johnceo.sparklingmutuals.safari.esp

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.BiomePbType
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariUniqueEspTest {
    @TempDir lateinit var dir: Path
    @Test fun `unique ESP follows missing species across every biome and resets next run`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        val party = PartySparklingState().apply {
            select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet()))
        }
        for (critter in SafariRoster.all) {
            run.visitBiome(critter.biome)
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            run.record(SafariCatch(critter))
            assertFalse(SafariPanels.missing(run, critter.biome, true, 0, party = party).rows.any { it.label == critter.name })
            assertFalse(SafariEspRules.neededForRun(critter.name, false, run, false, false))
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, true))
            assertTrue(SafariEspRules.neededForRun(critter.name, false, SafariRun(1000), false, false))
        }
    }
    @Test fun `unique captures hide Rockmite models and mounds without changing ESP settings`() {
        ConfigManager.init(dir)
        SafariFullClear.toggle(); SafariFullClear.toggle()
        val run = SafariRun(0)
        run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
        run.record(SafariCatch(SafariRoster.named("Snoozle")!!))
        run.record(SafariCatch(SafariRoster.named("Foxtrot")!!, false))
        assertFalse(SafariEspRules.neededForRun("Rockmite", true, run, false, false))
        assertFalse(SafariEspRules.neededForRun("Rockmite", false, run, false, false))
        assertFalse(SafariEspRules.neededForRun("Snoozle", false, run, false, false))
        assertFalse(SafariEspRules.neededForRun("Foxtrot", false, run, false, false))
        assertTrue(SafariEspConfig.rockmiteMoundEnabled)
        assertTrue(ConfigManager.highlightSnooperWalls)
        assertTrue(SafariEspConfig.mobs.getValue("Foxtrot").enabled)
        assertTrue(SafariEspRules.neededForRun("Foxtrot", false, SafariRun(2000), false, false))
    }
    @Test fun `loading old mismatched settings aligns PB and lookups to the run mode`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "fullClearMode=false\ntimesaveOnly=false\npb.biomeType=FULL_CLEAR\npb.forestMillis=90000\n")
        ConfigManager.init(dir)
        assertTrue(ConfigManager.timesaveOnly)
        assertEquals(BiomePbType.UNIQUE, ConfigManager.personalBests.biomeType)
        assertEquals(90000L, ConfigManager.personalBests.time("Forest"))
        assertTrue(SafariFullClear.toggle())
        assertFalse(ConfigManager.timesaveOnly)
        assertEquals(BiomePbType.FULL_CLEAR, ConfigManager.personalBests.biomeType)
        ConfigManager.init(dir)
        assertTrue(ConfigManager.fullClearMode)
        assertFalse(ConfigManager.timesaveOnly)
        assertEquals(BiomePbType.FULL_CLEAR, ConfigManager.personalBests.biomeType)
    }
}
