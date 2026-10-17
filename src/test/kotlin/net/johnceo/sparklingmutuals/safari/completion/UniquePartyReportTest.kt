package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniquePartyReportTest {
    private fun party(needs: Set<String> = SafariRoster.all.map { it.name }.toSet()) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needs)))
    }
    private fun word(biome: SafariBiome) = when (biome) {
        SafariBiome.FOREST -> "fd"; SafariBiome.CAVERN -> "cd"; SafariBiome.ICY -> "id"; SafariBiome.HAUNTED -> "hd"
    }
    private fun report(run: SafariRun, biome: SafariBiome) = SafariMessages.forRun("Party > Friend: ${word(biome)}", run, true)
    private fun caught(run: SafariRun, biome: SafariBiome, except: String? = null) = biome.critters.filter { it.name != except }
        .forEach { run.record(SafariCatch(it, false)) }
    @Test fun `each ordinary Unique capture advances remote progress immediately without any report or scan`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val state = party()
        for (species in SafariRoster.all.filter { it.name !in setOf("Honeybug", "Rockmite", "Snoozle") }) {
            val run = SafariRun(0)
            run.visitBiome(SafariBiome.entries.first { it != species.biome })
            assertFalse(run.uniqueComplete(species.name, state))
            run.record(SafariCatch(species, false))
            assertTrue(run.uniqueComplete(species.name, state), species.name)
            assertEquals("1/${species.biome.critters.size}", SafariPanels.progress(run, true, 0, state).rows.single { it.label == species.biome.label }.value)
            assertFalse(SafariPanels.missing(run, species.biome, true, 0, party = state).rows.any { it.label == species.name })
        }
    }
    @Test fun `Rockmite and Snoozle need their own catch and cd but not every Cavern catch`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        for (reportFirst in listOf(false, true)) {
            val run = SafariRun(0); val state = party(emptySet())
            if (reportFirst) report(run, SafariBiome.CAVERN)
            for (name in listOf("Rockmite", "Snoozle")) {
                assertFalse(run.uniqueComplete(name, state))
                run.record(SafariCatch(SafariRoster.named(name)!!, false))
                assertEquals(reportFirst, run.uniqueComplete(name, state))
                assertEquals(!reportFirst, SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, party = state).rows.any { it.label == name })
            }
            report(run, SafariBiome.CAVERN)
            assertEquals(2, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        }
    }
    @Test fun `loaded Cavern sources can reopen and complete checks while standing in Forest`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.FOREST)
        for (name in listOf("Rockmite", "Snoozle")) run.record(SafariCatch(SafariRoster.named(name)!!, false))
        val site = Triple(-100, 40, 50)
        run.moundSurvey.scan(setOf(site)) { true }
        run.observeCavernSources(true, true, false)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.moundSurvey.scan(emptySet()) { false } // Unloaded space is not a broken mound.
        run.observeCavernSources(false, false, false)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertFalse(run.uniqueComplete("Snoozle", state))
        run.moundSurvey.scan(emptySet()) { true }
        run.observeCavernSources(false, false, true)
        assertTrue(run.uniqueComplete("Rockmite", state)); assertTrue(run.uniqueComplete("Snoozle", state))
        assertEquals(SafariBiome.FOREST, run.lastBiome)
    }

    @Test fun `global source proof checks without catches and cd confirms only an unseen zero spawn`() {
        val run = SafariRun(0); val state = party()
        run.visitBiome(SafariBiome.HAUNTED)
        run.moundSurvey.scan(setOf(Triple(-100, 40, 50))) { true }
        run.moundSurvey.scan(emptySet()) { true }
        run.observeCavernSources(false, false, true)
        for (name in listOf("Rockmite", "Snoozle")) {
            assertTrue(run.sparklingComplete(name, state))
            assertFalse(run.uniqueComplete(name, state))
        }
        report(run, SafariBiome.CAVERN)
        for (name in listOf("Rockmite", "Snoozle")) {
            assertTrue(run.uniqueComplete(name, state))
            assertEquals(0, run.count(name))
        }
        run.observeCritter(12, "Rockmite")
        assertFalse(run.uniqueComplete("Rockmite", state), "seen ordinary critters still need the Unique catch")
        run.observeSparkling(UUID.randomUUID(), "Snoozle")
        assertFalse(run.sparklingComplete("Snoozle", state), "resolved sources cannot hide an uncaptured Sparkling")
        run.visitBiome(SafariBiome.CAVERN)
        assertTrue(run.sparklingComplete("Rockmite", state), "entry preserves source proof")
    }

    @Test fun `all four party reports require real captures then accept remaining checking evidence`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val state = party()
        for (biome in SafariBiome.entries) {
            val run = SafariRun(0)
            val last = biome.critters.last()
            report(run, biome)
            caught(run, biome, last.name)
            assertFalse(run.uniqueComplete(last.name, state), biome.label)
            run.record(SafariCatch(last, false))
            assertEquals(biome.critters.size, run.completed(SafariMode.UNIQUE, biome.critters, state), biome.label)
            assertFalse(SafariPanels.missing(run, biome, true, 3, party = state).rows.any { row -> biome.critters.any { it.name == row.label } })
            assertEquals("${biome.critters.size}/${biome.critters.size}", SafariPanels.progress(run, true, 0, state).rows.single { it.label == biome.label }.value)
            assertTrue(biome.critters.all { run.count(it.name) == 1 && run.personalCount(it.name) == 0 })
        }
    }
    @Test fun `Forest Icy and Haunted stay complete on entry despite unfinished local discovery checks`() {
        val state = party()
        for (biome in listOf(SafariBiome.FOREST, SafariBiome.ICY, SafariBiome.HAUNTED)) {
            val run = SafariRun(0); caught(run, biome); report(run, biome)
            repeat(2) {
                run.visitBiome(SafariBiome.entries.first { it != biome }); run.visitBiome(biome)
                run.updateCaptureEvidence(biome, biome.critters.map { it.name }.toSet(), false)
                run.inventoryEvidence(listOf("Shining Coin" to 2, "Yogi Berry" to 2), 10000)
                run.sparklingChecks.scan(biome, 0.0, 0.0, emptyList())
                assertEquals(biome.critters.size, run.completed(SafariMode.UNIQUE, biome.critters, state), biome.label)
            }
            assertEquals(biome.critters.size - if (biome == SafariBiome.FOREST) 1 else 0,
                run.completed(SafariMode.UNIQUE, biome.critters, state, localOnly = true))
            assertNull(run.completion.announcement(run, biome, SafariMode.UNIQUE, state), "do not echo another player's checking evidence")
        }
    }
    @Test fun `intact Cavern sources contradict completion regardless of discovery ownership`() {
        ConfigManager.fullClearMode = false; ConfigManager.sparklingMode = false
        val run = SafariRun(0); caught(run, SafariBiome.CAVERN); report(run, SafariBiome.CAVERN)
        val state = party()
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        run.visitBiome(SafariBiome.CAVERN)
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state), "unknown source scans are not contradictions")
        run.reconcileStructures(SafariBiome.CAVERN, true, false)
        assertFalse(run.uniqueComplete("Rockmite", state)); assertTrue(run.uniqueComplete("Snoozle", state))
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
        assertEquals(setOf("Rockmite", "Snoozle"), SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, party = state).rows
            .map { it.label }.filter { label -> SafariBiome.CAVERN.critters.any { it.name == label } }.toSet())
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, party(emptySet())))
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, party(setOf("Snoozle"))))
        run.visitBiome(SafariBiome.ICY); report(run, SafariBiome.CAVERN)
        assertEquals(7, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state), "a repeated report cannot erase observed unbroken sources")
        run.visitBiome(SafariBiome.CAVERN)
        run.sparklingChecks.scan(SafariBiome.CAVERN, -114.0, 49.0, emptyList(), moundsCleared = true, wallsCleared = true)
        assertEquals(9, run.completed(SafariMode.UNIQUE, SafariBiome.CAVERN.critters, state))
    }
    @Test fun `trusted Unique reports never hide a positively detected uncaptured Sparkling`() {
        val run = SafariRun(0); val state = party()
        caught(run, SafariBiome.HAUNTED); report(run, SafariBiome.HAUNTED)
        run.observeSparkling(UUID.randomUUID(), "Gazer")
        assertFalse(run.uniqueComplete("Gazer", state))
        report(run, SafariBiome.HAUNTED)
        assertFalse(run.uniqueComplete("Gazer", state))
        run.sparklingBroadcast("SPARKLING! Friend caught a SPARKLING Gazer!", 1000)
        assertTrue(run.uniqueComplete("Gazer", state))
    }
}
