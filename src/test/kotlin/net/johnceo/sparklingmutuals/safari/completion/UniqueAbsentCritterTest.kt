package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniqueAbsentCritterTest {
    private fun party() = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to emptySet()))
    }
    @Test fun `checked zero-spawn Cavern species still require Unique catches`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.CAVERN)
        SafariBiome.CAVERN.critters.filter { it.name !in setOf("Rockmite", "Snoozle") }.forEach { run.record(SafariCatch(it)) }
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.moundSurvey.scan(emptySet()) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        run.reconcileStructures(SafariBiome.CAVERN, false, false)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
        assertTrue(SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, party = state).rows.any { it.label in setOf("Rockmite", "Snoozle") })
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.reconcileStructures(SafariBiome.CAVERN, false, false)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertFalse(run.uniqueComplete("Snoozle", state))
        run.observeCritter(12, "Rockmite"); run.observeCritter(13, "Snoozle")
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        assertEquals(0, run.personalCount("Rockmite")); assertEquals(0, run.personalCount("Snoozle"))
        val unknown = SafariRun(1)
        unknown.reconcileStructures(SafariBiome.CAVERN, false, false)
        assertFalse(unknown.uniqueComplete("Rockmite", state)); assertFalse(unknown.uniqueComplete("Snoozle", state))
    }
    @Test fun `party fd cannot complete Unique Forest without catches`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.CAVERN)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { run.record(SafariCatch(it)) }
        run.partyFullClear("Party > Friend: fd")
        run.visitBiome(SafariBiome.FOREST)
        assertEquals(6, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state), "Forest entry requires local bird spawning proof")
        assertEquals(0, run.count("Macaw")); assertEquals(0, run.personalCount("Macaw"))
        assertFalse(run.uniqueComplete("Macaw", state, localOnly = true))
    }
    @Test fun `caught Unique entities hide with profitable highlighting disabled while structure helpers stay`() {
        val run = SafariRun(0); val state = party()
        for (critter in SafariRoster.all) {
            assertTrue(SafariEspRules.neededForRun(critter.name, false, run, false, false, state))
            run.record(SafariCatch(critter))
            assertFalse(SafariEspRules.neededForRun(critter.name, false, run, false, false, state))
        }
        assertTrue(SafariEspRules.neededForRun("Rockmite", true, run, false, true, state))
        assertTrue(SafariHelperRules.needed("Snoozle", SafariMode.UNIQUE, run, state))
        assertTrue(SafariHelperRules.showNests(SafariMode.UNIQUE, run, state), "a needed Sparkling Honeybug keeps its independent nest work")
    }
}
