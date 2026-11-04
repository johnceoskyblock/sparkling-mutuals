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
    private fun qualifiedChecks(run: SafariRun, biome: SafariBiome) {
        if (biome == SafariBiome.CAVERN) {
            val inputs = listOf(
                Triple("Cavernfish", -85.0 to 81.0, 3), Triple("Flitter", -84.0 to 62.0, 5),
                Triple("Shyworm", -119.0 to 43.0, 3), Triple("Driftling", -120.0 to 55.0, 2),
                Triple("Chuckwalla", -100.0 to 45.0, 1), Triple("Gemzie", -141.0 to 51.0, 2),
                Triple("Scrappy", -114.0 to 49.0, 2))
            for ((name, point, extra) in inputs)
                run.sparklingChecks.scan(biome, point.first, point.second,
                    List(extra) { name to java.util.UUID.randomUUID() }, moundsCleared = true, wallsCleared = true)
        } else {
            run.sparklingChecks.scan(biome, 3.0, 47.0,
                listOf("Foxtrot" to 5, "Treefrog" to 2, "Woodchucker" to 2).flatMap { (name, extra) ->
                    List(extra) { name to java.util.UUID.randomUUID() }
                }, nestsChecked = true, birdsSpawned = true)
        }
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
        assertFalse(run.completion.allNeededChecked(run, SafariBiome.CAVERN, state, localOnly = true))
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
        qualifiedChecks(run, SafariBiome.CAVERN)
        assertTrue(run.completion.allNeededChecked(run, SafariBiome.CAVERN, state, localOnly = true))
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
    @Test fun `no Macaw local Forest completion includes own starting food before fd or row removal`() = net.johnceo.sparklingmutuals.config.withStartingItems {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.FOREST)
        run.localPlayer("Me")
        run.inventoryEvidence(emptyList(), 10000)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, allNestsChecked = true)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { run.record(SafariCatch(it)) }
        run.birds.startingInventory(listOf("Bag of Seeds" to 2), 0)
        run.scanBirdSources(0, List(7) { "Bluebird" })
        assertEquals(11, run.birds.feedingTarget)
        assertFalse(run.uniqueComplete("Macaw", state))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.inventoryEvidence(listOf("Bag of Seeds" to 1), 20000)
        run.scanBirdSources(0, List(9) { "Bluebird" })
        assertFalse(run.uniqueComplete("Macaw", state), "held food must be used even when population is sufficient")
        run.inventoryEvidence(emptyList(), 21000)
        run.birdFoodCheck.spawn("A Bluebird was attracted to the Birdfeeder!", 21001)
        run.scanBirdSources(1, List(9) { "Bluebird" })
        assertFalse(run.uniqueComplete("Macaw", state), "remaining floor food must also be used")
        run.scanBirdSources(0, List(9) { "Bluebird" })
        assertTrue(run.uniqueComplete("Macaw", state, localOnly = true))
        assertEquals(0, run.count("Macaw"))
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Macaw" })
        assertFalse(run.completion.allNeededChecked(run, SafariBiome.FOREST, state, localOnly = true))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        qualifiedChecks(run, SafariBiome.FOREST)
        assertTrue(run.completion.allNeededChecked(run, SafariBiome.FOREST, state, localOnly = true))
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        run.observeCritter(42, "Macaw")
        assertFalse(run.uniqueComplete("Macaw", state), "a newly seen Macaw revokes zero-spawn completion")
    }
    @Test fun `any bird may have zero spawns after all feeding opportunities including own extras resolve`() = net.johnceo.sparklingmutuals.config.withStartingItems {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        for (absent in listOf("Bluebird", "Parakeet", "Macaw")) {
            val run = SafariRun(0); val state = party()
            val spawned = if (absent == "Bluebird") "Parakeet" else "Bluebird"
            run.visitBiome(SafariBiome.FOREST)
            run.localPlayer("Me")
            run.birds.startingInventory(listOf("Bag of Seeds" to 1), 0)
            run.inventoryEvidence(emptyList(), 10000)
            run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, allNestsChecked = true)
            SafariBiome.FOREST.critters.filter { it.name !in setOf("Bluebird", "Parakeet", "Macaw") }
                .forEach { run.record(SafariCatch(it)) }
            run.record(SafariCatch(SafariRoster.named(spawned)!!))
            run.scanBirdSources(0, List(8) { spawned })
            assertFalse(run.uniqueUnavailable(absent), "the player's tenth food has not been used")
            run.inventoryEvidence(listOf("Bag of Seeds" to 1), 20000)
            run.scanBirdSources(0, List(9) { spawned })
            assertFalse(run.uniqueComplete(absent, state), "held food can still spawn the missing bird")
            run.inventoryEvidence(emptyList(), 21000)
            run.birdFoodCheck.spawn("A Bluebird was attracted to the Birdfeeder!", 21001)
            run.scanBirdSources(1, List(9) { spawned })
            assertFalse(run.uniqueComplete(absent, state), "remaining floor food prevents absence proof")
            run.scanBirdSources(0, List(9) { spawned })
            assertTrue(run.uniqueUnavailable(absent), absent)
            assertTrue(run.uniqueComplete(absent, state, localOnly = true), absent)
            assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == absent })
            assertFalse(SafariEspRules.neededForRun(absent, false, run, false, false, state))
            assertFalse(run.completion.allNeededChecked(run, SafariBiome.FOREST, state, localOnly = true))
        assertNull(run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        qualifiedChecks(run, SafariBiome.FOREST)
        assertTrue(run.completion.allNeededChecked(run, SafariBiome.FOREST, state, localOnly = true))
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
    @Test fun `cleared sources supersede an old mound snapshot after leaving Cavern`() {
        val run=SafariRun(0);val state=party()
        val mound=Triple(-100,40,50)
        run.visitBiome(SafariBiome.CAVERN)
        run.moundSurvey.scan(setOf(mound)) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN,setOf("Rockmite Mound"),false)
        run.observeCavernSources(true,true,false)
        run.visitBiome(SafariBiome.HAUNTED)
        assertFalse(run.uniqueUnavailable("Rockmite"))
        assertFalse(run.uniqueUnavailable("Snoozle"))
        run.moundSurvey.scan(emptySet()) { true }
        run.observeCavernSources(false,false,true)
        assertTrue(run.uniqueComplete("Rockmite",state))
        assertTrue(run.uniqueComplete("Snoozle",state))
        assertEquals(0,run.count("Rockmite"));assertEquals(0,run.count("Snoozle"))
        run.observeCritter(1,"Snoozle")
        assertFalse(run.uniqueComplete("Snoozle",state))
    }
}
