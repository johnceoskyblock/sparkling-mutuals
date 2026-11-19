package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import java.util.Properties
import net.minecraft.client.gui.Font
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HudFontAndTransparencyTest {
    @TempDir lateinit var dir: Path

    @Test fun `HUD shadow and pixel alignment preferences autosave without changing font choice`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        assertTrue(settings.general.font.hudTextShadow)
        assertTrue(settings.general.font.pixelAlignedHuds)
        settings.general.font.hudTextShadow = false
        settings.general.font.pixelAlignedHuds = false
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(AppearanceConfig.hudTextShadow)
        assertFalse(AppearanceConfig.pixelAlignedHuds)
        assertTrue(AppearanceConfig.resourcePackFont)
    }

    @Test fun `HUD font preference is in Overview and survives autosave`() {
        ConfigManager.init(dir)
        assertTrue(AppearanceConfig.resourcePackFont)
        val settings = SafariSettings()
        val overview = SettingsCatalog(settings).pages.single { it.name == "General" }
        assertTrue(overview.rows.single { it.name == "HUD font customization" }.children.any { it.field.name == "resourcePackFont" })
        settings.general.font.resourcePackFont = false
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(AppearanceConfig.resourcePackFont)
        val restored = SafariSettings()
        restored.general.font.resourcePackFont = true
        restored.apply()
        ConfigManager.init(dir)
        assertTrue(AppearanceConfig.resourcePackFont)
    }

    @Test fun `backgrounds use zero solid and one hundred invisible and honor picker alpha`() {
        AppearanceConfig.load(Properties())
        for (style in AppearanceConfig.panels.values) {
            style.backgroundColor = "0:255:18:52:86"
            style.transparency = 0
            assertEquals(0xFF123456.toInt(), style.backgroundArgb)
            style.transparency = 100
            assertEquals(0, style.backgroundArgb ushr 24)
            style.transparency = 50
            assertEquals(128, style.backgroundArgb ushr 24)
            style.backgroundColor = "0:128:18:52:86"
            assertEquals(64, style.backgroundArgb ushr 24)
            style.backgroundColor = "0:0:18:52:86"
            style.transparency = 0
            assertEquals(0, style.backgroundArgb ushr 24)
        }
    }

    @Test fun `font provider accessor target exists on this Minecraft version`() {
        assertEquals(Font.Provider::class.java, Font::class.java.getDeclaredField("provider").type)
        val resource = javaClass.getResourceAsStream("/assets/sparkling-mutuals/font/hud.json")!!
        val json = resource.bufferedReader().use { it.readText() }
        assertTrue(json.contains("minecraft:include/default"))
        assertTrue(json.contains("minecraft:include/unifont"))
    }
}
