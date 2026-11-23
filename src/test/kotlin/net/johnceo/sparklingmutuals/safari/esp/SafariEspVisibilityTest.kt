package net.johnceo.sparklingmutuals.safari.esp

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.config.SettingsCatalog
import net.johnceo.sparklingmutuals.config.SettingsTarget
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariEspVisibilityTest {
    @TempDir lateinit var dir: Path
    @Test fun `visibility choices survive mode switches and reloads without changing colors`() {
        java.nio.file.Files.writeString(dir.resolve("sparkling-mutuals.properties"), "esp.floor.onlyInBiome=true\n")
        ConfigManager.init(dir)
        if (!SafariSettings.standardEdition) for (type in FloorDropType.entries) {
            assertEquals(type == FloorDropType.GENERIC, SafariEspConfig.dropOnlyInBiome(type), type.name)
            assertEquals(type != FloorDropType.GENERIC, SafariEspRules.visible(true, true,
                SafariEspConfig.dropOnlyInBiome(type), SafariBiome.CAVERN, SafariBiome.HAUNTED), type.name)
        }
        val settings = SafariSettings()
        if (!SafariSettings.standardEdition) {
            val catalog = SettingsCatalog(settings)
            for (biome in listOf("forest", "cavern")) {
                catalog.locate(SettingsTarget("ESP General", listOf("biomes", biome)))!!.second.last().write(false)
            }
            for ((type, outside) in mapOf("generic" to true, "incense" to false, "lime" to false)) {
                catalog.locate(SettingsTarget("ESP General", listOf("floorVisibility", type)))!!.second.last().write(outside)
            }
        }
        settings.customization.forest.foxtrot.color = "0:255:18:52:86"
        settings.apply()
        net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.select(net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR)
        settings.apply(); settings.apply()
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        ConfigManager.init(dir)
        assertEquals(SafariSettings.standardEdition, SafariEspConfig.groups.getValue("forest").onlyInBiome)
        assertEquals(SafariSettings.standardEdition, SafariEspConfig.groups.getValue("cavern").onlyInBiome)
        if (!SafariSettings.standardEdition) {
            assertFalse(SafariEspConfig.dropOnlyInBiome(FloorDropType.GENERIC))
            assertTrue(SafariEspConfig.dropOnlyInBiome(FloorDropType.INCENSE))
            assertTrue(SafariEspConfig.dropOnlyInBiome(FloorDropType.LIME))
            assertFalse(SafariEspConfig.dropOnlyInBiome(FloorDropType.PURPLE))
            assertFalse(SafariEspConfig.dropOnlyInBiome(FloorDropType.SEEDS))
            val saved = java.util.Properties().apply {
                java.nio.file.Files.newInputStream(dir.resolve("sparkling-mutuals.properties")).use(::load)
            }
            assertNull(saved.getProperty("esp.floor.onlyInBiome"))
            assertTrue(SafariEspRules.visible(true, true, SafariEspConfig.dropOnlyInBiome(FloorDropType.GENERIC), SafariBiome.CAVERN, SafariBiome.HAUNTED))
            assertFalse(SafariEspRules.visible(true, true, SafariEspConfig.dropOnlyInBiome(FloorDropType.INCENSE), SafariBiome.CAVERN, SafariBiome.HAUNTED))
        }
        assertTrue(SafariEspConfig.groups.getValue("icy").onlyInBiome)
        assertEquals("0:255:18:52:86", SafariEspConfig.mobs.getValue("Foxtrot").color)
        assertEquals(!SafariSettings.standardEdition, SafariEspRules.visible(true, true,
            SafariEspConfig.groups.getValue("forest").onlyInBiome, SafariBiome.CAVERN, SafariBiome.FOREST))
        assertFalse(SafariEspRules.visible(true, true, SafariEspConfig.groups.getValue("icy").onlyInBiome, SafariBiome.CAVERN, SafariBiome.ICY))
    }
    @Test fun `legacy individual switches cannot block mode owned ESP after reopening the game`() {
        ConfigManager.init(dir)
        SafariFullClear.select(SafariMode.FULL_CLEAR)
        SafariEspConfig.groups.getValue("forest").enabled = false
        SafariEspConfig.mobs.getValue("Foxtrot").enabled = false
        ConfigManager.save(); ConfigManager.init(dir)
        assertTrue(SafariEspConfig.groups.getValue("forest").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Foxtrot").enabled)
    }
}
