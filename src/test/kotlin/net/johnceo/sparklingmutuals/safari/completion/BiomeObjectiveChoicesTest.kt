package net.johnceo.sparklingmutuals.safari.completion

import java.util.Properties
import net.johnceo.sparklingmutuals.commands.SafariMissingReply
import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.config.SettingsCatalog
import net.johnceo.sparklingmutuals.config.SettingsTarget
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.structures.WallState
import net.johnceo.sparklingmutuals.safari.structures.WallSummary
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import net.johnceo.sparklingmutuals.sync.CritterProgressProtocol
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BiomeObjectiveChoicesTest {
    private fun party(vararg needed: String) = PartySparklingState().apply {
        val all = SafariRoster.all.map { it.name }.toSet()
        val members = setOf("me", "teammate")
        select(members)
        accept(members, mapOf("me" to all, "teammate" to (all - needed.toSet())))
    }

    private fun withChoices(vararg disabled: String, test: () -> Unit) {
        val old = Properties().also(BiomeObjectiveConfig::save)
        val choices = Properties().apply { disabled.forEach { setProperty(it, "false") } }
        try {
            BiomeObjectiveConfig.load(choices)
            test()
        } finally {
            BiomeObjectiveConfig.load(old)
        }
    }

    @Test fun skippingRockmiteStillRequiresOneUniqueAndNeverExportsUncheckedProof() =
        withChoices("objectives.checks.rockmite") {
            val biome = SafariBiome.CAVERN
            val state = party("Rockmite")
            val run = SafariRun(0).apply {
                visitBiome(biome)
                biome.critters.filter { it.name != "Rockmite" }.forEach { record(SafariCatch(it)) }
                updateCaptureEvidence(biome, setOf("Rockmite", "Rockmite Mound"), allWallsChecked = true)
                observeCavernSources(moundsRemain = true, wallsRemain = false, allWallsBroken = true)
            }
            assertTrue(state.needs("Rockmite"))
            assertTrue(SafariEspRules.neededForRun("Rockmite", true, run, false, party = state))
            assertTrue(SafariHelperRules.highlightSource("Rockmite", SafariMode.UNIQUE, run, state))
            assertFalse(SafariEspRules.neededForSparkling("Rockmite", true, state, false, run))
            assertNull(run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            assertEquals(listOf("Uniques left to capture: Rockmite"),
                SafariMissingReply.requirements(run, biome, SafariMode.UNIQUE, state, 3, "0"))

            run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
            assertFalse(SafariEspRules.neededForRun("Rockmite", true, run, false, party = state))
            assertFalse(SafariHelperRules.highlightSource("Rockmite", SafariMode.UNIQUE, run, state))
            assertFalse(run.captureComplete("Rockmite"))
            assertEquals("cd", run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            assertEquals(listOf("Uniques left to capture: None"),
                SafariMissingReply.requirements(run, biome, SafariMode.UNIQUE, state, 3, "0"))
            assertTrue(SafariMissingReply.requirements(run, biome, SafariMode.SPARKLING, state, 3, "0").isEmpty())
            val evidence = CritterProgressProtocol.decode(CritterProgressProtocol.encode(run, state))!!.getValue("Rockmite")
            assertEquals(1, evidence.captures)
            assertFalse(evidence.checked)
            assertFalse(evidence.fullClear)
        }

    @Test fun hidingARowDoesNotSkipItsSparklingCheck() = withChoices("objectives.rows.mounds") {
        val wasFullClear = ConfigManager.fullClearMode
        val wasSparkling = ConfigManager.sparklingMode
        try {
            ConfigManager.fullClearMode = false
            ConfigManager.sparklingMode = false
            val biome = SafariBiome.CAVERN
            val run = SafariRun(0)
            val state = party("Rockmite", "Snoozle")
            val walls = WallSummary(listOf(WallState.INTACT))
            val rows = SafariPanels.objectives(run, biome, true, 0, 3, walls, state).rows.map { it.label }
            assertFalse("Mounds" in rows)
            assertTrue("Snoozle walls" in rows)
            assertTrue(SafariHelperRules.highlightSource("Rockmite", SafariMode.UNIQUE, run, state))
            assertTrue("Mounds: 3" in SafariMissingReply.requirements(run, biome, SafariMode.SPARKLING, state, 3, "1"))
        } finally {
            ConfigManager.fullClearMode = wasFullClear
            ConfigManager.sparklingMode = wasSparkling
        }
    }

    @Test fun skippedBirdCheckDoesNotRequireUnusedFoodAfterAllBirdUniques() =
        withChoices("objectives.checks.macaw") {
            val biome = SafariBiome.FOREST
            val run = SafariRun(0).apply {
                visitBiome(biome)
                biome.critters.filter { it.name != "Macaw" }.forEach { record(SafariCatch(it)) }
                updateCaptureEvidence(biome, emptySet(), allWallsChecked = true)
            }
            val state = party("Macaw")
            assertFalse(run.canReportBiome(biome, state, SafariMode.UNIQUE))
            run.record(SafariCatch(SafariRoster.named("Macaw")!!))
            assertTrue(run.canReportBiome(biome, state, SafariMode.UNIQUE))
            assertFalse(run.canReportBiome(biome, state, SafariMode.FULL_CLEAR))
            assertEquals("fd", run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            assertFalse(run.uniqueComplete("Macaw", state, localOnly = true, respectObjectiveChoices = false))
        }

    @Test fun objectiveControlsRemainAccessibleWithTheHudOffAndSaveIndependently() = withChoices {
        val settings = SafariSettings().apply { tracking.objectivesHud.enabled = false }
        val catalog = SettingsCatalog(settings)
        fun row(section: String, key: String) =
            catalog.locate(SettingsTarget("HUDs", listOf("objectivesHud", section, key)))!!.second.last()
        val moundRow = row("rows", "MOUNDS")
        val rockmite = row("checks", "Rockmite")
        assertEquals("Rockmite mounds", moundRow.name)
        rockmite.field.setBoolean(rockmite.owner, false)
        assertTrue(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, settings.tracking.objectivesHud.checks))
        val props = Properties().also(BiomeObjectiveConfig::save)
        assertEquals("true", props.getProperty("objectives.rows.mounds"))
        assertEquals("false", props.getProperty("objectives.checks.rockmite"))
    }
}
