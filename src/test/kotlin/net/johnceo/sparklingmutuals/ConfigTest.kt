package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ConfigTest {
    @TempDir lateinit var dir: Path

    @Test
    fun `legacy API configuration gets the new defaults and preserves its key`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "apiKey=example-key\n")
        ConfigManager.init(dir)
        assertEquals("example-key", ConfigManager.apiKey)
        assertEquals(25, ConfigManager.warpDelaySeconds)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertTrue(ConfigManager.partyCommandsEnabled)
    }

    @Test
    fun `warp settings survive a restart and invalid values fall back safely`() {
        ConfigManager.init(dir)
        ConfigManager.warpAlertsEnabled = true
        ConfigManager.warpDelaySeconds = 28
        ConfigManager.partyCommandsEnabled = false
        ConfigManager.save()
        ConfigManager.init(dir)
        assertTrue(ConfigManager.warpAlertsEnabled)
        assertEquals(28, ConfigManager.warpDelaySeconds)
        assertFalse(ConfigManager.partyCommandsEnabled)
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "warpDelaySeconds=invalid\n")
        ConfigManager.init(dir)
        assertEquals(25, ConfigManager.warpDelaySeconds)
    }

    @Test
    fun `warning presets parse spaces and none survives saving`() {
        ContestConfig.init(dir)
        assertEquals(listOf(5, 3, 1), ContestConfig.warningMinutes())
        ContestConfig.contestWarnMinutes = "3, 1"
        ContestConfig.save()
        ContestConfig.init(dir)
        assertEquals(listOf(3, 1), ContestConfig.warningMinutes())
        ContestConfig.contestWarnEnabled = false
        ContestConfig.contestWarnMinutes = ""
        ContestConfig.save()
        ContestConfig.init(dir)
        assertTrue(ContestConfig.warningMinutes().isEmpty())
    }
}
