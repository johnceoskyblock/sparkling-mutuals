package net.johnceo.sparklingmutuals.safari.structures

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SafariHelperRules
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BeeNestCompletionTest {
    private fun party(needsHoneybug: Boolean) = PartySparklingState().apply {
        select(setOf("me"))
        accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() -
            if (needsHoneybug) setOf("Honeybug") else emptySet())))
    }
    @Test fun `Sparkling Honeybug completes on nest evidence without waiting for bee sightings`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), nestsChecked = false)
        assertFalse(run.sparklingComplete("Honeybug", party(true), localOnly = true))
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), nestsChecked = true)
        assertTrue(run.sparklingComplete("Honeybug", party(true), localOnly = true))
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), nestsChecked = false)
        assertFalse(run.sparklingComplete("Honeybug", party(true), localOnly = true))
    }
    @Test fun `Unique Honeybug needs one capture without local nest-check requirements`() {
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.FOREST)
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true, nestsPunched = 3)
        assertFalse(run.uniqueComplete("Honeybug", party(true), localOnly = true))
        run.record(SafariCatch(SafariRoster.named("Honeybug")!!))
        assertTrue(run.uniqueComplete("Honeybug", party(true), localOnly = true))
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, false, nestsPunched = 2)
        assertTrue(run.uniqueComplete("Honeybug", party(true), localOnly = true))
        assertTrue(run.uniqueComplete("Honeybug", party(false), localOnly = true))
        assertFalse(SafariHelperRules.showNests(SafariMode.UNIQUE, run, party(true)), "a completed punch survey remains resolved")
        assertFalse(SafariEspRules.neededForRun("Honeybug", false, run, false, party = party(true)))
    }
    @Test fun `Full Clear requires every observed bee even without a center zone scan`() {
        val run = SafariRun(0)
        val honeybug = SafariRoster.named("Honeybug")!!
        repeat(4) { run.observeCritter(it, "Honeybug", java.util.UUID.randomUUID()) }
        repeat(3) { run.record(SafariCatch(honeybug)) }
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, true)
        assertFalse(run.captureComplete("Honeybug"))
        run.record(SafariCatch(honeybug))
        assertTrue(run.captureComplete("Honeybug"))
        run.updateCaptureEvidence(SafariBiome.FOREST, setOf("Honeybug"), false, true)
        assertFalse(run.captureComplete("Honeybug"))
        run.updateCaptureEvidence(SafariBiome.FOREST, emptySet(), false, false)
        assertTrue(run.captureComplete("Honeybug"), "completed source evidence survives later empty scans")
    }
}
