package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class SafariExpansionTest {
    @TempDir lateinit var directory: Path
    @Test fun `requested Safari features have the specified saved defaults`() {
        ConfigManager.init(directory)
        ConfigManager.save()
        val properties = Properties().apply {
            Files.newInputStream(directory.resolve("sparkling-mutuals.properties")).use(::load)
        }
        for (key in listOf("progressHud", "missingPanel", "highlightBeeNests", "removeDarkness", "sparklingAlert", "sparklingPartyAnnouncer"))
            assertEquals("true", properties.getProperty(key), key)
        for (key in listOf("countUniqueOnly", "catchCountPanel")) assertEquals("false", properties.getProperty(key), key)
        assertEquals("1", properties.getProperty("showWhere"))
    }
}
