package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.safari.completion.BiomePbType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariModeConfigTest {
    @TempDir lateinit var dir: Path
    @Test fun `Modes setting selects the same presets as the command and survives repeated GUI ticks`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.select(net.johnceo.sparklingmutuals.safari.SafariMode.FULL_CLEAR)
        settings.apply()
        assertTrue(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.FULL_CLEAR, ConfigManager.personalBests.biomeType)
        assertFalse(ConfigManager.timesaveOnly)
        assertTrue(ConfigManager.catchCountPanel)
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        repeat(3) { settings.apply() }
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.select(net.johnceo.sparklingmutuals.safari.SafariMode.UNIQUE)
        settings.apply()
        repeat(3) { settings.apply() }
        assertFalse(ConfigManager.fullClearMode)
        assertEquals(BiomePbType.UNIQUE, ConfigManager.personalBests.biomeType)
        assertTrue(ConfigManager.timesaveOnly)
        assertFalse(ConfigManager.catchCountPanel)
        assertTrue(SafariEspConfig.mobs.getValue("Macaw").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Driftling").enabled)
        ConfigManager.init(dir)
        assertEquals(net.johnceo.sparklingmutuals.safari.SafariMode.UNIQUE, net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode)
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
