package net.johnceo.sparklingmutuals.safari.ui

import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NestCounterPanelTest {
    @Test fun `mid-run bee population resolves the counter only while site coverage is known`() {
        val run = SafariRun(0).apply {
            visitBiome(SafariBiome.FOREST)
            updateNestEvidence(false, 0, 3, 0)
            record(SafariCatch(SafariRoster.named("Honeybug")!!))
            liveHoneybugs(2)
        }
        for (mode in SafariMode.entries)
            assertFalse(rows(mode, run, 3).any { it.label == "Bee nests to punch" })
        run.updateNestEvidence(false, 0, 4, 1)
        for (mode in SafariMode.entries)
            assertTrue(rows(mode, run, 4).any { it.label == "Bee nests to punch" })
    }
    private fun party(needs: Boolean = true) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() -
            if (needs) setOf("Honeybug") else emptySet())))
    }
    private fun rows(mode: SafariMode, run: SafariRun, nests: Int, needs: Boolean = true) = kotlin.run {
        ConfigManager.fullClearMode = mode == SafariMode.FULL_CLEAR
        ConfigManager.sparklingMode = mode == SafariMode.SPARKLING
        SafariPanels.missing(run, SafariBiome.FOREST, mode == SafariMode.UNIQUE, nests, party = party(needs)).rows
    }
    @Test fun `missing panel shows only the nest counter without survey diagnostics`() {
        for (mode in SafariMode.entries) {
            val run = SafariRun(0).apply { visitBiome(SafariBiome.FOREST); updateNestEvidence(false, 0, 3, 4) }
            val panel = rows(mode, run, 3)
            assertEquals("3", panel.first { it.label == "Bee nests to punch" }.value)
            assertFalse(panel.any { it.label in setOf("Nest survey", "Unverified nest sites", "Honeybugs captured",
                "Load grey survey markers", "Check nest data: /sm debug") })
        }
    }
    @Test fun `counter hides for actual three to six nest totals only after distinct bees match`() {
        for (total in 3..6) {
            val run = SafariRun(0).apply {
                visitBiome(SafariBiome.FOREST)
                updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false)
                updateNestEvidence(true, total, total, 0)
            }
            repeat(total - 1) { run.observeCritter(it, "Honeybug", UUID(0, it.toLong())) }
            assertTrue(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
            run.observeCritter(100, "Honeybug", UUID(0, (total - 1).toLong()))
            assertFalse(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
            assertFalse(run.captureComplete("Honeybug"))
            repeat(total) { run.record(SafariCatch(SafariRoster.named("Honeybug")!!)) }
            assertTrue(run.captureComplete("Honeybug"))
        }
    }
    @Test fun `repeated sightings and entity IDs do not stand in for unique Honeybug UUIDs`() {
        val run = SafariRun(0).apply { visitBiome(SafariBiome.FOREST); updateNestEvidence(true, 3, 3, 0) }
        repeat(5) { run.observeCritter(it, "Honeybug", UUID(0, 1)) }
        assertTrue(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
        run.observeCritter(7, "Honeybug", UUID(0, 2)); run.observeCritter(8, "Honeybug", UUID(0, 3))
        assertFalse(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
        run.observeCritter(9, "Honeybug", UUID(0, 4))
        assertTrue(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
    }
    @Test fun `Unique and Sparkling counters wait for spawned bees after the last nest click`() {
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING)) {
            val run = SafariRun(0).apply {
                visitBiome(SafariBiome.FOREST); record(SafariCatch(SafariRoster.named("Honeybug")!!))
                updateNestEvidence(true, 3, 3, 0)
            }
            assertTrue(rows(mode, run, 0).any { it.label == "Bee nests to punch" })
            repeat(3) { run.observeCritter(it, "Honeybug", UUID(0, it.toLong())) }
            assertFalse(rows(mode, run, 0).any { it.label == "Bee nests to punch" })
        }
    }
    @Test fun `incomplete survey prevents the zero counter disappearing and party overrides remain mode aware`() {
        val run = SafariRun(0).apply { visitBiome(SafariBiome.FOREST); updateNestEvidence(false, 3, 3, 1) }
        repeat(3) { run.observeCritter(it, "Honeybug", UUID(0, it.toLong())) }
        assertTrue(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
        run.partyFullClear("Party > Friend: fd")
        for (mode in listOf(SafariMode.UNIQUE, SafariMode.SPARKLING))
            assertFalse(rows(mode, run, 0).any { it.label == "Bee nests to punch" })
        assertTrue(rows(SafariMode.FULL_CLEAR, run, 0).any { it.label == "Bee nests to punch" })
    }
}
