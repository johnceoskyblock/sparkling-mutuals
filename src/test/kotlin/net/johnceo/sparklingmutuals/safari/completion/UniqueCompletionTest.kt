package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniqueCompletionTest {
    private fun party(vararg needed: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to (SafariRoster.all.map { it.name }.toSet() - needed.toSet())))
    }
    private fun caught(run: SafariRun, vararg names: String) = names.forEach { run.record(SafariCatch(SafariRoster.named(it)!!)) }
    private fun complete(run: SafariRun, name: String, party: PartySparklingState) =
        run.completed(SafariMode.UNIQUE, listOf(SafariRoster.named(name)!!), party) == 1

    @Test fun `Cavern special catches wait for local sparkle checks and unvisited party done`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        caught(run, "Rockmite", "Snoozle", "Driftling")
        assertFalse(complete(run, "Rockmite", state)); assertTrue(complete(run, "Driftling", state))
        run.partyFullClear("Party > Friend: cd")
        assertFalse(complete(run, "Rockmite", state)); assertFalse(complete(run, "Snoozle", state))
        run.visitBiome(SafariBiome.CAVERN)
        run.reconcileStructures(SafariBiome.CAVERN, true, true)
        assertFalse(complete(run, "Rockmite", state)); assertFalse(complete(run, "Snoozle", state))
        run.sparklingChecks.scan(SafariBiome.CAVERN, 0.0, 0.0, emptyList(), moundsCleared = true, wallsCleared = true)
        run.reconcileStructures(SafariBiome.CAVERN, false, false)
        assertTrue(complete(run, "Rockmite", state)); assertTrue(complete(run, "Snoozle", state))
        assertTrue(complete(run, "Rockmite", party()))
    }
    @Test fun `Unique Forest retains only party-needed source checks`() {
        val run = SafariRun(0); val state = party("Honeybug", "Bluebird")
        run.visitBiome(SafariBiome.FOREST); caught(run, "Honeybug", "Bluebird", "Parakeet", "Macaw")
        assertFalse(complete(run, "Honeybug", state))
        run.updateNestEvidence(true, 3, 3, 0)
        assertTrue(complete(run, "Honeybug", state))
        assertFalse(complete(run, "Bluebird", state)); assertTrue(complete(run, "Parakeet", state))
        repeat(9) { run.birds.spawn("A Bluebird was attracted to the Birdfeeder!") }
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), nestsChecked = true, birdsSpawned = run.birds.sparklingSpawned)
        assertTrue(complete(run, "Bluebird", state))
        repeat(10) { run.birds.pickup("FLOOR DROP! Bag of Seeds", SafariBiome.FOREST) }
        run.sparklingChecks.scan(SafariBiome.FOREST, 3.0, 47.0, emptyList(), nestsChecked = true, birdsSpawned = run.birds.used >= run.birds.feedingTarget)
        assertFalse(complete(run, "Bluebird", state))
    }
    @Test fun `Unique local completion announces once and never echoes remote completion`() {
        val run = SafariRun(0); val state = party()
        SafariBiome.CAVERN.critters.forEach { caught(run, it.name) }
        run.partyFullClear("Party > Friend: cd")
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
        run.visitBiome(SafariBiome.CAVERN); run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), true)
        assertEquals("cd", run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
        assertNull(run.completion.announcement(run, SafariBiome.CAVERN, SafariMode.UNIQUE, state))
    }
    @Test fun `every Unique species composes its catch with any required local sparkling check`() {
        val run = SafariRun(0); val state = party(*SafariRoster.all.map { it.name }.toTypedArray())
        (SafariBiome.ICY.critters + SafariBiome.HAUNTED.critters).forEach {
            caught(run, it.name)
            assertEquals(it.name in setOf("Wumpa", "Doomspiral"), complete(run, it.name, state), "Only the two bosses complete their sparkling check from the catch alone")
            assertTrue(complete(run, it.name, party()), "Owned discovery needs only the catch")
        }
    }
    @Test fun `Unique missing panel tracks captures while progress waits and party claims add no personal captures`() {
        net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = false
        net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = false
        val run = SafariRun(0); val state = party()
        run.record(SafariCatch(SafariRoster.named("Honeybug")!!, false))
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Honeybug" })
        run.partyFullClear("Party > Friend: fd")
        assertFalse(SafariPanels.missing(run, SafariBiome.FOREST, true, 0, party = state).rows.any { it.label == "Honeybug" })
        assertEquals(0, run.personalCount("Honeybug"))
        assertFalse(complete(run, "Macaw", state))
    }
    @Test fun `party fd overrides inherited and starting Forest while local evidence remains separate`() {
        val state = party("Honeybug", "Bluebird")
        val run = SafariRun(0)
        run.visitBiome(SafariBiome.CAVERN)
        caught(run, "Honeybug", "Bluebird", "Parakeet")
        run.partyFullClear("Party > Friend: fd")
        run.visitBiome(SafariBiome.FOREST)
        assertFalse(complete(run, "Honeybug", state))
        assertFalse(complete(run, "Bluebird", state))
        assertFalse(complete(run, "Macaw", state))
        assertFalse(run.uniqueComplete("Honeybug", state, localOnly = true))
        val starting = SafariRun(0)
        starting.visitBiome(SafariBiome.FOREST)
        caught(starting, "Honeybug", "Bluebird")
        starting.partyFullClear("Party > Friend: fd")
        assertFalse(complete(starting, "Honeybug", state))
        assertFalse(complete(starting, "Bluebird", state))
        assertFalse(starting.uniqueComplete("Honeybug", state, localOnly = true))
    }
    @Test fun `inherited Cavern trusts done until remaining structures contradict it`() {
        val run = SafariRun(0); val state = party("Rockmite", "Snoozle")
        run.visitBiome(SafariBiome.FOREST)
        caught(run, "Rockmite", "Snoozle")
        run.partyFullClear("Party > Friend: cd")
        run.visitBiome(SafariBiome.CAVERN)
        assertFalse(complete(run, "Rockmite", state)); assertFalse(complete(run, "Snoozle", state))
        run.reconcileStructures(SafariBiome.CAVERN, true, false)
        assertFalse(complete(run, "Rockmite", state)); assertFalse(complete(run, "Snoozle", state))
        net.johnceo.sparklingmutuals.config.ConfigManager.fullClearMode = false
        net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode = false
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, true, 0, mounds = 2, party = state).rows
        assertTrue(rows.any { it.label == "Rockmite" })
        assertTrue(rows.any { it.label == "Snoozle" })
        assertTrue(rows.any { it.label == "Mounds to break" })
    }
    @Test fun `Unique Icy and Haunted announce done when local conditions are met`() {
        for (biome in listOf(SafariBiome.ICY, SafariBiome.HAUNTED)) {
            val run = SafariRun(0)
            run.visitBiome(biome)
            biome.critters.forEach { caught(run, it.name) }
            run.updateCaptureEvidence(biome, emptySet(), true)
            assertEquals(if (biome == SafariBiome.ICY) "id" else "hd", run.completion.announcement(run, biome, SafariMode.UNIQUE, party()))
        }
    }
}
