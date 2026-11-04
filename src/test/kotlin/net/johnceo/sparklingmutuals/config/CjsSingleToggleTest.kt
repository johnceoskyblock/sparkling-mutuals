package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CjsSingleToggleTest {
    @TempDir lateinit var root:Path
    private fun rows(rows:List<SettingsCatalog.Row>):List<SettingsCatalog.Row> = rows.flatMap { listOf(it)+rows(it.children) }
    @Test fun `one last warping switch saves full entry with a red warning`() {
        ConfigManager.init(root);ContestConfig.init(root)
        val settings=SafariSettings();val catalog=SettingsCatalog(settings)
        val warping=catalog.pages.single { it.name=="Sparkle Warping" }
        val switch=warping.rows.last()
        assertEquals("Sparkling Auto-entry",switch.name)
        assertFalse(switch.read() as Boolean)
        assertTrue(switch.description.lines().contains("USE AT YOUR OWN RISK!!!!"))
        assertEquals(0xFF5555, switch.field.getAnnotation(DescriptionColor::class.java).rgb)
        assertNull(warping.rows.first().field.getAnnotation(DescriptionColor::class.java))
        val all=catalog.pages.flatMap { rows(it.rows) }
        assertEquals(1,all.count { it.field.name=="autoEnter" })
        assertFalse(all.any { it.field.name in setOf("dryRun","approach") })
        switch.write(true);settings.apply();ConfigManager.init(root)
        assertTrue(ConfigManager.safariEntryAutoEnter)
        val restored=SafariSettings();restored.warp.autoEnter=false;restored.apply();ConfigManager.init(root)
        assertFalse(ConfigManager.safariEntryAutoEnter)
    }
    @Test fun `legacy report and approach choices cannot activate or block the single switch`() {
        Files.writeString(root.resolve("sparkling-mutuals.properties"),
            "safariEntryDryRun=true\nsafariEntryApproach=true\nautoSelectSafariTicket=true\n")
        ConfigManager.init(root);ContestConfig.init(root)
        assertFalse(ConfigManager.safariEntryAutoEnter)
        Files.writeString(root.resolve("sparkling-mutuals.properties"),
            "safariEntryDryRun=false\nsafariEntryApproach=false\nsafariEntryAutoEnter=true\nautoSelectSafariTicket=false\n")
        ConfigManager.init(root)
        assertTrue(ConfigManager.safariEntryAutoEnter)
        val s=SafariSettings();assertTrue(s.warp.autoEnter)
        s.apply();ConfigManager.init(root);assertTrue(ConfigManager.safariEntryAutoEnter)
        assertFalse(ConfigManager.autoSelectSafariTicket)
        SafariSettings().apply { warp.autoEnter=false }.apply() // A real edit saves and drops obsolete keys.
        val saved=Files.readString(root.resolve("sparkling-mutuals.properties"))
        assertFalse(saved.contains("safariEntryDryRun="));assertFalse(saved.contains("safariEntryApproach="))
    }
}
