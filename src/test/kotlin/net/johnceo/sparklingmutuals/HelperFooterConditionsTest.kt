package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class HelperFooterConditionsTest {
    @TempDir lateinit var dir: Path
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("local"))
        accept(setOf("local"), mapOf("local" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }
    private fun labels(run: SafariRun, mode: SafariMode, state: PartySparklingState): Set<String> {
        ConfigManager.fullClearMode = mode == SafariMode.FULL_CLEAR
        ConfigManager.sparklingMode = mode == SafariMode.SPARKLING
        return listOf(SafariBiome.FOREST, SafariBiome.CAVERN).flatMap {
            SafariPanels.missing(run, it, mode == SafariMode.UNIQUE, 3, 2,
                WallSummary(List(5) { WallState.INTACT }), state).rows
        }.map { it.label }.toSet()
    }
    @Test fun `source footers follow each modes first catch and missing discovery rules independently`() {
        ConfigManager.init(dir)
        val helpers = mapOf("Honeybug" to "Bee nests to punch", "Snoozle" to "Snooper walls to break", "Rockmite" to "Mounds to break")
        val run = SafariRun(0)
        assertTrue(labels(run, SafariMode.UNIQUE, party()).containsAll(helpers.values))
        helpers.keys.forEach { run.record(SafariCatch(SafariRoster.named(it)!!)) }
        assertTrue(labels(run, SafariMode.FULL_CLEAR, party()).containsAll(helpers.values))
        assertTrue(helpers.values.none { it in labels(run, SafariMode.UNIQUE, party()) })
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING)) for ((species, footer) in helpers) {
            val current = labels(run, mode, party(species))
            assertTrue(footer in current)
            assertTrue(helpers.values.filter { it != footer }.none { it in current })
        }
    }
    @Test fun `resolved sources hide helpers without bringing back duplicate Gazer or absence prose`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        run.updateNestEvidence(true, 3, 3, 0)
        repeat(3) { run.observeCritter(it, "Honeybug", java.util.UUID(0, it.toLong())) }
        ConfigManager.fullClearMode = true
        val forest = SafariPanels.missing(run, SafariBiome.FOREST, false, 0).rows
        assertFalse(forest.any { it.label == "Bee nests to punch" })
        val cavern = SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, 0, WallSummary(List(5) { WallState.BROKEN })).rows
        assertFalse(cavern.any { it.label in setOf("Snooper walls to break", "Mounds to break") || it.label.startsWith("No ") })
        val haunted = SafariPanels.missing(run, SafariBiome.HAUNTED, false, 0).rows
        assertEquals(1, haunted.count { it.label == "Gazer" })
        assertFalse(haunted.any { it.label.startsWith("Gazers") })
    }
}
