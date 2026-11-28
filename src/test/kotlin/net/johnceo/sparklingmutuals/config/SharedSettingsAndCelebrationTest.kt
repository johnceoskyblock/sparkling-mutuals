package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.ui.SparklingAlertStyle
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class SharedSettingsAndCelebrationTest {
    @TempDir lateinit var dir: Path

    @Test fun `share restores settings and layouts while preserving linked account and records`() {
        ConfigManager.init(dir)
        ContestConfig.init(dir)
        ConfigManager.apiKey = "original key"
        val historyOwner = "12345678123456781234567812345678"
        ConfigManager.historyOwnerUuid = historyOwner
        ConfigManager.removeIcyFrost = false
        ContestConfig.showContestTimer = false
        val original = SettingsShare.exportCode()
        ConfigManager.removeIcyFrost = true
        ContestConfig.showContestTimer = true
        ContestConfig.save()
        val history = dir.resolve("sparkling-mutuals-runs.json")
        Files.writeString(history, "untouched records")

        val decoded = SettingsShare.preview(original)
        assertFalse(decoded.main.containsKey("apiKey"))
        assertFalse(decoded.main.containsKey("history.ownerUuid"))
        assertEquals("false", decoded.main.getProperty("removeIcyFrost"))
        assertEquals("false", decoded.contest.getProperty("showContestTimer"))
        SettingsShare.apply(decoded)
        assertFalse(ConfigManager.removeIcyFrost)
        assertFalse(ContestConfig.showContestTimer)
        assertEquals("original key", ConfigManager.apiKey)
        assertEquals(historyOwner, ConfigManager.historyOwnerUuid)
        assertEquals("untouched records", Files.readString(history))
        assertTrue(Files.exists(dir.resolve("sparkling-mutuals.before-share.properties")))
        assertThrows(Exception::class.java) { SettingsShare.preview("SMC1:not-a-settings-code") }
    }

    @Test fun `decode discards inactive edition and private settings`() {
        val upstream = Properties().apply {
            setProperty("recatchWaypoint", "true")
            setProperty("wildExtra", "true")
            setProperty("warpList.players", "APlayer")
        }
        val receiver = Properties().apply { setProperty("recatchWaypoint", "false") }
        val data = SettingsShare.encode(SettingsShare.Snapshot(upstream, Properties()))
        val result = SettingsShare.decode(data, SettingsShare.Snapshot(receiver, Properties()))
        assertEquals(setOf("recatchWaypoint"), result.main.stringPropertyNames())
    }

    @Test fun `custom celebration survives share and reload without changing catch triggers`() {
        ConfigManager.init(dir)
        ConfigManager.specialCatchAlert = false
        ConfigManager.alwaysCelebrateSparkling = true
        val builtIn = CelebrationRecipe.fromPreset(32)
        assertArrayEquals(SparklingAlertStyle.preset(32).effectLevels(), builtIn.style().effectLevels())
        val custom = builtIn.copy(seconds = 60, volume = 0, caption = "We got one!")
        val roundTrip = CustomCatchCelebrations.decode(CustomCatchCelebrations.encode(custom))
        assertEquals(30, roundTrip.seconds)
        assertEquals(custom.caption, roundTrip.caption)
        val preset = NamedCelebration("Quiet", roundTrip, favorite = true)
        CustomCatchCelebrations.apply(roundTrip, listOf(preset))
        ConfigManager.init(dir)
        assertEquals(SparklingAlertStyle.CUSTOM_INDEX, ConfigManager.specialCatchStyle)
        assertEquals(listOf(preset), CustomCatchCelebrations.presets)
        assertEquals(roundTrip, CustomCatchCelebrations.active)
        assertFalse(ConfigManager.specialCatchAlert)
        assertTrue(ConfigManager.alwaysCelebrateSparkling)
    }
}
