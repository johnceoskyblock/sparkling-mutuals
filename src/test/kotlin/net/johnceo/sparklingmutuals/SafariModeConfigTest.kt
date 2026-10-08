package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariModeConfigTest {
    @TempDir lateinit var dir: Path
    @Test fun `General mode selects the same presets as the command and survives repeated GUI ticks`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.general.mode = 1
        settings.apply()
        assertTrue(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.FULL_CLEAR, ConfigManager.personalBests.biomeType)
        assertFalse(ConfigManager.timesaveOnly)
        assertTrue(ConfigManager.catchCountPanel)
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        repeat(3) { settings.apply() }
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        settings.forestEsp.macaw.enabled = false
        settings.apply()
        assertFalse(SafariEspConfig.mobs.getValue("Macaw").enabled)
        settings.general.mode = 0
        settings.apply()
        repeat(3) { settings.apply() }
        assertFalse(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.UNIQUE, ConfigManager.personalBests.biomeType)
        assertTrue(ConfigManager.timesaveOnly)
        assertFalse(ConfigManager.catchCountPanel)
        assertFalse(SafariEspConfig.mobs.getValue("Macaw").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Driftling").enabled)
        ConfigManager.init(dir)
        assertEquals(0, SafariSettings().general.mode)
    }
    @Test fun `setting the existing mode preserves individual manual ESP choices`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.forestEsp.foxtrot.enabled = false
        settings.apply(); settings.apply()
        assertFalse(SafariEspConfig.mobs.getValue("Foxtrot").enabled)
        assertTrue(CommandHelp.localLines().any { it.contains("/fc") })
    }
}
