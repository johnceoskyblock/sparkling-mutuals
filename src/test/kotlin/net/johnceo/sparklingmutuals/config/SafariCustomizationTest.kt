package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.Category
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.structures.WallState
import net.johnceo.sparklingmutuals.safari.structures.WallSummary
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariCustomizationTest {
    @TempDir lateinit var dir: Path
    @Test fun `dark theme upgrades old panel defaults while preserving custom colors and opacity`() {
        val saved = Properties().apply {
            setProperty("appearance.progress.backgroundColor", AppearanceConfig.color(0x241F18))
            setProperty("appearance.progress.borderColor", AppearanceConfig.color(0xE3C77E))
            setProperty("appearance.run_complete.backgroundColor", AppearanceConfig.color(0xAAFFAA))
            setProperty("appearance.run_complete.transparency", "80")
            setProperty("appearance.missing.backgroundColor", "3:70:18:52:86")
            setProperty("appearance.missing.borderColor", AppearanceConfig.color(0x123456))
            setProperty("appearance.missing.transparency", "45")
        }
        AppearanceConfig.load(saved)
        assertEquals(AppearanceConfig.color(SafariTheme.BACKGROUND), AppearanceConfig.panels.getValue("progress").backgroundColor)
        assertEquals(AppearanceConfig.color(SafariTheme.BORDER), AppearanceConfig.panels.getValue("progress").borderColor)
        assertEquals(0xCC0D0D10.toInt(), AppearanceConfig.panels.getValue("run_complete").backgroundArgb)
        val custom = AppearanceConfig.panels.getValue("missing")
        assertEquals("3:70:18:52:86", custom.backgroundColor)
        assertEquals(AppearanceConfig.color(0x123456), custom.borderColor)
        assertEquals(45, custom.transparency)
        AppearanceConfig.save(saved)
        AppearanceConfig.load(saved)
        assertEquals("3:70:18:52:86", custom.backgroundColor)
        assertEquals(0xCC0D0D10.toInt(), AppearanceConfig.panels.getValue("run_complete").backgroundArgb)
    }
    @Test fun `settings categories omit descriptors and general owns shared actions`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val categories = SafariSettings::class.java.fields.mapNotNull { it.getAnnotation(Category::class.java) }
        assertTrue(categories.all { it.desc.isEmpty() })
        assertTrue(categories.any { it.name == "General" })
        assertTrue(categories.any { it.name == "Customization" })
    }
    @Test fun `defaults separate missing panel counts from highlights and disable mound results`() {
        ConfigManager.init(dir)
        assertTrue(ConfigManager.highlightSnooperWalls)
        assertTrue(ConfigManager.showSnooperWalls)
        assertTrue(ConfigManager.showMoundCount)
        assertTrue(ConfigManager.showBeeNests)
        assertFalse(ConfigManager.showMoundStats)
        AppearanceConfig.panels.values.forEach { assertFalse(it.borderEnabled) }
        for (id in listOf("miria", "progress", "missing", "captures", "sparklings")) {
            val style = AppearanceConfig.panels.getValue(id)
            assertEquals(20, style.transparency)
            assertEquals(0xCC000000.toInt(), style.backgroundArgb)
        }
        assertEquals(67, AppearanceConfig.panels.getValue("incense").transparency)
    }
    @Test fun `one selected hex color applies to multiple targets without enabling borders or ESP`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        AppearanceConfig.panels.getValue("miria").borderEnabled = false
        SafariEspConfig.groups.getValue("forest").enabled = false
        SafariEspConfig.mobs.getValue("Macaw").enabled = false
        val settings = SafariSettings()
        settings.customization.selectedHex = "#126aFE"
        settings.customization.miria.border.setColor.run()
        settings.customization.missing.background.setColor.run()
        settings.customization.macaw.setColor.run()
        settings.customization.beeNests.setColor.run()
        settings.customization.captures.transparency = 65
        ConfigManager.showBeeNests = false
        ConfigManager.showMoundCount = false
        ConfigManager.showSnooperWalls = false
        ConfigManager.highlightSnooperWalls = false
        settings.apply()
        assertFalse(SafariEspConfig.groups.getValue("forest").enabled)
        assertFalse(SafariEspConfig.mobs.getValue("Macaw").enabled)
        ConfigManager.init(dir)
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(AppearanceConfig.panels.getValue("miria").borderColor))
        assertEquals(0xCC126AFE.toInt(), AppearanceConfig.panels.getValue("missing").backgroundArgb)
        assertEquals(65, AppearanceConfig.panels.getValue("captures").transparency)
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(SafariEspConfig.mobs.getValue("Macaw").color))
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(AppearanceConfig.nestColor))
        assertFalse(AppearanceConfig.panels.getValue("miria").borderEnabled)
        assertTrue(SafariEspConfig.groups.getValue("forest").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Macaw").enabled)
        assertFalse(ConfigManager.showBeeNests)
        assertFalse(ConfigManager.showMoundCount)
        assertFalse(ConfigManager.showSnooperWalls)
        assertFalse(ConfigManager.highlightSnooperWalls)
        assertFalse(ConfigManager.showMoundStats)
    }
    @Test fun `invalid hex colors do not change targets and valid hex preserves alpha and chroma`() {
        assertEquals("3:70:18:52:86", AppearanceConfig.withHex("3:70:0:0:0", " #123456 "))
        for (invalid in listOf("red", "#123", "#12345678", "00000g", "#", "")) {
            assertNull(AppearanceConfig.withHex("0:255:0:0:0", invalid))
        }
        ConfigManager.init(dir)
        val choice = CustomizationSettings()
        val original = AppearanceConfig.nestColor
        choice.selectedHex = "wrong"
        assertFalse(choice.beeNests.setSelected())
        assertEquals(original, AppearanceConfig.nestColor)
        AppearanceConfig.load(Properties().apply {
            setProperty("appearance.miria.transparency", "999")
            setProperty("appearance.missing.transparency", "-1")
            setProperty("appearance.captures.backgroundColor", "invalid")
        })
        assertEquals(100, AppearanceConfig.panels.getValue("miria").transparency)
        assertEquals(0, AppearanceConfig.panels.getValue("missing").transparency)
        assertEquals(0xCC0D0D10.toInt(), AppearanceConfig.panels.getValue("captures").backgroundArgb)
    }
    @Test fun `customization offers every existing critter color exactly once`() {
        ConfigManager.init(dir)
        val choices = CustomizationSettings()
        val names = listOf(choices.cavern, choices.forest, choices.icy, choices.haunted).flatMap { group -> group.javaClass.fields.toList() }.filter { it.type == ColorChoice::class.java || it.type == ScrappyAppearanceSettings::class.java }.map {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java).name
        }
        SafariRoster.all.filter { it.name != "Rockmite" }.forEach { assertEquals(1, names.count { name -> name == "${it.name} ESP" }) }
        assertTrue(names.containsAll(listOf("Rockmite silverfish ESP", "Rockmite mound ESP")))
    }
    @Test fun `warnings live in their accordion and mode-owned structure controls are absent`() {
        fun names(type: Class<*>) = type.fields.mapNotNull { it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.name }
        assertEquals(setOf("Ticket mode", "Contest HUD and tracking", "Auto !dt contest", "Honeyhive beacons", "Warning"), names(SafariSettings.Miria::class.java).toSet())
        assertTrue(names(SafariSettings.Unique::class.java).contains("Profitable critter ESP"))
        assertEquals(setOf("5 minute warning", "3 minute warning", "1 minute warning", "No contest warnings", "Warning titles", "Sound volume", "Warning sound", "Save sound"), names(SafariSettings.Warning::class.java).toSet())
        assertEquals(setOf("Command help", "Move and resize HUDs"), names(SafariSettings.General::class.java).toSet())
        assertFalse(names(SafariSettings.Party::class.java).contains("Command help"))
        assertFalse(names(SafariSettings.Tracking::class.java).contains("Move and resize HUDs"))
        assertFalse(names(SafariSettings.Safari::class.java).any { it in setOf("Remaining", "Highlight Snooper Walls", "Highlight bee nests") })
        assertFalse(names(SafariSettings.Tracking::class.java).any { it in setOf("Bee Nests", "Rockmite Mounds", "Snooper Walls", "Mound results", "Count Unique Only", "Biome capture counts") })
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val settings = SafariSettings()
        ConfigManager.showMoundStats = true
        ConfigManager.countUniqueOnly = true
        ConfigManager.catchCountPanel = true
        settings.apply()
        assertTrue(ConfigManager.showMoundStats)
        assertTrue(ConfigManager.countUniqueOnly)
        assertTrue(ConfigManager.catchCountPanel)
    }
    @Test fun `biome colors and rockmite form colors remain independent`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val settings = SafariSettings()
        val groups = listOf(settings.customization.huds, settings.customization.alertHuds, settings.customization.critters)
        val labels = groups.flatMap { group -> group.javaClass.fields.mapNotNull {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.name
        } }
        assertTrue(labels.containsAll(listOf("Missing panel HUD", "Sparkling alert HUD", "Cavern critter colors", "Forest critter colors", "Icy critter colors", "Haunted critter colors")))
        assertFalse(labels.any { it.endsWith(" ESP") && it != "Floor drop ESP" })
        settings.customization.selectedHex = "#123456"
        settings.customization.cavern.rockmiteMound.setColor.run()
        settings.customization.cavern.rockmite.color = "0:255:101:102:103"
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(0xFF123456.toInt(), SafariEspConfig.rgb(SafariEspConfig.rockmiteMoundColor))
        assertEquals(0xFF656667.toInt(), SafariEspConfig.rgb(SafariEspConfig.mobs.getValue("Rockmite").color))
        assertTrue(SafariEspConfig.rockmiteMoundEnabled)
        assertTrue(SafariEspConfig.mobs.getValue("Rockmite").enabled)
    }
    @Test fun `missing panel stays species only despite legacy toggles and unknown walls`() {
        ConfigManager.init(dir)
        val partial = WallSummary(listOf(WallState.INTACT, WallState.BROKEN, WallState.UNKNOWN))
        assertFalse(partial.allBroken)
        assertEquals("1 (+1?)", partial.remaining)
        assertFalse(WallSummary(emptyList()).allBroken)
        assertTrue(WallSummary(List(5) { WallState.BROKEN }).allBroken)
        val forest = SafariPanels.missing(null, SafariBiome.FOREST, true, 3)
        assertTrue(forest.rows.any { it.label == "Honeybug" })
        ConfigManager.showBeeNests = false
        assertTrue(ConfigManager.highlightBeeNests)
        assertTrue(SafariPanels.missing(null, SafariBiome.FOREST, true, 3).rows.any { it.label == "Honeybug" })
        val cavern = SafariPanels.missing(null, SafariBiome.CAVERN, true, 0, 7, partial)
        assertTrue(cavern.rows.any { it.label == "Rockmite" })
        assertTrue(cavern.rows.any { it.label == "Snoozle" })
        assertEquals("7", cavern.rows.single { it.label == "Mounds to break" }.value)
        assertEquals("1 (+1?)", cavern.rows.single { it.label == "Snooper walls to break" }.value)
        ConfigManager.showMoundCount = false
        ConfigManager.showSnooperWalls = false
        val hidden = SafariPanels.missing(null, SafariBiome.CAVERN, true, 0, 7, partial)
        assertEquals(cavern.rows, hidden.rows)
        assertFalse(SafariPanels.missing(null, SafariBiome.CAVERN, true, 0).rows.any { it.label == "Mounds to break" })
    }
}
