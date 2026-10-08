package net.johnceo.sparklingmutuals

import io.github.notenoughupdates.moulconfig.annotations.Category
import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.*

class SafariCustomizationTest {
    @TempDir lateinit var dir: Path
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
        AppearanceConfig.panels.values.forEach {
            assertFalse(it.borderEnabled)
            assertEquals(20, it.transparency)
            assertEquals(0xCC000000.toInt(), it.backgroundArgb)
        }
    }
    @Test fun `one selected hex color applies to multiple targets without enabling borders or ESP`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        SafariEspConfig.groups.getValue("forest").enabled = false
        SafariEspConfig.mobs.getValue("Macaw").enabled = false
        val settings = SafariSettings()
        settings.customization.selectedHex = "#126aFE"
        settings.customization.miria.border.setColor.run()
        settings.customization.missing.background.setColor.run()
        settings.customization.macaw.setColor.run()
        settings.customization.beeNests.setColor.run()
        settings.customization.captures.transparency = 65
        settings.safari.remaining.nests = false
        settings.safari.remaining.mounds = false
        settings.safari.remaining.walls = false
        settings.safari.snooperHighlight = false
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(AppearanceConfig.panels.getValue("miria").borderColor))
        assertEquals(0xCC126AFE.toInt(), AppearanceConfig.panels.getValue("missing").backgroundArgb)
        assertEquals(65, AppearanceConfig.panels.getValue("captures").transparency)
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(SafariEspConfig.mobs.getValue("Macaw").color))
        assertEquals(0xFF126AFE.toInt(), SafariEspConfig.rgb(AppearanceConfig.nestColor))
        assertFalse(AppearanceConfig.panels.getValue("miria").borderEnabled)
        assertFalse(SafariEspConfig.groups.getValue("forest").enabled)
        assertFalse(SafariEspConfig.mobs.getValue("Macaw").enabled)
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
        assertEquals(0xCC000000.toInt(), AppearanceConfig.panels.getValue("captures").backgroundArgb)
    }
    @Test fun `customization offers every existing critter color exactly once`() {
        ConfigManager.init(dir)
        val choices = CustomizationSettings()
        val names = listOf(choices.cavern, choices.forest, choices.icy, choices.haunted).flatMap { group -> group.javaClass.fields.toList() }.filter { it.type == ColorChoice::class.java }.map {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java).name
        }
        SafariRoster.all.filter { it.name != "Rockmite" }.forEach { assertEquals(1, names.count { name -> name == "${it.name} ESP" }) }
        assertTrue(names.containsAll(listOf("Rockmite silverfish ESP", "Rockmite mound ESP")))
    }
    @Test fun `warning and remaining controls live in their accordions and full clear controls are absent`() {
        fun names(type: Class<*>) = type.fields.mapNotNull { it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.name }
        assertEquals(setOf("Contest HUD and tracking", "Warning"), names(SafariSettings.Miria::class.java).toSet())
        assertEquals(setOf("5 minute warning", "3 minute warning", "1 minute warning", "No contest warnings", "Warning titles", "Sound volume", "Warning sound", "Save sound"), names(SafariSettings.Warning::class.java).toSet())
        assertEquals(setOf("Run mode", "Command help", "Move and resize HUDs"), names(SafariSettings.General::class.java).toSet())
        assertFalse(names(SafariSettings.Party::class.java).contains("Command help"))
        assertFalse(names(SafariSettings.Tracking::class.java).contains("Move and resize HUDs"))
        assertTrue(names(SafariSettings.Safari::class.java).contains("Remaining"))
        assertEquals(setOf("Bee Nests", "Rockmite Mounds", "Snooper Walls"), names(SafariSettings.Remaining::class.java).toSet())
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
    @Test fun `biome colors and rockmite toggles remain independent`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val settings = SafariSettings()
        val labels = CustomizationSettings::class.java.fields.mapNotNull {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.name
        }
        assertTrue(labels.containsAll(listOf("Missing panel HUD", "Sparkling alert HUD", "Cavern critter colors", "Forest critter colors", "Icy critter colors", "Haunted critter colors")))
        assertFalse(labels.any { it.endsWith(" ESP") && it != "Floor drop ESP" })
        settings.customization.selectedHex = "#123456"
        settings.customization.cavern.rockmiteMound.setColor.run()
        settings.customization.cavern.rockmite.color = "0:255:101:102:103"
        settings.cavernEsp.rockmiteMound = false
        settings.cavernEsp.rockmite.enabled = true
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(0xFF123456.toInt(), SafariEspConfig.rgb(SafariEspConfig.rockmiteMoundColor))
        assertEquals(0xFF656667.toInt(), SafariEspConfig.rgb(SafariEspConfig.mobs.getValue("Rockmite").color))
        assertFalse(SafariEspConfig.rockmiteMoundEnabled)
        assertTrue(SafariEspConfig.mobs.getValue("Rockmite").enabled)
    }
    @Test fun `panel footer toggles are independent and unknown walls are never claimed broken`() {
        ConfigManager.init(dir)
        val partial = WallSummary(listOf(WallState.INTACT, WallState.BROKEN, WallState.UNKNOWN))
        assertFalse(partial.allBroken)
        assertEquals("1 (+1?)", partial.remaining)
        assertFalse(WallSummary(emptyList()).allBroken)
        assertTrue(WallSummary(List(5) { WallState.BROKEN }).allBroken)
        val forest = SafariPanels.missing(null, SafariBiome.FOREST, true, 3)
        assertTrue(forest.rows.any { it.label == "Bee nests to punch" })
        ConfigManager.showBeeNests = false
        assertTrue(ConfigManager.highlightBeeNests)
        assertFalse(SafariPanels.missing(null, SafariBiome.FOREST, true, 3).rows.any { it.label == "Bee nests to punch" })
        val cavern = SafariPanels.missing(null, SafariBiome.CAVERN, true, 0, 7, partial)
        assertEquals("7", cavern.rows.first { it.label == "Mounds to break" }.value)
        assertEquals("1 (+1?)", cavern.rows.first { it.label == "Snooper walls to break" }.value)
        ConfigManager.showMoundCount = false
        ConfigManager.showSnooperWalls = false
        val hidden = SafariPanels.missing(null, SafariBiome.CAVERN, true, 0, 7, partial)
        assertFalse(hidden.rows.any { it.label.contains("Mounds") || it.label.contains("Snooper walls") })
        assertFalse(SafariPanels.missing(null, SafariBiome.CAVERN, true, 0).rows.any { it.label == "Mounds to break" })
    }
}
