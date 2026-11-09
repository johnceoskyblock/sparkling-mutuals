package net.johnceo.sparklingmutuals.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsNavigationTest {
    @Test fun `collapsed HUD header toggle edits the existing setting without changing other HUDs`() {
        val settings=SafariSettings()
        val catalog=SettingsCatalog(settings)
        val row=catalog.locate(SettingsTarget("HUDs",listOf("progressHud")))!!.second.last()
        val other=settings.tracking.missingHud.enabled
        val before=settings.tracking.progressHud.enabled
        row.primaryToggle!!.write(!before)
        assertEquals(!before,settings.tracking.progressHud.enabled)
        assertEquals(other,settings.tracking.missingHud.enabled)
    }
    @Test fun `same label retains distinct mode locations and searches visible tab names`() {
        val catalog=SettingsCatalog(SafariSettings())
        val rows=catalog.search("hide critter ESP")
        assertTrue(rows.any { catalog.location(it)=="Safari → Modes → Unique mode" })
        assertTrue(rows.any { catalog.location(it)=="Safari → Modes → Full clear mode" })
        assertTrue(catalog.search("Safari Unique hide").any { it.name=="Hide critter ESP" })
        assertTrue(catalog.search("Advanced SkyBlock Endgame").any { it.name=="Phantomleaf Solver" })
    }
    @Test fun `search does not duplicate shared HUD appearance bindings`() {
        val catalog=SettingsCatalog(SafariSettings())
        val rows=catalog.search("progress")
        assertEquals(rows.size,rows.distinctBy { it.owner to it.field }.size)
        assertTrue(rows.any { catalog.location(it).startsWith("HUDs → HUDs → Progress HUD") })
    }
    @Test fun `right click opens owning HUD and its appearance without unrelated branches`() {
        val catalog=SettingsCatalog(SafariSettings())
        val paths=catalog.expansionFor(SettingsTarget("HUDs",listOf("progressHud","enabled")))
        assertTrue(paths.contains("HUDs / Progress HUD"))
        assertTrue(paths.contains("HUDs / Progress HUD / Appearance"))
        assertFalse(paths.any { it.contains("Missing / Unchecked") })
        assertTrue(catalog.expansionFor(SettingsTarget("Banner",listOf("enabled"))).contains("Banner / Appearance"))
        assertTrue(catalog.expansionFor(SettingsTarget("Banner",listOf("encounters","gems","enabled"))).contains("Banner / Appearance"))
    }
}
