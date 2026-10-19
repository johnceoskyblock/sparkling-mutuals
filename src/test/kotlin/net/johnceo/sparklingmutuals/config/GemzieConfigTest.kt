package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GemzieConfigTest {
    @TempDir lateinit var dir:Path
    @Test fun `fresh missing and malformed choices default on in Safari helpers`() {
        for(contents in listOf("","gemzieGemAutoSelect=invalid\n")) {
            Files.writeString(dir.resolve("sparkling-mutuals.properties"),contents)
            ConfigManager.init(dir)
            assertTrue(ConfigManager.gemzieGemAutoSelect)
            assertTrue(SafariSettings().safari.gemzieGemAutoSelect)
        }
    }
    @Test fun `an explicit saved off choice survives real settings save and reload`() {
        ConfigManager.init(dir)
        val settings=SafariSettings()
        settings.safari.gemzieGemAutoSelect=false
        settings.apply()
        ConfigManager.gemzieGemAutoSelect=true
        ConfigManager.init(dir)
        assertFalse(ConfigManager.gemzieGemAutoSelect)
        assertFalse(SafariSettings().safari.gemzieGemAutoSelect)
        val saved=java.util.Properties().also { Files.newInputStream(dir.resolve("sparkling-mutuals.properties")).use(it::load) }
        assertEquals("false",saved.getProperty("gemzieGemAutoSelect"))
    }
    @Test fun `mode changes do not turn a deliberately disabled helper back on`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"),"gemzieGemAutoSelect=false\n")
        ConfigManager.init(dir)
        val settings=SafariSettings()
        settings.modes.fullClear.enabled=true
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(ConfigManager.gemzieGemAutoSelect)
    }
}
