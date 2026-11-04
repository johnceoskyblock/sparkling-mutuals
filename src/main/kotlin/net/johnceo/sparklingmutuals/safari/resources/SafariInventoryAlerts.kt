package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft

enum class InventoryAlert(val biome: SafariBiome, val title: String, val detail: String, val items: Map<String, Int>) {
    GEMS(SafariBiome.CAVERN, "ALL GEMS READY!", "Purple, Lime and Orange Gems for Gemzie",
        mapOf("Purple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 1)),
    BIRD_FOOD(SafariBiome.FOREST, "ALL BIRD FOOD COLLECTED!", "All nine Forest bird food pickups collected",
        mapOf("Yogi Berry" to 3, "Wriggleworm" to 3, "Bag of Seeds" to 3)),
    COINS(SafariBiome.HAUNTED, "ALL COINS COLLECTED!", "All Shining Coin floor drops collected", emptyMap()),
    INCENSE(SafariBiome.HAUNTED, "ALL INCENSE READY!", "4 Soothing Incense for Doomspiral", mapOf("Soothing Incense" to 4))
}

/** Notify on a readiness transition, rather than every inventory scan. */
class InventoryAlertState {
    private val ready = mutableSetOf<InventoryAlert>()
    fun poll(biome: SafariBiome?, stacks: List<Pair<String, Int>>, enabled: Set<InventoryAlert>): List<InventoryAlert> {
        val counts = mutableMapOf<String, Int>()
        stacks.forEach { (name, count) -> if (count > 0) counts.merge(SafariRules.strip(name).trim(), count, Int::plus) }
        val current = InventoryAlert.entries.filter { alert -> alert !in setOf(InventoryAlert.BIRD_FOOD, InventoryAlert.COINS, InventoryAlert.GEMS) && alert in enabled && alert.biome == biome &&
            alert.items.all { (name, amount) -> (counts[name] ?: 0) >= amount } }.toSet()
        val notices = current.filter { it !in ready }
        ready.clear(); ready.addAll(current)
        return notices
    }
    fun reset() { ready.clear() }
}

/** Suppress readiness inherited on entry; only edges witnessed in the same biome alert. */
class EncounterReadiness {
    private var previousBiome: SafariBiome?=null
    private val previous=mutableMapOf<InventoryAlert,Boolean>()
    private val announced=mutableSetOf<InventoryAlert>()
    fun poll(biome: SafariBiome?,values: Map<InventoryAlert,Boolean>): List<InventoryAlert> {
        val result=values.filter { (alert,ready) -> ready && previous[alert]==false &&
            previousBiome==biome && biome==alert.biome && alert !in announced }.keys.toList()
        announced.addAll(result)
        // A ready state first observed elsewhere must not replay upon entry later.
        values.filterValues { it }.keys.forEach(announced::add)
        previous.clear();previous.putAll(values);previousBiome=biome
        return result
    }
    fun reset() { previous.clear();announced.clear();previousBiome=null }
}

object SafariInventoryAlerts {
    private val readiness = EncounterReadiness()
    private var ticks = 0
    private var level: Any? = null
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        val player = client.player
        if (!SafariAssist.inSafari || player == null) { readiness.reset(); ticks = 0; return }
        val run=SafariTracking.ledger.current
        run?.gems?.tick(client)
        if (run?.gems?.opened == true) run.sparklingChecks.confirmGemzieChamber()
        val stacks = BirdfeederInventory.snapshot(client)
        run?.gems?.inventory(stacks.map { SafariRules.strip(it.first).trim() to it.second })
        SafariTracking.ledger.current?.inventoryEvidence(stacks)
        if (++ticks < 10) return
        ticks = 0
        SafariFloorDrops.state.inventory(stacks, SafariTracking.ledger.current?.inStartingInventoryWindow(System.currentTimeMillis()) == true)
        val birds = SafariTracking.ledger.current?.birds
        birds?.inventory(stacks)
        SafariTracking.ledger.current?.sparklingChecks?.inventory(stacks)
        val notices=readiness.poll(SafariAssist.biome,mapOf(
            InventoryAlert.GEMS to (run?.gems?.ready==true && run.gems.opened.not()),
            InventoryAlert.BIRD_FOOD to (birds?.foodsComplete==true),
            InventoryAlert.COINS to (run!=null && run.gimmiegoldCheck.floorMinimum>0 && run.gimmiegoldCheck.remaining==0),
            InventoryAlert.INCENSE to (stacks.filter { SafariRules.strip(it.first).trim()=="Soothing Incense" }.sumOf { it.second }>=4)
        ))
        notices.forEach {
            SmallAlerts.show(when (it) {
                InventoryAlert.COINS -> SafariHud.COINS
                InventoryAlert.GEMS -> SafariHud.GEMS
                InventoryAlert.BIRD_FOOD -> SafariHud.BIRD_FOOD
                InventoryAlert.INCENSE -> SafariHud.INCENSE
            })

        }
    }
    fun reset() { readiness.reset(); ticks = 0; level = null; SmallAlerts.resetInventory() }
}
