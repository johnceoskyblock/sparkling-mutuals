package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Path
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
import org.junit.jupiter.api.io.TempDir

class SafariModeHelpersTest {
    @TempDir lateinit var dir: Path
    private fun caught(name: String) = SafariRun(0).apply { record(SafariCatch(SafariRoster.named(name)!!)) }
    private fun party(discoveries: Set<String>) = PartySparklingState().apply {
        select(setOf("one", "two")); accept(setOf("one", "two"), mapOf("one" to discoveries, "two" to discoveries))
    }
    @Test fun `unique structure checks survive captures while any party member needs a sparkling`() {
        val state = party(emptySet())
        for (name in listOf("Honeybug", "Rockmite", "Snoozle")) {
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, caught(name), state))
            assertFalse(SafariEspRules.neededForRun(name, false, caught(name), false, false, state))
        }
    }
    @Test fun `confirmed party discoveries stop unique checks after the first catch`() {
        val state = party(setOf("HONEYBUG", "ROCKMITE", "SNOOZLE"))
        for (name in listOf("Honeybug", "Rockmite", "Snoozle")) {
            assertTrue(SafariHelperRules.needed(name, SafariMode.UNIQUE, SafariRun(0), state))
            assertFalse(SafariHelperRules.needed(name, SafariMode.UNIQUE, caught(name), state))
        }
        assertFalse(SafariEspRules.neededForRun("Rockmite", true, caught("Rockmite"), false, false, state))
        assertFalse(SafariEspRules.neededForRun("Rockmite", false, caught("Rockmite"), false, false, state))
    }
    @Test fun `unknown or partial discoveries cannot disable unique shiny checks`() {
        val state = PartySparklingState().apply { select(setOf("one", "two")) }
        assertFalse(state.accept(setOf("one", "two"), mapOf("one" to setOf("HONEYBUG"))))
        assertTrue(SafariHelperRules.needed("Honeybug", SafariMode.UNIQUE, caught("Honeybug"), state))
    }
    @Test fun `sparkling mode excludes both Rockmite forms when everyone has the discovery`() {
        val state = party(setOf("ROCKMITE", "SNOOZLE"))
        assertFalse(SafariHelperRules.needed("Snoozle", SafariMode.SPARKLING, SafariRun(0), state))
        assertFalse(SafariEspRules.neededForSparkling("Rockmite", true, state, true))
        assertFalse(SafariEspRules.neededForSparkling("Rockmite", false, state, true))
        assertTrue(SafariHelperRules.needed("Rockmite", SafariMode.FULL_CLEAR, caught("Rockmite"), state))
    }
    @Test fun `full clear missing list requires minimum captures and no loaded species`() {
        ConfigManager.init(dir); ConfigManager.fullClearMode = true
        val run = caught("Driftling").apply { repeat(2) { record(SafariCatch(SafariRoster.named("Driftling")!!)) } }
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling"), false)
        assertTrue(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "Driftling" })
        assertTrue(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "Scrappy" })
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false, playerX = -120.0, playerZ = 55.0)
        run.partyFullClear("Party > Friend: cd")
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "Driftling" })
        run.updateCaptureEvidence(SafariBiome.CAVERN, setOf("Driftling", "Scrappy"), false)
        val panel = SafariPanels.missing(run, SafariBiome.CAVERN, false, 0)
        assertTrue(panel.rows.any { it.label == "Driftling" })
        assertEquals("0/3 captured", panel.rows.single { it.label == "Scrappy" }.value)
    }
}
