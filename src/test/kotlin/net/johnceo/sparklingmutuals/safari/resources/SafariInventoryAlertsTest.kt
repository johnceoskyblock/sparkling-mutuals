package net.johnceo.sparklingmutuals.safari.resources

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.SafariBiome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SafariInventoryAlertsTest {
    @TempDir lateinit var dir: Path
    private val enabled = InventoryAlert.entries.toSet()
    @Test fun `encounter readiness only alerts for an edge witnessed in its biome`() {
        val state=EncounterReadiness()
        fun poll(biome: SafariBiome,ready: Boolean)=state.poll(biome,mapOf(InventoryAlert.COINS to ready))
        assertTrue(poll(SafariBiome.CAVERN,false).isEmpty())
        assertTrue(poll(SafariBiome.CAVERN,true).isEmpty())
        assertTrue(poll(SafariBiome.HAUNTED,true).isEmpty(),"late entry must not replay")
        state.reset()
        assertTrue(poll(SafariBiome.HAUNTED,false).isEmpty())
        assertEquals(listOf(InventoryAlert.COINS),poll(SafariBiome.HAUNTED,true))
        assertTrue(poll(SafariBiome.HAUNTED,true).isEmpty())
    }
    @Test fun `gem readiness is owned by persistent run objective instead of simultaneous inventory`() {
        val state=InventoryAlertState()
        assertTrue(state.poll(SafariBiome.CAVERN,listOf("Purple Gem" to 1,"Lime Gem" to 1,"Orange Gem" to 1),enabled).isEmpty())
        val objective=GemzieObjective()
        for(name in listOf("Purple Gem","Lime Gem","Orange Gem")) objective.inventory(listOf(name to 1))
        assertTrue(objective.ready)
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
        val options = SafariSettings.Alerts::class.java.fields.mapNotNull { it.getAnnotation(net.johnceo.sparklingmutuals.config.ConfigOption::class.java)?.name }
        assertTrue(options.none { it in setOf("All Gems Alert", "All Bird Food Alert", "All Incense Alert") })
    }
}
