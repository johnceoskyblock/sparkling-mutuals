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
    @Test fun `local zero-spawn Cavern survey completes Unique rows and reports without fabricating catches`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.CAVERN)
        SafariBiome.CAVERN.critters.filter { it.name !in setOf("Rockmite", "Snoozle") }.forEach { run.record(SafariCatch(it)) }
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.moundSurvey.scan(emptySet()) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        run.reconcileStructures(SafariBiome.CAVERN, false, false)
        assertTrue(run.uniqueComplete("Rockmite", state, localOnly = true))
        assertTrue(run.uniqueComplete("Snoozle", state, localOnly = true))
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        assertEquals("cd", run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, party = state).rows.any { it.label in setOf("Rockmite", "Snoozle") })
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.reconcileStructures(SafariBiome.CAVERN, false, false)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        assertTrue(run.uniqueComplete("Snoozle", state), "completed local absence proof survives leaving the biome")
        run.observeCritter(12, "Rockmite"); run.observeCritter(13, "Snoozle")
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        assertEquals(0, run.personalCount("Rockmite")); assertEquals(0, run.personalCount("Snoozle"))
        val unknown = SafariRun(1)
        unknown.reconcileStructures(SafariBiome.CAVERN, false, false)
        assertFalse(unknown.uniqueComplete("Rockmite", state)); assertFalse(unknown.uniqueComplete("Snoozle", state))
    }
    @Test fun `no Macaw local Forest completion includes all reported starting food before fd or row removal`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.FOREST)
        run.localPlayer("Me")
        run.inventoryEvidence(emptyList(), 10000)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, allNestsChecked = true)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { run.record(SafariCatch(it)) }
        run.resources.partyNotice("Party > Other: [SM] Starting items: 2x Bag of Seeds")
        run.scanBirdSources(0, List(7) { "Bluebird" })
        assertEquals(11, run.birds.feedingTarget)
        assertFalse(run.uniqueComplete("Macaw", state))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.inventoryEvidence(listOf("Bag of Seeds" to 1), 20000)
        run.scanBirdSources(0, List(9) { "Bluebird" })
        assertFalse(run.uniqueComplete("Macaw", state), "held food must be used even when population is sufficient")
        run.inventoryEvidence(emptyList(), 21000)
        run.scanBirdSources(1, List(9) { "Bluebird" })
        assertFalse(run.uniqueComplete("Macaw", state), "remaining floor food must also be used")
        run.scanBirdSources(0, List(9) { "Bluebird" })
        assertTrue(run.uniqueComplete("Macaw", state, localOnly = true))
        assertEquals(0, run.count("Macaw"))
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Macaw" })
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.observeCritter(42, "Macaw")
        assertFalse(run.uniqueComplete("Macaw", state), "a newly seen Macaw revokes zero-spawn completion")
    }
    @Test fun `any bird may have zero spawns after all feeding opportunities including party extras resolve`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        for (absent in listOf("Bluebird", "Parakeet", "Macaw")) {
            val run = SafariRun(0); val state = party()
            val spawned = if (absent == "Bluebird") "Parakeet" else "Bluebird"
            run.visitBiome(SafariBiome.FOREST)
            run.localPlayer("Me")
            run.resources.partyNotice("Party > Other: [SM] Starting items: 1x Bag of Seeds")
            run.inventoryEvidence(emptyList(), 10000)
            run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, allNestsChecked = true)
            SafariBiome.FOREST.critters.filter { it.name !in setOf("Bluebird", "Parakeet", "Macaw") }
                .forEach { run.record(SafariCatch(it)) }
            run.record(SafariCatch(SafariRoster.named(spawned)!!))
            run.scanBirdSources(0, List(8) { spawned })
            assertFalse(run.uniqueUnavailable(absent), "the party's tenth food has not been used")
            run.inventoryEvidence(listOf("Bag of Seeds" to 1), 20000)
            run.scanBirdSources(0, List(9) { spawned })
            assertFalse(run.uniqueComplete(absent, state), "held food can still spawn the missing bird")
            run.inventoryEvidence(emptyList(), 21000)
            run.scanBirdSources(1, List(9) { spawned })
            assertFalse(run.uniqueComplete(absent, state), "remaining floor food prevents absence proof")
            run.scanBirdSources(0, List(9) { spawned })
            assertTrue(run.uniqueUnavailable(absent), absent)
            assertTrue(run.uniqueComplete(absent, state, localOnly = true), absent)
            assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == absent })
            assertFalse(SafariEspRules.neededForRun(absent, false, run, false, false, state))
            assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
            assertFalse(run.captureComplete(absent), "Full Clear still needs the spawned birds caught")
            repeat(9) { run.record(SafariCatch(SafariRoster.named(spawned)!!)) }
            run.scanBirdSources(0, emptyList())
            for (bird in listOf("Bluebird", "Parakeet", "Macaw")) {
                assertTrue(run.captureComplete(bird), "Full Clear permits zero $absent after catching the actual population")
                assertTrue(run.fullClearMissingComplete(bird))
            }
            assertEquals(0, run.count(absent))
            assertEquals(0, run.personalCount(absent))
            run.observeCritter(4242, absent)
            assertFalse(run.uniqueUnavailable(absent), "a later sighting revokes zero-spawn proof")
        }
    }
    @Test fun `party fd cannot complete Unique Forest without catches`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.CAVERN)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { run.record(SafariCatch(it)) }
        run.partyFullClear("Party > Friend: fd")
        run.visitBiome(SafariBiome.FOREST)
        assertEquals(5, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state), "Forest entry requires local nest and bird spawning proof")
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
