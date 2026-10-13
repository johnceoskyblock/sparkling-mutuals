package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UniqueHelperPriorityTest {
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
    @Test fun `unique bird and coin supplies are not hidden by party sparkling discoveries before captures`() {
        val run = SafariRun(0); val state = FloorDropState(); val all = party()
        assertTrue(state.enabledForMode(SafariBiome.FOREST, SafariMode.UNIQUE, true, run, all))
        state.inventory(listOf("Soothing Incense" to 4))
        assertTrue(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, all))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, false, run, all))
        run.record(SafariCatch(SafariRoster.named("Gimmiegold")!!))
        assertFalse(state.enabledForMode(SafariBiome.HAUNTED, SafariMode.UNIQUE, true, run, all))
    }
    @Test fun `structure helpers prioritize first catch then remaining party checks`() {
        for (name in SafariHelperRules.species) {
            val run = SafariRun(0); val needs = party(name); val all = party()
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, all))
            run.record(SafariCatch(SafariRoster.named(name)!!))
            assertFalse(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, all))
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, needs))
            run.sparklingChecks.chat("Party > Friend: " + if (name == "Honeybug") "fd" else "cd", manualAllowed = true)
            assertFalse(SafariHelperRules.needed(name, SafariMode.UNIQUE, run, needs))
            assertTrue(SafariHelperRules.needed(name, SafariMode.FULL_CLEAR, run, all))
        }
    }
}
