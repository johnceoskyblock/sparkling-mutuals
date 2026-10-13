package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class SparklingSoundTest {
    @TempDir lateinit var dir: Path
    private fun roundTrip(sound: String, volume: String): Properties {
        val path = dir.resolve("sparkling-mutuals.properties")
        Properties().apply { setProperty("sparklingSound", sound); setProperty("sparklingSoundVolume", volume) }
            .also { p -> Files.newOutputStream(path).use { p.store(it, "test") } }
        ConfigManager.init(dir); ConfigManager.save(); ConfigManager.init(dir)
        return Properties().also { p -> Files.newInputStream(path).use(p::load) }
    }
    @Test fun `custom sparkling sound and muted volume survive restart`() {
        val saved = roundTrip("minecraft:block.note_block.chime", "0")
        assertEquals("minecraft:block.note_block.chime", saved.getProperty("sparklingSound"))
        assertEquals("0", saved.getProperty("sparklingSoundVolume"))
        assertEquals("minecraft:block.note_block.chime", SafariSettings().safari.alerts.sound)
        assertEquals(0, SafariSettings().safari.alerts.volume)
    }
    @Test fun `invalid sound falls back to chime and volume is clamped`() {
        val saved = roundTrip("Bad sound!", "200")
        assertEquals("minecraft:block.amethyst_block.chime", saved.getProperty("sparklingSound"))
        assertEquals("100", saved.getProperty("sparklingSoundVolume"))
    }
}
