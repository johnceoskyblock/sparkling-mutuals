package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariInventoryAlertsTest {
    @TempDir lateinit var dir: Path
    private val enabled = InventoryAlert.entries.toSet()
    @Test fun `gems require one of each exact type in Cavern and only alert once while held`() {
        val state = InventoryAlertState()
        val gems = listOf("§5Purple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 1)
        assertTrue(state.poll(SafariBiome.CAVERN, gems.dropLast(1), enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.FOREST, gems, enabled).isEmpty())
        assertEquals(listOf(InventoryAlert.GEMS), state.poll(SafariBiome.CAVERN, gems, enabled))
        assertTrue(state.poll(SafariBiome.CAVERN, gems, enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.CAVERN, gems.dropLast(1), enabled).isEmpty())
        assertEquals(listOf(InventoryAlert.GEMS), state.poll(SafariBiome.CAVERN, gems, enabled))
    }
    @Test fun `bird food inventory alone never qualifies without Forest pickup evidence`() {
        val state = InventoryAlertState()
        assertTrue(state.poll(SafariBiome.FOREST, listOf("Yogi Berry" to 9), enabled).isEmpty())
        val food = listOf("Yogi Berry" to 1, "Yogi Berry" to 2, "Wriggleworm" to 3, "Bag of Seeds" to 2)
        assertTrue(state.poll(SafariBiome.FOREST, food, enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.FOREST, food + ("Bag of Seeds" to 1), enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.FOREST, food + ("Bag of Seeds" to 1), enabled).isEmpty())
    }
    @Test fun `incense needs four in Haunted and toggles biome exit and new runs reset readiness`() {
        val state = InventoryAlertState()
        val incense = listOf("Soothing Incense" to 4)
        assertTrue(state.poll(SafariBiome.HAUNTED, listOf("Soothing Incense" to 3), enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.ICY, incense, enabled).isEmpty())
        assertTrue(state.poll(SafariBiome.HAUNTED, incense, emptySet()).isEmpty())
        assertEquals(listOf(InventoryAlert.INCENSE), state.poll(SafariBiome.HAUNTED, incense, enabled))
        state.poll(null, incense, enabled)
        assertEquals(listOf(InventoryAlert.INCENSE), state.poll(SafariBiome.HAUNTED, incense, enabled))
        state.reset()
        assertEquals(listOf(InventoryAlert.INCENSE), state.poll(SafariBiome.HAUNTED, incense, enabled))
        assertTrue(state.poll(SafariBiome.HAUNTED, listOf("Soothing Incense Shard" to 4), enabled).isEmpty())
    }
    @Test fun `alert dropdown toggles save independently and survive restart`() {
        ConfigManager.init(dir)
        val settings = SafariSettings()
        settings.safari.alerts.gems = false
        settings.safari.alerts.birdFood = true
        settings.safari.alerts.incense = false
        settings.apply()
        ConfigManager.init(dir)
        assertFalse(ConfigManager.allGemsAlert)
        assertTrue(ConfigManager.allBirdFoodAlert)
        assertFalse(ConfigManager.allIncenseAlert)
        val options = SafariSettings.Alerts::class.java.fields.mapNotNull { it.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.name }
        assertEquals(setOf("All Gems Alert", "All Bird Food Alert", "All Incense Alert"), options.toSet())
    }
}
