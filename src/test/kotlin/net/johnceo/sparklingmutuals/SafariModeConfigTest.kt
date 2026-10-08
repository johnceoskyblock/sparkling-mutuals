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
    @Test fun `Modes setting selects the same presets as the command and survives repeated GUI ticks`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.modes.fullClear.enabled = true
        settings.apply()
        assertTrue(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.FULL_CLEAR, ConfigManager.personalBests.biomeType)
        assertFalse(ConfigManager.timesaveOnly)
        assertTrue(ConfigManager.catchCountPanel)
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        repeat(3) { settings.apply() }
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        settings.modes.unique.enabled = true
        settings.apply()
        repeat(3) { settings.apply() }
        assertFalse(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.UNIQUE, ConfigManager.personalBests.biomeType)
        assertTrue(ConfigManager.timesaveOnly)
        assertFalse(ConfigManager.catchCountPanel)
        assertFalse(SafariEspConfig.mobs.getValue("Macaw").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Driftling").enabled)
        ConfigManager.init(dir)
        assertTrue(SafariSettings().modes.unique.enabled)
    }
    @Test fun `setting the existing mode preserves biome visibility choices`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.esp.forest = false
        settings.apply(); settings.apply()
        assertFalse(SafariEspConfig.groups.getValue("forest").onlyInBiome)
        assertTrue(CommandHelp.localLines().any { it.contains("/fc") })
    }
}
