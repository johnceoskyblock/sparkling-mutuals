package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.resources.FloorDropState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniqueHelperPriorityTest {
    @Test fun `Gemzie completion hides remaining gems in every mode even with manual ESP enabled`() {
        for (mode in SafariMode.entries) {
            var now = 1000L
            val run = SafariRun(0) { now }
            val state = FloorDropState().apply { visit(SafariBiome.CAVERN); force(true) }
            val needs = party("Gemzie")
            assertTrue(state.enabledForMode(SafariBiome.CAVERN, mode, true, run, needs))
            when (mode) {
                SafariMode.UNIQUE -> run.record(SafariCatch(SafariRoster.named("Gemzie")!!))
                SafariMode.FULL_CLEAR -> repeat(3) { run.record(SafariCatch(SafariRoster.named("Gemzie")!!)) }
                SafariMode.SPARKLING -> {
                    run.sparklingChecks.chat("A rumbling sound can be heard, and the door at the back of the chamber opens...")
                    now = 6000
                }
            }
            assertEquals(1, run.completed(mode, listOf(SafariRoster.named("Gemzie")!!), needs))
            assertFalse(state.enabledForMode(SafariBiome.CAVERN, mode, true, run, needs))
            state.visit(SafariBiome.FOREST)
            assertFalse(state.enabledForMode(SafariBiome.CAVERN, mode, true, run, needs))
        }
    }
    private fun party(vararg missing: String) = PartySparklingState().apply {
        select(setOf("me")); accept(setOf("me"), mapOf("me" to SafariRoster.all.map { it.name }.toSet() - missing.toSet()))
    }
    @Test fun `unique gem collection precedes party discoveries and then respects pickup evidence`() {
        val run = SafariRun(0); val state = FloorDropState(); val all = party()
        assertTrue(state.enabledForMode(SafariBiome.CAVERN, SafariMode.UNIQUE, true, run, all))
        assertFalse(state.enabledForMode(SafariBiome.CAVERN, SafariMode.SPARKLING, true, run, all))
        for (gem in listOf("Purple Gem", "Lime Gem", "Orange Gem")) state.record("FLOOR DROP! $gem", SafariBiome.CAVERN)
        assertFalse(state.enabledForMode(SafariBiome.CAVERN, SafariMode.UNIQUE, true, run, all))
        state.reset(); run.record(SafariCatch(SafariRoster.named("Gemzie")!!))
        assertFalse(state.enabledForMode(SafariBiome.CAVERN, SafariMode.UNIQUE, true, run, all))
        assertTrue(state.enabledForMode(SafariBiome.CAVERN, SafariMode.FULL_CLEAR, true, run, all))
    }
    @Test fun `unique bird supplies precede captures while remaining coins stay enabled`() {
        val run = SafariRun(0); val state = FloorDropState(); val all = party()
        val needsCoin = party("Gimmiegold")
        assertTrue(state.enabledForMode(SafariBiome.FOREST, SafariMode.UNIQUE, true, run, all))
        state.inventory(listOf("Soothing Incense" to 4))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, all))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, needsCoin))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, false, run, needsCoin))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, needsCoin))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, all))
        repeat(2) { state.record("FLOOR DROP! Shining Coin", SafariBiome.HAUNTED) }
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, needsCoin))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, all))
    }
    @Test fun `structure helpers prioritize first catch then remaining party checks`() {
        for (name in SafariHelperRules.species) {
            val run = SafariRun(0); val needs = party(name); val all = party()
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, all))
            run.record(SafariCatch(SafariRoster.named(name)!!))
            assertFalse(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, all))
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, needs))
            run.sparklingChecks.chat("Party > Friend: " + if (name == "Honeybug") "fd" else "cd", manualAllowed = true)
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, needs))
            assertTrue(SafariHelperRules.needed(name, SafariMode.FULL_CLEAR, run, all))
        }
    }
}
