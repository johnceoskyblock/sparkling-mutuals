package net.johnceo.sparklingmutuals.safari.completion

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.commands.SafariMissingReply
import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.FreshInstallDefaults
import net.johnceo.sparklingmutuals.config.SafariEspConfig
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
import org.junit.jupiter.api.io.TempDir

class BiomeObjectiveChoicesTest {
    private fun party(vararg needed: String) = PartySparklingState().apply {
        val all = SafariRoster.all.map { it.name }.toSet()
        val members = setOf("me", "teammate")
        select(members)
        accept(members, mapOf("me" to all, "teammate" to (all - needed.toSet())))
    }

    private fun withChoices(vararg disabled: String, test: () -> Unit) {
        val old = Properties().also(BiomeObjectiveConfig::save)
        val choices = Properties().apply {
            BiomeObjectiveConfig.optionalSpecies.forEach { setProperty("objectives.checks.${it.lowercase()}", "true") }
            disabled.forEach { setProperty(it, "false") }
        }
        try {
            BiomeObjectiveConfig.load(choices)
            test()
        } finally {
            BiomeObjectiveConfig.load(old)
        }
    }

    @Test fun optionalChecksDefaultOffAndExplicitOptInsSurviveReload() {
        val old = Properties().also(BiomeObjectiveConfig::save)
        try {
            val defaults = FreshInstallDefaults.main()
            BiomeObjectiveConfig.load(defaults)
            assertTrue(BiomeObjectiveConfig.optionalSpecies.isNotEmpty())
            assertTrue(BiomeObjectiveConfig.optionalSpecies.none(BiomeObjectiveConfig::check))
            assertTrue(BiomeObjectiveConfig.check("Foxtrot"))

            defaults.setProperty("objectives.checks.rockmite", "true")
            BiomeObjectiveConfig.load(defaults)
            val saved = Properties().also(BiomeObjectiveConfig::save)
            BiomeObjectiveConfig.load(saved)
            assertEquals(listOf("Rockmite"), BiomeObjectiveConfig.optionalSpecies.filter(BiomeObjectiveConfig::check))
        } finally {
            BiomeObjectiveConfig.load(old)
        }
    }

    @Test fun `an open settings draft picks up party check changes and keeps local edits`() {
        withChoices("objectives.checks.doomspiral", "objectives.checks.macaw") {
            val settings = SafariSettings()
            val draft = settings.modes.sparklingChecks
            assertFalse(draft.enabled("Doomspiral"))
            assertTrue(BiomeObjectiveConfig.setCheck("Doomspiral", true))
            assertFalse(BiomeObjectiveConfig.setCheck("Doomspiral", true))
            assertFalse(BiomeObjectiveConfig.setCheck("Foxtrot", false))
            draft.entries.single { it.key == "Macaw" }.enabled = true
            assertTrue(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, draft))
            assertTrue(draft.enabled("Doomspiral"))
            assertTrue(BiomeObjectiveConfig.check("Macaw"))

            assertTrue(BiomeObjectiveConfig.setCheck("Doomspiral", false))
            BiomeObjectiveConfig.load(Properties().also(BiomeObjectiveConfig::save))
            assertFalse(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, draft))
            assertFalse(draft.enabled("Doomspiral"))
            assertTrue(BiomeObjectiveConfig.check("Macaw"))
            assertTrue(BiomeObjectiveConfig.check("Foxtrot"))
        }
    }

    @Test fun upgradeTurnsOffEveryOptionalCheckOnce(@TempDir directory: Path) {
        val old = Properties().also(BiomeObjectiveConfig::save)
        try {
            val file = directory.resolve("sparkling-mutuals.properties")
            val previous = Properties().apply {
                setProperty("migration.shardSaleGuidanceV2", "true")
                setProperty("migration.sparkleWarpTimers22_27", "true")
                setProperty("objectives.rows.mounds", "false")
                BiomeObjectiveConfig.optionalSpecies.forEach { setProperty("objectives.checks.${it.lowercase()}", "true") }
            }
            Files.newOutputStream(file).use { previous.store(it, null) }

            ConfigManager.init(directory)
            assertTrue(BiomeObjectiveConfig.optionalSpecies.none(BiomeObjectiveConfig::check))
            val migrated = Properties().also { Files.newInputStream(file).use(it::load) }
            assertEquals("true", migrated.getProperty(BiomeObjectiveConfig.OPTIONAL_CHECKS_MIGRATION))
            assertEquals("false", migrated.getProperty("objectives.rows.mounds"))

            val settings = SafariSettings()
            settings.modes.sparklingChecks.entries.single { it.key == "Rockmite" }.enabled = true
            assertTrue(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, settings.modes.sparklingChecks))
            ConfigManager.saveSettings()
            ConfigManager.init(directory)
            ConfigManager.init(directory, "4.4.6")
            assertEquals(listOf("Rockmite"), BiomeObjectiveConfig.optionalSpecies.filter(BiomeObjectiveConfig::check))
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
            assertTrue(SafariEspRules.neededForSparkling("Rockmite", true, state, false, run))
            assertNull(run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            assertEquals(listOf("Uniques left to capture: Rockmite"),
                SafariMissingReply.requirements(run, biome, SafariMode.UNIQUE, state, 3, "0"))

            run.record(SafariCatch(SafariRoster.named("Rockmite")!!))
            assertFalse(SafariEspRules.neededForRun("Rockmite", true, run, false, party = state))
            assertFalse(SafariHelperRules.highlightSource("Rockmite", SafariMode.UNIQUE, run, state))
            assertFalse(run.captureComplete("Rockmite"))
            assertNull(run.completion.announcement(run, biome, SafariMode.SPARKLING, state))
            assertEquals("cd", run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            assertEquals(listOf("Uniques left to capture: None"),
                SafariMissingReply.requirements(run, biome, SafariMode.UNIQUE, state, 3, "0"))
            assertEquals(listOf("Mounds: 3"),
                SafariMissingReply.requirements(run, biome, SafariMode.SPARKLING, state, 3, "0"))
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

    @Test fun postClearSourcesAndObjectivesDoNotReopenUniqueProgress(@TempDir directory: Path) {
        ConfigManager.init(directory)
        ConfigManager.fullClearMode = false
        ConfigManager.sparklingMode = false
        ConfigManager.profitableShardEsp = false
        val biome = SafariBiome.CAVERN
        val state = party("Rockmite", "Snoozle", "Shyworm")
        val walls = WallSummary(listOf(WallState.INTACT))
        val run = SafariRun(0).apply {
            visitBiome(biome)
            biome.critters.forEach { record(SafariCatch(it)) }
            updateCaptureEvidence(biome, setOf("Rockmite Mound"), allWallsChecked = false)
            observeCavernSources(moundsRemain = true, wallsRemain = true, allWallsBroken = false)
        }
        fun objectiveLabels() = SafariPanels.objectives(run, biome, true, 0, 3, walls, state).rows.map { it.label }
        fun progress() = SafariPanels.progress(run, true, 0, state).rows
        val before = progress()
        assertEquals(listOf("All objectives complete!"), objectiveLabels())
        assertEquals("cd", run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
        run.completion.sent(biome, SafariMode.UNIQUE)
        try {
            SafariEspConfig.uniqueRemainingChecks = true
            assertEquals(before, progress())
            assertTrue(SafariPanels.missingCritters(run, biome, state).isEmpty())
            assertNull(run.completion.announcement(run, biome, SafariMode.UNIQUE, state))
            val after = !SafariSettings.standardEdition
            assertEquals("All captured", SafariPanels.missing(run, biome, true, 0, party = state).rows.single().label)
            assertEquals(after, "Mounds" in objectiveLabels())
            assertEquals(after, "Snoozle walls" in objectiveLabels())
            assertEquals(!after, "Check Rockmite" in objectiveLabels())
            assertEquals(!after, "Check Snoozle" in objectiveLabels())
            assertEquals(after, SafariHelperRules.highlightSource("Rockmite", SafariMode.UNIQUE, run, state))
            for (name in listOf("Rockmite", "Snoozle"))
                assertFalse(SafariEspRules.neededForRun(name, false, run, false, false, state))
        } finally {
            SafariEspConfig.uniqueRemainingChecks = false
        }
    }

    @Test fun `remaining checks use one saved setting in both editions and refresh open settings`(@TempDir directory: Path) {
        ConfigManager.init(directory)
        val settings = SafariSettings()
        assertFalse(settings.modes.unique.remainingChecks)
        assertTrue(SafariEspConfig.setRemainingChecks(true))
        ConfigManager.saveSettings()
        settings.apply()
        assertTrue(settings.modes.unique.remainingChecks)
        assertNotNull(SettingsCatalog(settings).locate(SettingsTarget("Modes", listOf("unique", "remainingChecks"))))
        ConfigManager.init(directory)
        assertTrue(SafariSettings().modes.unique.remainingChecks)
        settings.modes.unique.remainingChecks = false
        settings.apply()
        ConfigManager.init(directory)
        assertFalse(SafariSettings().modes.unique.remainingChecks)
    }

    @Test fun biomeBannerAndReportWaitForTheSameOptionalChecks() = withChoices {
        val biome = SafariBiome.CAVERN
        val state = party("Gemzie")
        val run = SafariRun(0).apply {
            visitBiome(biome)
            biome.critters.forEach { record(SafariCatch(it)) }
            updateCaptureEvidence(biome, emptySet(), allWallsChecked = true)
        }
        val mode = SafariMode.UNIQUE
        assertEquals(biome.critters.size, run.completed(mode, biome.critters, state, localOnly = true))
        assertFalse(run.completion.reportReady(run, biome, mode, state))
        assertNull(run.completion.announcement(run, biome, mode, state))
        val savedStyles = java.util.Properties().also { SafariEspConfig.save(it, !SafariSettings.standardEdition) }
        try {
            SafariEspConfig.glowToggles["Gemzie"] = false
            SafariEspConfig.boxToggles["Gemzie"] = false
            SafariEspConfig.standardCritterGlow = false
            SafariEspConfig.standardCritterBoxes = false
            assertFalse(SafariEspConfig.styledHighlight("Gemzie"))
            assertFalse(run.completion.reportReady(run, biome, mode, state))
        } finally {
            SafariEspConfig.load(savedStyles, !SafariSettings.standardEdition)
        }

        val settings = SafariSettings()
        settings.modes.sparklingChecks.entries.single { it.key == "Gemzie" }.enabled = false
        assertTrue(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, settings.modes.sparklingChecks))
        assertTrue(run.completion.reportReady(run, biome, mode, state))
        assertEquals("cd", run.completion.announcement(run, biome, mode, state))
        assertFalse(run.completion.reportReady(run, biome, SafariMode.SPARKLING, state))
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

    @Test fun modeChecksRemainAccessibleWithTheHudOffAndSaveIndependently() = withChoices {
        val settings = SafariSettings().apply { tracking.objectivesHud.enabled = false }
        val catalog = SettingsCatalog(settings)
        fun row(section: String, key: String) =
            catalog.locate(SettingsTarget("HUDs", listOf("objectivesHud", section, key)))!!.second.last()
        val moundRow = row("rows", "MOUNDS")
        val rockmite = catalog.locate(SettingsTarget("Modes", listOf("sparklingChecks", "Rockmite")))!!.second.last()
        assertEquals("Rockmite mounds", moundRow.name)
        rockmite.field.setBoolean(rockmite.owner, false)
        assertTrue(BiomeObjectiveConfig.apply(settings.tracking.objectivesHud.rows, settings.modes.sparklingChecks))
        val props = Properties().also(BiomeObjectiveConfig::save)
        assertEquals("true", props.getProperty("objectives.rows.mounds"))
        assertEquals("false", props.getProperty("objectives.checks.rockmite"))
    }
}
