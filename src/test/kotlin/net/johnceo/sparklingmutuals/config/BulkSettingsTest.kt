package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BulkSettingsTest {
    @TempDir lateinit var dir: Path

    @BeforeEach fun loadFreshSettings() {
        ConfigManager.init(dir, "4.4.2")
        ContestConfig.init(dir)
    }

    @Test fun `reset restores preferences and keeps saved Safari records byte for byte`() {
        val sampleFiles = listOf("sparkling-mutuals-runs.json", "sparkling-mutuals-pbs.properties",
            "sparkling-mutuals-sparklings.json", "sparkling-mutuals-achievements.json")
        for (file in sampleFiles) Files.writeString(dir.resolve(file), "Saved Safari records for " + file)
        val freshProgress = ConfigManager.progressHud
        val freshContestTimer = ContestConfig.showContestTimer
        ConfigManager.progressHud = !freshProgress
        ContestConfig.showContestTimer = !freshContestTimer
        ConfigManager.runCoinsInstaSell = true
        ConfigManager.profitsTicketMode = 2
        ConfigManager.hotspotAnnouncer = true
        ConfigManager.apiKey = "local-test-key"
        ConfigManager.saveSettings()
        ContestConfig.save()

        ConfigManager.resetSettings()

        assertEquals(freshProgress, ConfigManager.progressHud)
        assertEquals(freshContestTimer, ContestConfig.showContestTimer)
        assertTrue(ConfigManager.runCoinsInstaSell)
        assertEquals(2, ConfigManager.profitsTicketMode)
        assertTrue(ConfigManager.hotspotAnnouncer)
        assertEquals("local-test-key", ConfigManager.apiKey)
        assertEquals("4.4.2", ConfigManager.installedVersionHistory.installed)
        for (file in sampleFiles) assertEquals("Saved Safari records for " + file, Files.readString(dir.resolve(file)), file)
        val reset = SettingsCatalog(SafariSettings()).pages.single { it.name == "Reset" }
        assertTrue(reset.rows.first().description.contains("does not reset saved Safari records"))
    }

    @Test fun `disable clears every feature switch and persists across restart`() {
        net.minecraft.SharedConstants.tryDetectVersion()
        net.minecraft.server.Bootstrap.bootStrap()
        try {
            ConfigManager.runCoinsInstaSell = true
            ConfigManager.hotspotAnnouncer = true
            val settings = SafariSettings()
            SettingsCatalog(settings).disableSwitches()
            settings.apply()

            ConfigManager.init(dir, "4.4.2")
            ContestConfig.init(dir)
            assertTrue(ConfigManager.hotspotAnnouncer)
            fun check(rows: List<SettingsCatalog.Row>) {
                for (row in rows) {
                    if (row.field.isAnnotationPresent(ConfigEditorBoolean::class.java))
                        assertEquals(row.owner is SafariSettings.History && row.field.name == "instaSell", row.read(), row.path)
                    check(row.children)
                }
            }
            SettingsCatalog(SafariSettings()).pages.forEach { check(it.rows) }
        } finally {
            ConfigManager.resetSettings()
            ConfigManager.runCoinsInstaSell = false
            ConfigManager.hotspotAnnouncer = false
        }
    }

    @Test fun `progress rows can show just a count or leave just the timer`() {
        val settings = SafariSettings()
        val hud = settings.tracking.progressHud
        hud.rows.overall = ProgressRowDisplay.HIDDEN.ordinal
        hud.rows.forest = ProgressRowDisplay.COUNT_ONLY.ordinal
        hud.rows.icy = ProgressRowDisplay.HIDDEN.ordinal
        hud.profit = false
        hud.hotspot = false
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(ProgressRowDisplay.COUNT_ONLY, ConfigManager.progressRow(SafariBiome.FOREST))
        val now = 80_000L
        val run = SafariRun(now - 75_000L)
        val panel = SafariPanels.progress(run, true, now)
        assertEquals(listOf("Forest", "Cavern", "Haunted"), panel.rows.map { it.label })
        assertNull(panel.rows.first().fraction)
        assertNotNull(panel.rows[1].fraction)

        SafariSettings().also { edited ->
            edited.tracking.progressHud.rows.apply {
                forest = ProgressRowDisplay.HIDDEN.ordinal
                cavern = ProgressRowDisplay.HIDDEN.ordinal
                haunted = ProgressRowDisplay.HIDDEN.ordinal
            }
        }.apply()
        val timer = SafariPanels.progress(run, true, now)
        assertTrue(timer.rows.isEmpty())
        assertEquals("Critter Safari  1:15", timer.title)
    }
}
