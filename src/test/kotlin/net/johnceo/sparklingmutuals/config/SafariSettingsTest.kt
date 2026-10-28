package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.safari.SafariAreaMap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariSettingsTest {
    @TempDir lateinit var dir: Path
    @Test fun `MoulConfig view applies and persists toggles and independent warnings`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertEquals(0, ContestConfig.ticketMode)
        val settings = SafariSettings()
        assertFalse(settings.party.startingItems)
        settings.party.startingItems = true
        settings.esp.teammateColor = "0:255:100:80:200"
        assertFalse(settings.modes.unique.hotspot)
        assertFalse(settings.modes.fullClear.hotspot)
        assertFalse(settings.modes.sparkling.hotspot)
        settings.modes.unique.hotspot = true
        settings.modes.sparkling.hotspot = true
        settings.miria.ticketMode = 1
        settings.miria.three = false
        settings.safari.paintings = false
        settings.safari.shiny = false
        settings.party.enabled = false
        settings.warp.enabled = true
        settings.apply()
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        assertTrue(ContestConfig.firstClassTicket)
        assertTrue(ConfigManager.announceStartingItems)
        assertEquals("0:255:100:80:200", SafariEspConfig.teammateColor)
        assertEquals(0xFF6450C8.toInt(), net.johnceo.sparklingmutuals.safari.esp.TeammateEsp.COLOR)
        assertTrue(ConfigManager.uniqueHotspotAnnouncer)
        assertFalse(ConfigManager.fullClearHotspotAnnouncer)
        assertTrue(ConfigManager.sparklingHotspotAnnouncer)
        assertFalse(ConfigManager.hideHauntedPaintings)
        assertFalse(ConfigManager.shinyDetection)
        assertFalse(ConfigManager.partyCommandsEnabled)
        assertTrue(ConfigManager.warpAlertsEnabled)
        assertEquals(listOf(1), ContestConfig.warningMinutes())
        settings.miria.five = false
        settings.miria.ticketMode = 0
        settings.miria.one = false
        settings.apply()
        ContestConfig.init(dir)
        assertFalse(ContestConfig.firstClassTicket)
        assertTrue(ContestConfig.warningMinutes().isEmpty())
    }
    @Test fun `Safari palette is scoped to our settings rendering`() {
        val libraryGrey = 0xFF202026.toInt()
        assertEquals(libraryGrey, SafariTheme.color(libraryGrey))
        SafariTheme.begin()
        try { assertEquals(0xFF19191E.toInt(), SafariTheme.color(libraryGrey)) } finally { SafariTheme.end() }
        assertEquals(libraryGrey, SafariTheme.color(libraryGrey))
    }
    @Test fun `bundled 0_9 area table identifies Haunted and rejects outside coordinates`() {
        assertEquals(4, SafariAreaMap.biomeAt(-4.0, 71.0, -79.0))
        assertNull(SafariAreaMap.biomeAt(10000.0, 71.0, 10000.0))
    }
}
