package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.resources.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FloorDropAlwaysOnTest {
    @TempDir lateinit var dir: Path

    @Test fun `gems default on and every floor toggle persists through settings`() {
        ConfigManager.init(dir)
        val gems = setOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE)
        FloorDropType.entries.forEach { assertEquals(it in gems, SafariEspConfig.floorAlwaysOn(it)) }
        val settings = FloorDropToggleSettings()
        settings.lime = false; settings.coin = true
        assertTrue(settings.apply())
        val saved = Properties().also(SafariEspConfig::save)
        SafariEspConfig.load(saved)
        assertFalse(SafariEspConfig.floorAlwaysOn(FloorDropType.LIME))
        assertTrue(SafariEspConfig.floorAlwaysOn(FloorDropType.COIN))
        assertTrue(SafariEspConfig.floorAlwaysOn(FloorDropType.PURPLE))
    }

    @Test fun `always on keeps remaining gem markers after collection and respects floor master`() {
        ConfigManager.init(dir)
        SafariFloorDrops.state.reset()
        try {
            SafariFloorDrops.state.inventory(listOf("Lime Gem" to 1, "Purple Gem" to 1, "Orange Gem" to 1))
            val drop = PredictedFloorDrop(1, -100.0, 60.0, 40.0, SafariBiome.CAVERN, FloorDropType.LIME)
            assertTrue(SafariFloorDrops.selected(drop, emptySet()))
            SafariEspConfig.floorAlwaysOnToggles[FloorDropType.LIME] = false
            assertFalse(SafariFloorDrops.selected(drop, emptySet()))
            SafariEspConfig.floorAlwaysOnToggles[FloorDropType.LIME] = true
            SafariEspConfig.groups.getValue("floor").enabled = false
            assertFalse(SafariFloorDrops.selected(drop, emptySet()))
        } finally {
            SafariFloorDrops.state.reset()
            SafariEspConfig.groups.getValue("floor").enabled = true
        }
    }
}
