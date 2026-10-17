package net.johnceo.sparklingmutuals.safari.integration

import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.config.ContestConfig
import net.johnceo.sparklingmutuals.config.CustomizationSettings
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariEspSettings
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.contest.ContestDowntimeState
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FloorColorsAndContestDowntimeTest {
    @TempDir lateinit var dir: Path
    @Test fun `individual floor colors migrate shared color persist and reject invalid values independently`() {
        val legacy = "0:255:1:2:3"
        SafariEspConfig.load(Properties().apply { setProperty("esp.floor.color", legacy) })
        assertTrue(SafariEspConfig.floorColors.values.all { it == legacy })
        val settings = SafariEspSettings()
        val customization = CustomizationSettings(settings.floorColors)
        settings.floorColors.coin = "0:255:255:215:0"
        settings.floorColors.incense = "0:255:100:0:255"
        settings.floorColors.lime = "invalid"
        assertTrue(settings.apply())
        customization.apply()
        assertEquals(legacy, SafariEspConfig.dropColor(FloorDropType.LIME))
        assertEquals(legacy, SafariEspConfig.dropColor(FloorDropType.SEEDS))
        val saved = Properties().also(SafariEspConfig::save)
        SafariEspConfig.load(saved)
        assertEquals("0:255:255:215:0", SafariEspConfig.dropColor(FloorDropType.COIN))
        assertEquals("0:255:100:0:255", SafariEspConfig.dropColor(FloorDropType.INCENSE))
        assertEquals(9, SafariEspConfig.floorColors.size)
        customization.selectedHex = "#112233"
        assertTrue(customization.floor.setSelected())
        settings.apply(); customization.apply()
        assertEquals(SafariEspConfig.floorColor, SafariEspConfig.dropColor(FloorDropType.COIN))
        assertEquals(SafariEspConfig.floorColor, SafariEspConfig.dropColor(FloorDropType.INCENSE))
        SafariEspConfig.floorColor = "0:255:9:8:7"
        assertTrue(SafariEspConfig.floorColors.values.all { it == "0:255:9:8:7" })
    }
    @Test fun `contest downtime sends once on a fresh boundary with party and never on initial load`() {
        val state = ContestDowntimeState()
        assertFalse(state.poll(1, 0, true, true, true, true))
        assertTrue(state.poll(2, 0, true, true, true, true))
        repeat(3) { assertFalse(state.poll(2, 1, true, true, true, true)) }
        assertFalse(state.poll(3, 0, false, true, true, true))
        assertFalse(state.poll(3, 1, true, true, true, true))
        assertFalse(state.poll(4, 0, true, true, false, true))
        assertFalse(state.poll(4, 1, true, true, true, true))
        assertFalse(state.poll(5, 0, true, false, true, true))
        assertFalse(state.poll(6, 100, true, true, true, true))
        assertTrue(state.poll(7, 1, true, true, true, true))
    }
    @Test fun `nonleaders cannot auto request and becoming leader midcontest does not catch up`() {
        val state = ContestDowntimeState()
        assertFalse(state.poll(1, 0, true, true, true, false))
        assertFalse(state.poll(2, 0, true, true, true, false))
        assertFalse(state.poll(2, 1, true, true, true, true))
        assertTrue(state.poll(3, 0, true, true, true, true))
    }
    @Test fun `contest downtime switch defaults on and saved off remains off`() {
        ContestConfig.init(dir)
        assertTrue(ContestConfig.autoContestDowntime)
        ContestConfig.autoContestDowntime = false
        ContestConfig.save()
        ContestConfig.autoContestDowntime = true
        ContestConfig.init(dir)
        assertFalse(ContestConfig.autoContestDowntime)
        assertFalse(SafariSettings.Miria().autoContestDowntime)
    }
}
