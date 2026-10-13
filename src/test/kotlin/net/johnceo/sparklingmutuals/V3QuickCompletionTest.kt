package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class V3QuickCompletionTest {
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    private fun caught(run: SafariRun, name: String) = run.record(SafariCatch(SafariRoster.named(name)!!))
    private fun unique() { ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false }

    @Test fun `Unique announces Haunted and Icy completion instead of suppressing it`() {
        for ((biome, word) in listOf(SafariBiome.HAUNTED to "hd", SafariBiome.ICY to "id")) {
            val run = SafariRun(0)
            run.visitBiome(biome)
            biome.critters.forEach { caught(run, it.name) }
            run.updateCaptureEvidence(biome, emptySet(), true)
            assertEquals(word, run.completion.announcement(run, biome, SafariMode.UNIQUE, party()))
            assertNull(run.completion.announcement(run, biome, SafariMode.UNIQUE, party()))
        }
    }
    @Test fun `party done overrides every Unique requirement even in the starting biome`() {
        val state = party(*SafariRoster.all.map { it.name }.toTypedArray())
        for ((biome, word) in listOf(SafariBiome.FOREST to "fd", SafariBiome.HAUNTED to "hd", SafariBiome.ICY to "id")) {
            val run = SafariRun(0); run.visitBiome(biome)
            run.partyFullClear("Party > Friend: $word")
            run.updateCaptureEvidence(biome, biome.critters.map { it.name }.toSet(), false)
            assertEquals(biome.critters.size, run.completed(SafariMode.UNIQUE, biome.critters, state))
            assertEquals(0, biome.critters.sumOf { run.count(it.name) })
            assertEquals(0, run.completed(SafariMode.UNIQUE, biome.critters, state, localOnly = true))
        }
    }
    @Test fun `party done removes overridden missing Unique rows without inventing captures`() {
        unique()
        val run = SafariRun(0); val state = party("Honeybug", "Macaw")
        run.visitBiome(SafariBiome.FOREST)
        SafariMessages.forRun("Party > Friend: fd", run, true)
        assertTrue(SafariPanels.missingCritters(run, SafariBiome.FOREST, state).isEmpty())
        assertEquals(0, run.count("Honeybug"))
        ConfigManager.fullClearMode = true
        assertEquals(9, SafariPanels.missingCritters(run, SafariBiome.FOREST, state).size, "Full Clear capture rows retain their independent evidence")
        unique()
    }
    @Test fun `cd is trusted until locally observed party-needed structures contradict it independently`() {
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING)) {
            val run = SafariRun(0); val needed = party("Snoozle")
            SafariMessages.forRun("Party > Friend: cd", run, true)
            assertEquals(9, run.completed(mode, SafariBiome.CAVERN.critters, needed))
            run.visitBiome(SafariBiome.CAVERN)
            run.reconcileStructures(SafariBiome.CAVERN, true, false)
            assertEquals(9, run.completed(mode, SafariBiome.CAVERN.critters, needed), "unneeded Rockmite mounds must not reopen the biome")
            run.reconcileStructures(SafariBiome.CAVERN, false, true)
            assertEquals(8, run.completed(mode, SafariBiome.CAVERN.critters, needed))
            assertTrue(run.completed(mode, listOf(SafariRoster.named("Rockmite")!!), needed) == 1)
            assertFalse(run.completed(mode, listOf(SafariRoster.named("Snoozle")!!), needed) == 1)
            run.reconcileStructures(SafariBiome.CAVERN, true, false)
            assertEquals(9, run.completed(mode, SafariBiome.CAVERN.critters, needed))
        }
    }
    @Test fun `cd received after a local survey still respects needed intact mounds`() {
        val run = SafariRun(0); val needed = party("Rockmite")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING)) {
            assertEquals(8, run.completed(mode, SafariBiome.CAVERN.critters, needed))
            assertFalse(run.completed(mode, listOf(SafariRoster.named("Rockmite")!!), needed) == 1)
            assertTrue(run.completed(mode, listOf(SafariRoster.named("Snoozle")!!), needed) == 1)
        }
    }
    @Test fun `party cd hides only the Cavern helpers that are no longer needed`() {
        unique()
        val run = SafariRun(0); val needed = party("Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertFalse(SafariHelperRules.needed("Rockmite", SafariMode.UNIQUE, run, needed))
        assertTrue(SafariHelperRules.needed("Snoozle", SafariMode.UNIQUE, run, needed))
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, mounds = 3, walls = WallSummary(List(5) { WallState.INTACT }), party = needed).rows
        assertFalse(rows.any { it.label == "Mounds to break" })
        assertTrue(rows.any { it.label == "Snooper walls to break" })
    }
    @Test fun `late cd cannot hide needed Sparkling structure helpers or mound ESP`() {
        val run = SafariRun(0); val needed = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertTrue(SafariHelperRules.needed("Rockmite", SafariMode.SPARKLING, run, needed))
        assertTrue(SafariHelperRules.needed("Snoozle", SafariMode.SPARKLING, run, needed))
        assertTrue(SafariEspRules.neededForSparkling("Rockmite", true, needed, false, run))
    }
    @Test fun `Gazer waits for four distinct UUIDs only when the party needs its sparkling form`() {
        val run = SafariRun(0); val state = party("Gazer")
        run.visitBiome(SafariBiome.HAUNTED); caught(run, "Gazer")
        val id = UUID.randomUUID()
        repeat(4) { run.observeCritter(it, "Gazer", id) }
        assertFalse(run.uniqueComplete("Gazer", state), "four entity IDs for one UUID are not four gazers")
        repeat(2) { run.observeCritter(10 + it, "Gazer", UUID.randomUUID()) }
        assertFalse(run.uniqueComplete("Gazer", state))
        run.observeCritter(12, "Gazer", UUID.randomUUID())
        assertTrue(run.uniqueComplete("Gazer", state))
        val unneeded = SafariRun(0).apply { caught(this, "Gazer") }
        assertTrue(unneeded.uniqueComplete("Gazer", party()))
        assertFalse(SafariRun(0).uniqueComplete("Gazer", state))
    }
    @Test fun `all food used without any Macaw removes the missing row and completes Forest`() {
        unique()
        val run = SafariRun(0); val state = party("Macaw")
        run.visitBiome(SafariBiome.FOREST)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        repeat(8) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertFalse(run.uniqueUnavailable("Macaw"))
        run.birds.spawn("A Parakeet was attracted to the Birdfeeder!")
        assertTrue(run.uniqueUnavailable("Macaw"))
        assertTrue(run.uniqueComplete("Macaw", state))
        val rows = SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows
        assertFalse(rows.any { it.label == "Macaw" })
        assertTrue(rows.any { it.label == "No macaws this run" })
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        assertEquals(0, run.count("Macaw"))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        assertFalse(run.uniqueUnavailable("Macaw"))
    }
    @Test fun `starting food and extra pickups prevent premature Macaw absence`() {
        val run = SafariRun(0)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        repeat(9) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertFalse(run.uniqueUnavailable("Macaw"))
        repeat(2) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertTrue(run.uniqueUnavailable("Macaw"))
        repeat(12) { run.birds.pickup("FLOOR DROP! Bag of Seeds", SafariBiome.FOREST) }
        assertFalse(run.uniqueUnavailable("Macaw"))
        run.birds.spawn("A Bluebird was attracted to the Birdfeeder!")
        assertFalse(run.uniqueUnavailable("Macaw"), "two starting foods remain in addition to twelve pickups")
        repeat(2) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        assertTrue(run.uniqueUnavailable("Macaw"))
        run.birds.spawn("Two Macaws were attracted to the Birdfeeder!")
        assertFalse(run.uniqueUnavailable("Macaw"), "spawn announcement is evidence even before the entity scan")
    }
    @Test fun `completed Cavern absence keeps explanatory rows after helpers are no longer needed`() {
        unique()
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.moundSurvey.scan(emptySet()) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, walls = WallSummary(List(5) { WallState.BROKEN }), party = state).rows
        assertFalse(rows.any { it.label == "Rockmite" || it.label == "Snoozle" })
        assertTrue(rows.any { it.label == "No rockmites this run" })
        assertTrue(rows.any { it.label == "No snoozles this run" })
    }
    @Test fun `completion announcement is once per biome across mode switches`() {
        val run = SafariRun(0); val state = party("Wumpa")
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, state))
    }
    @Test fun `new cd outside Cavern replaces stale survey until fresh local checking`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        run.visitBiome(SafariBiome.FOREST)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING))
            assertEquals(9, run.completed(mode, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.CAVERN)
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.reconcileStructures(SafariBiome.CAVERN, true, false)
        assertEquals(8, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        assertEquals(8, run.completed(SafariMode.SPARKLING, SafariBiome.CAVERN.critters, state))
    }
    @Test fun `an unsent completion can retry after a mode switch cancels it`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.FULL_CLEAR, state))
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state), "the canceled queue reservation must not count as a sent announcement")
    }
    @Test fun `an actually sent completion is not repeated after later contradictions`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        run.completion.sent(SafariBiome.ICY)
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.FULL_CLEAR, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
    }
    @Test fun `Sparkling biome with no remaining discovery needs can announce after a local survey`() {
        val run = SafariRun(0)
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, party()))
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, party()))
        val unknown = SafariRun(0).apply { updateCaptureEvidence(SafariBiome.ICY, emptySet(), false) }
        assertNull(unknown.completion.announcement(unknown, SafariBiome.ICY, SafariMode.SPARKLING, PartySparklingState()))
    }
}
