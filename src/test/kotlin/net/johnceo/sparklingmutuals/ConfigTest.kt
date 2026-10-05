package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.hud.SafariHud

class ConfigTest {
    @TempDir lateinit var dir: Path
    @Test fun `HUD layouts and personal bests survive actual configuration saving`() {
        ConfigManager.init(dir)
        ConfigManager.personalBests.record("CAPTURE! You caught a Doomspiral!", SafariRun(1000), 62000)
        SafariHud.CAPTURES.layout.x = .4f
        SafariHud.CAPTURES.layout.scale = 1.3f
        ConfigManager.catchCountPanel = true
        ConfigManager.save()
        ConfigManager.init(dir)
        assertEquals(61000L, ConfigManager.personalBests.time("Doomspiral"))
        assertNull(ConfigManager.personalBests.time("Wumpa"))
        assertEquals(.4f, SafariHud.CAPTURES.layout.x)
        assertEquals(1.3f, SafariHud.CAPTURES.layout.scale)
        assertTrue(ConfigManager.catchCountPanel)
        val saved = Files.readString(dir.resolve("sparkling-mutuals.properties"))
        assertTrue(saved.contains("pb.doomspiralMillis=61000"))
        assertTrue(saved.contains("hud.captures.scale=1.3"))
    }

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
