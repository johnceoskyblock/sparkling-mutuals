package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariEspVisibilityTest {
    @TempDir lateinit var dir: Path
    @Test fun `visibility choices survive mode switches and reloads without changing colors`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.esp.forest = false
        settings.esp.cavern = false
        settings.esp.floor = false
        settings.customization.forest.foxtrot.color = "0:255:18:52:86"
        settings.apply()
        settings.modes.fullClear.enabled = true
        settings.apply(); settings.apply()
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        ConfigManager.init(dir)
        assertFalse(SafariEspConfig.groups.getValue("forest").onlyInBiome)
        assertFalse(SafariEspConfig.groups.getValue("cavern").onlyInBiome)
        assertFalse(SafariEspConfig.groups.getValue("floor").onlyInBiome)
        assertTrue(SafariEspConfig.groups.getValue("icy").onlyInBiome)
        assertEquals("0:255:18:52:86", SafariEspConfig.mobs.getValue("Foxtrot").color)
        assertTrue(SafariEspRules.visible(true, true, SafariEspConfig.groups.getValue("forest").onlyInBiome, SafariBiome.CAVERN, SafariBiome.FOREST))
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
