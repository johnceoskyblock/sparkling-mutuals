package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.hud.SafariHud
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigPresentationTest {
    @TempDir lateinit var dir: Path

    @Test fun allPlayerHudLinksStillResolveAfterRenamingAndOrdering() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        for(hud in SafariHud.entries.filter {it.editionEnabled && it!=SafariHud.DEVELOPER}) {
            val target=HudConfigLinks.target(hud)
            assertNotNull(catalog.locate(target),hud.name)
            assertTrue(catalog.expansionFor(target).isNotEmpty(),hud.name)
        }
        assertNotNull(catalog.locate(HudConfigLinks.contest))
    }

    @Test fun contentControlsStayAheadOfAppearanceAndPlacement() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        val roots=catalog.pages.single {it.name=="HUDs"}.rows
        for(root in roots.filter {it.owner is SafariSettings.Tracking && it.group}) {
            val fields=root.children.map {it.navigationName}
            if("enabled" in fields)assertEquals("enabled",fields.first(),root.name)
            val appearance=fields.indexOf("appearance")
            assertTrue(appearance>=0,root.name)
            assertTrue(fields.take(appearance).none {it=="placement"},root.name)
            assertEquals("placement",fields.last(),root.name)
        }
    }

    @Test fun searchUsesVisibleEspTabsAndRetainsIndependentBindings() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        val rows=catalog.search("Always on critters Hideyho")
        assertEquals(1,rows.size)
        assertTrue(rows.single().owner is CritterToggleSettings)
        val settings=SafariSettings();val updated=SettingsCatalog(settings)
        val row=updated.search("Always on critters Hideyho").single()
        row.write(false);settings.apply();ConfigManager.init(dir)
        assertFalse(SafariEspConfig.alwaysOn("Hideyho"))
        assertTrue(SafariEspConfig.modWideEnabled("Hideyho"))
        assertTrue(SafariEspConfig.profitableToggles.getValue("Hideyho"))
    }

    @Test fun bannerTemplateHelpOnlyAppearsWhereThePlaceholderWorks() {
        ConfigManager.init(dir)
        val catalog=SettingsCatalog(SafariSettings())
        fun text(vararg fields: String)=catalog.locate(SettingsTarget("Banner",fields.toList()+"text"))!!.second.last()
        assertTrue(text("encounters","hideyho").description.contains("<LOCATION>"))
        assertTrue(text("safari","biome").description.contains("<BIOME>"))
        assertEquals("",text("safari","complete").description)
    }
}
