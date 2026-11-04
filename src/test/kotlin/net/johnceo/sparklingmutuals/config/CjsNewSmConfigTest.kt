package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CjsNewSmConfigTest {
    @TempDir lateinit var root: Path

    @Test fun `migrated CJ choices survive settings edits and are not replaced by fresh defaults`() {
        Files.writeString(root.resolve("sparkling-mutuals.properties"),
            "safariEntryDryRun=false\nsafariEntryApproach=true\nautoSelectSafariTicket=true\nwarpDelaySeconds=26\n")
        val data = "{\"retained\":\"profile-specific shard counts\"}"
        Files.writeString(root.resolve("sparkling-mutuals-box-guidance.json"), data)
        val migrated = ConfigStorage.migrate(root)
        assertEquals(root.resolve("sparkling-mutuals"), migrated)
        assertFalse(Files.exists(root.resolve("sparkling-mutuals.properties")))
        ConfigManager.init(migrated); ContestConfig.init(migrated)
        assertFalse(ConfigManager.safariEntryDryRun)
        assertTrue(ConfigManager.safariEntryApproach)
        assertTrue(ConfigManager.autoSelectSafariTicket)
        assertEquals(26, ConfigManager.warpDelaySeconds)
        SafariSettings().apply { safari.entry.approach = false }.apply()
        ConfigManager.init(ConfigStorage.migrate(root))
        assertFalse(ConfigManager.safariEntryApproach)
        assertFalse(ConfigManager.safariEntryDryRun)
        assertTrue(ConfigManager.autoSelectSafariTicket)
        assertEquals(26, ConfigManager.warpDelaySeconds)
        assertEquals(data, Files.readString(migrated.resolve("sparkling-mutuals-box-guidance.json")))
    }

    @Test fun `fresh migrated CJ profile exchanges status without moving or selecting tickets`() {
        val migrated = ConfigStorage.migrate(root)
        ConfigManager.init(migrated); ContestConfig.init(migrated)
        assertTrue(ConfigManager.safariEntryDryRun)
        assertFalse(ConfigManager.safariEntryApproach)
        assertFalse(ConfigManager.autoSelectSafariTicket)
        assertEquals(28, ConfigManager.warpDelaySeconds)
        SafariSettings().apply { safari.entry.approach = true }.apply()
        ConfigManager.init(migrated)
        assertTrue(ConfigManager.safariEntryApproach)
        assertFalse(ConfigManager.autoSelectSafariTicket)
        assertEquals(28, ConfigManager.warpDelaySeconds)
    }

    @Test fun `older installed CJ settings without new keys preserve explicit timing`() {
        Files.writeString(root.resolve("sparkling-mutuals.properties"), "warpDelaySeconds=27\nshinyDetection=false\n")
        ConfigManager.init(ConfigStorage.migrate(root))
        assertEquals(27, ConfigManager.warpDelaySeconds)
        assertFalse(ConfigManager.shinyDetection)
        assertTrue(ConfigManager.safariEntryDryRun)
        assertFalse(ConfigManager.safariEntryApproach)
        assertFalse(ConfigManager.autoSelectSafariTicket)
    }
}
