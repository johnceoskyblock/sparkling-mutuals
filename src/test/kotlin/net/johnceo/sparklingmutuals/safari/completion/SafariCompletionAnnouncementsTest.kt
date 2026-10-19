package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.resources.renderBird
import net.johnceo.sparklingmutuals.safari.resources.scanRenderedBirds

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.structures.WallState
import net.johnceo.sparklingmutuals.safari.structures.WallSummary
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariCompletionAnnouncementsTest {
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
    @Test fun `party done overrides no Unique requirement even in the starting biome`() {
        val state = party(*SafariRoster.all.map { it.name }.toTypedArray())
        for ((biome, word) in listOf(SafariBiome.FOREST to "fd", SafariBiome.HAUNTED to "hd", SafariBiome.ICY to "id")) {
            val run = SafariRun(0); run.visitBiome(biome)
            run.partyFullClear("Party > Friend: $word")
            run.updateCaptureEvidence(biome, biome.critters.map { it.name }.toSet(), false)
            assertEquals(0, run.completed(SafariMode.UNIQUE, biome.critters, state))
            assertEquals(0, biome.critters.sumOf { run.count(it.name) })
            assertEquals(0, run.completed(SafariMode.UNIQUE, biome.critters, state, localOnly = true))
        }
    }
    @Test fun `party done preserves uncaptured Unique rows`() {
        unique()
        val run = SafariRun(0); val state = party("Honeybug", "Macaw")
        run.visitBiome(SafariBiome.FOREST)
        SafariMessages.forRun("Party > Friend: fd", run, true)
        assertEquals(9, SafariPanels.missingCritters(run, SafariBiome.FOREST, state).size)
        assertEquals(0, run.count("Honeybug"))
        ConfigManager.fullClearMode = true
        assertEquals(9, SafariPanels.missingCritters(run, SafariBiome.FOREST, state).size, "fd supplies progress trust, not local missing-panel proof")
        unique()
    }
    @Test fun `cd requires source proof for Unique no spawn and never adds catches`() {
        val run = SafariRun(0); val needed = party("Snoozle")
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, needed))
        assertEquals(9, run.completed(SafariMode.SPARKLING, SafariBiome.CAVERN.critters, needed))
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertFalse(run.sparklingComplete("Snoozle", needed))
        run.sparklingChecks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList(), wallsCleared = true)
        assertTrue(run.sparklingComplete("Snoozle", needed))
        assertTrue(run.uniqueComplete("Snoozle", needed))
        assertEquals(0, run.count("Snoozle"))
    }
    @Test fun `fresh local evidence invalidates a received cd without adding catches`() {
        val run = SafariRun(0); val needed = party("Rockmite")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, needed))
        assertFalse(run.sparklingComplete("Rockmite", needed))
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Rockmite Mound"), false)
        assertEquals(0, run.completed(SafariMode.SPARKLING, SafariBiome.CAVERN.critters, needed))
    }
    @Test fun `party cd cannot hide required Unique Cavern helpers`() {
        unique()
        val run = SafariRun(0); val needed = party("Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertTrue(SafariHelperRules.needed("Rockmite", SafariMode.UNIQUE, run, needed))
        assertTrue(SafariHelperRules.needed("Snoozle", SafariMode.UNIQUE, run, needed))
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, mounds = 3, walls = WallSummary(List(5) { WallState.INTACT }), party = needed).rows
        assertTrue(rows.any { it.label == "Rockmite" })
        assertTrue(rows.any { it.label == "Snoozle" })
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
    @Test fun `Unique Gazer completes from its first shared catch without a UUID radius check`() {
        val run = SafariRun(0); val state = party("Gazer")
        run.visitBiome(SafariBiome.HAUNTED); caught(run, "Gazer")
        val id = UUID.randomUUID()
        repeat(4) { run.observeCritter(it, "Gazer", id) }
        assertTrue(run.uniqueComplete("Gazer", state), "four entity IDs for one UUID are not four gazers")
        repeat(2) { run.observeCritter(10 + it, "Gazer", UUID.randomUUID()) }
        assertTrue(run.uniqueComplete("Gazer", state))
        run.observeCritter(12, "Gazer", UUID.randomUUID())
        run.sparklingChecks.scan(SafariBiome.HAUNTED, -4.0, -64.0, List(3) { "Gazer" to UUID.randomUUID() })
        assertTrue(run.uniqueComplete("Gazer", state))
        val unneeded = SafariRun(0).apply { caught(this, "Gazer") }
        assertTrue(unneeded.uniqueComplete("Gazer", party()))
        assertFalse(SafariRun(0).uniqueComplete("Gazer", state))
    }
    @Test fun `all food used without any Macaw completes Unique Forest without inventing a catch`() {
        unique()
        val run = SafariRun(0); val state = party("Macaw")
        run.visitBiome(SafariBiome.FOREST)
        SafariBiome.FOREST.critters.filter { it.name != "Macaw" }.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        repeat(8) { run.birds.renderBird("Bluebird") }
        assertFalse(run.uniqueUnavailable("Macaw"))
        assertFalse(run.uniqueComplete("Macaw", state))
        assertTrue(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Macaw" })
        run.birds.renderBird("Parakeet")
        repeat(6) { run.recordBirdFood("FLOOR DROP! Yogi Berry", SafariBiome.FOREST, 10000) }
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        assertTrue(run.uniqueUnavailable("Macaw"))
        assertTrue(run.uniqueComplete("Macaw", state))
        val rows = SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows
        assertFalse(rows.any { it.label == "Macaw" })
        assertFalse(rows.any { it.label == "No macaws this run" })
        assertEquals(SafariBiome.FOREST.critters.size, run.completed(SafariMode.UNIQUE, SafariBiome.FOREST.critters, state))
        assertEquals("fd", run.completion.announcement(run, SafariBiome.FOREST, SafariMode.UNIQUE, state))
        assertEquals(0, run.count("Macaw"))
        assertEquals(0, run.personalCount("Macaw"))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Macaw"), false)
        run.scanBirdSources(0, listOf("Macaw"))
        assertFalse(run.uniqueUnavailable("Macaw"))
        assertFalse(run.uniqueComplete("Macaw", state))
        assertTrue(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Macaw" })
    }
    @Test fun `starting food and extra pickups prevent premature Macaw absence`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.inventoryEvidence(listOf("Bag of Seeds" to 2), 0)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
        repeat(9) { run.birds.renderBird("Bluebird") }
        assertFalse(run.uniqueUnavailable("Macaw"))
        repeat(2) { run.birds.renderBird("Bluebird") }
        assertFalse(run.uniqueUnavailable("Macaw"), "party feedings cannot resolve food still held locally")
        run.inventoryEvidence(emptyList(), 10000)
        run.scanRenderedBirds()
        assertTrue(run.uniqueUnavailable("Macaw"))
        repeat(12) { run.birds.pickup("FLOOR DROP! Bag of Seeds", SafariBiome.FOREST) }
        assertFalse(run.uniqueUnavailable("Macaw"))
        run.birds.renderBird("Bluebird")
        assertFalse(run.uniqueUnavailable("Macaw"), "two starting foods remain in addition to twelve pickups")
        repeat(11) { run.birds.renderBird("Bluebird") }
        run.scanRenderedBirds()
        assertTrue(run.uniqueUnavailable("Macaw"))
        run.birds.renderBird("Macaw")
        assertFalse(run.uniqueUnavailable("Macaw"), "spawn announcement is evidence even before the entity scan")
        assertFalse(run.uniqueComplete("Macaw", party("Macaw")))
    }
    @Test fun `completed Cavern absence removes Unique species without explanatory rows`() {
        unique()
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.moundSurvey.scan(emptySet()) { true }
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, walls = WallSummary(List(5) { WallState.BROKEN }), party = state).rows
        assertFalse(rows.any { it.label == "Rockmite" || it.label == "Snoozle" })
        assertFalse(rows.any { it.label.startsWith("No ") })
        assertTrue(run.uniqueUnavailable("Rockmite")); assertTrue(run.uniqueUnavailable("Snoozle"))
    }
    @Test fun `completion announcement is once per biome across mode switches`() {
        val run = SafariRun(0); val state = party("Wumpa")
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        run.sparklingChecks.scan(SafariBiome.ICY, -110.0, -78.0, emptyList())
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, state))
    }
    @Test fun `remote cd cannot erase globally observed intact sources`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        run.visitBiome(SafariBiome.FOREST)
        SafariMessages.forRun("Party > Friend: cd", run, true)
        assertEquals(7, run.completed(SafariMode.SPARKLING, SafariBiome.CAVERN.critters, state))
        assertEquals(0, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.CAVERN)
        assertEquals(0, run.completed(SafariMode.SPARKLING, SafariBiome.CAVERN.critters, state))
    }
    @Test fun `an unsent completion can retry after a mode switch cancels it`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        run.sparklingChecks.scan(SafariBiome.ICY, -110.0, -78.0, emptyList())
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.FULL_CLEAR, state))
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state), "the canceled queue reservation must not count as a sent announcement")
    }
    @Test fun `an actually sent completion is not repeated after later contradictions`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.ICY)
        SafariBiome.ICY.critters.forEach { caught(run, it.name) }
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        run.sparklingChecks.scan(SafariBiome.ICY, -110.0, -78.0, emptyList())
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
        run.completion.sent(SafariBiome.ICY)
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.FULL_CLEAR, state))
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.UNIQUE, state))
    }
    @Test fun `Sparkling biome with no remaining discovery needs can announce after a local survey`() {
        val run = SafariRun(0)
        assertNull(run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, party()))
        run.visitBiome(SafariBiome.ICY)
        run.updateCaptureEvidence(SafariBiome.ICY, emptySet(), false)
        run.sparklingChecks.scan(SafariBiome.ICY, -110.0, -78.0, emptyList())
        assertEquals("id", run.completion.announcement(run, SafariBiome.ICY, SafariMode.SPARKLING, party()))
        val unknown = SafariRun(0).apply { updateCaptureEvidence(SafariBiome.ICY, emptySet(), false) }
        assertNull(unknown.completion.announcement(unknown, SafariBiome.ICY, SafariMode.SPARKLING, PartySparklingState()))
    }
}
