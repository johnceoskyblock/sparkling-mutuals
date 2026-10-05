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
    @Test fun `settings expose a customization tab and the requested warp description`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        val categories = SafariSettings::class.java.fields.mapNotNull { it.getAnnotation(Category::class.java) }
        assertEquals("Warp reminders", categories.first { it.name == "Warp reminders" }.desc)
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
        settings.tracking.nests = false
        settings.tracking.mounds = false
        settings.tracking.walls = false
        settings.tracking.moundStats = true
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
        assertTrue(ConfigManager.showMoundStats)
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
        val names = choices.javaClass.fields.filter { it.type == ColorChoice::class.java }.map {
            it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java).name
        }
        SafariRoster.all.forEach { assertEquals(1, names.count { name -> name == "${it.name} ESP" }) }
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
