package net.johnceo.sparklingmutuals.safari.integration

import java.nio.file.Path
import java.util.Properties
import java.util.UUID
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ForestMidRunAndEspScaleTest {
    @TempDir lateinit var dir: Path
    @Test fun `ESP scale defaults larger applies through GUI and persists across restart`() {
        ConfigManager.init(dir); ContestConfig.init(dir)
        assertEquals(2f, SafariEspConfig.textScale)
        assertEquals(.05f, SafariEspConfig.labelWorldScale)
        val settings = SafariSettings()
        settings.esp.textScale = 3.25f
        settings.apply()
        ConfigManager.init(dir)
        assertEquals(3.25f, SafariEspConfig.textScale)
        assertEquals(.025f * 3.25f, SafariEspConfig.labelWorldScale)
    }
    @Test fun `ESP scale rejects non finite input and bounds invalid saved values`() {
        for ((raw, expected) in mapOf("NaN" to 2f, "Infinity" to 2f, "wrong" to 2f, "99" to 4f, "-1" to .5f)) {
            SafariEspConfig.load(Properties().apply { setProperty("esp.textScale", raw) })
            assertEquals(expected, SafariEspConfig.textScale, raw)
        }
    }
}
