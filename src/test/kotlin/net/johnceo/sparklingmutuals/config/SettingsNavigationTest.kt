package net.johnceo.sparklingmutuals.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsNavigationTest {
    @Test fun `entry cooldown lives in Safari helpers and HUD link exposes its customization`() {
        val catalog=SettingsCatalog(SafariSettings())
        val target=HudConfigLinks.target(net.johnceo.sparklingmutuals.hud.SafariHud.ENTRY_COOLDOWN)
        assertEquals(SettingsTarget("Safari helpers",listOf("entryCooldown","enabled")),target)
        val row=catalog.locate(target)!!.second.last()
        assertEquals("Safari → Helpers → Safari re-entry cooldown",catalog.location(row))
        assertTrue(catalog.search("Safari re-entry cooldown").contains(row))
        assertTrue(catalog.expansionFor(target).contains("Safari helpers / Safari re-entry cooldown / Appearance"))
    }
    @Test fun `HUD enabled control is inside its dropdown and changes only its existing setting`() {
        val settings=SafariSettings()
        val catalog=SettingsCatalog(settings)
        val row=catalog.locate(SettingsTarget("HUDs",listOf("progressHud")))!!.second.last()
        val other=settings.tracking.missingHud.enabled
        val before=settings.tracking.progressHud.enabled
        row.children.single {it.field.name=="enabled"}.write(!before)
        assertEquals(!before,settings.tracking.progressHud.enabled)
        assertEquals(other,settings.tracking.missingHud.enabled)
    }
    @Test fun `same label retains distinct mode locations and searches visible tab names`() {
        val catalog=SettingsCatalog(SafariSettings())
        val rows=catalog.search("hide critter ESP")
        assertTrue(rows.any { catalog.location(it)=="Safari → Modes → Unique mode" })
        assertTrue(rows.any { catalog.location(it)=="Safari → Modes → Full clear mode" })
        assertTrue(catalog.search("Safari Unique hide").any { it.name=="Hide critter ESP" })
        val solver = catalog.locate(SettingsTarget("SkyBlock", listOf("helpers", "phantomleafSolver")))!!.second.last()
        assertEquals("Misc → SkyBlock → Helpers", catalog.location(solver))
        assertTrue(catalog.search("Misc SkyBlock Helpers").contains(solver))
        assertNull(catalog.locate(SettingsTarget("SkyBlock", listOf("endgame"))))
    }
    @Test fun `search does not duplicate shared HUD appearance bindings`() {
        val catalog=SettingsCatalog(SafariSettings())
        val rows=catalog.search("progress")
        assertEquals(rows.size,rows.distinctBy { it.owner to it.field }.size)
        assertTrue(rows.any { catalog.location(it).startsWith("HUDs → HUDs → Run progress") })
    }
    @Test fun `right click opens owning HUD and its appearance without unrelated branches`() {
        val catalog=SettingsCatalog(SafariSettings())
        val paths=catalog.expansionFor(SettingsTarget("HUDs",listOf("progressHud","enabled")))
        assertTrue(paths.contains("HUDs / Run progress"))
        assertTrue(paths.contains("HUDs / Run progress / Appearance"))
        assertFalse(paths.any { it.contains("Missing / Unchecked") })
        assertTrue(catalog.expansionFor(SettingsTarget("Banner",listOf("enabled"))).contains("Banner / Appearance"))
        assertTrue(catalog.expansionFor(SettingsTarget("Banner",listOf("encounters","gems","enabled"))).contains("Banner / Appearance"))
    }
}
