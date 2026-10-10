package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariSettingsTest {
    @TempDir lateinit var dir: Path
    @Test fun `MoulConfig view applies and persists toggles and independent warnings`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val settings = SafariSettings()
        settings.miria.three = false
        settings.safari.paintings = false
        settings.safari.shiny = false
        settings.party.enabled = false
        settings.warp.enabled = true
        settings.apply()
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertFalse(ConfigManager.hideHauntedPaintings)
        assertFalse(ConfigManager.shinyDetection)
        assertFalse(ConfigManager.partyCommandsEnabled)
        assertTrue(ConfigManager.warpAlertsEnabled)
        assertEquals(listOf(5, 1), ContestConfig.warningMinutes())
        settings.miria.five = false
        settings.miria.one = false
        settings.apply()
        ContestConfig.init(dir)
        assertTrue(ContestConfig.warningMinutes().isEmpty())
    }
    @Test fun `Safari palette is scoped to our settings rendering`() {
        val libraryGrey = 0xFF202026.toInt()
        assertEquals(libraryGrey, SafariTheme.color(libraryGrey))
        SafariTheme.begin()
        try { assertEquals(0xFF352D21.toInt(), SafariTheme.color(libraryGrey)) } finally { SafariTheme.end() }
        assertEquals(libraryGrey, SafariTheme.color(libraryGrey))
    }
    @Test fun `bundled 0_9 area table identifies Haunted and rejects outside coordinates`() {
        assertEquals(4, SafariAreaMap.biomeAt(-4.0, 71.0, -79.0))
        assertNull(SafariAreaMap.biomeAt(10000.0, 71.0, 10000.0))
    }
}
