package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.LocalChat

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SmallAlerts
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

enum class InventoryAlert(val biome: SafariBiome, val title: String, val detail: String, val items: Map<String, Int>) {
    GEMS(SafariBiome.CAVERN, "ALL GEMS READY!", "Purple, Lime and Orange Gems for Gemzie",
        mapOf("Purple Gem" to 1, "Lime Gem" to 1, "Orange Gem" to 1)),
    BIRD_FOOD(SafariBiome.FOREST, "ALL BIRD FOOD COLLECTED!", "All nine Forest bird food pickups collected",
        mapOf("Yogi Berry" to 3, "Wriggleworm" to 3, "Bag of Seeds" to 3)),
    INCENSE(SafariBiome.HAUNTED, "ALL INCENSE READY!", "4 Soothing Incense for Doomspiral", mapOf("Soothing Incense" to 4))
}

/** Notify on a readiness transition, rather than every inventory scan. */
class InventoryAlertState {
    private val ready = mutableSetOf<InventoryAlert>()
    fun poll(biome: SafariBiome?, stacks: List<Pair<String, Int>>, enabled: Set<InventoryAlert>): List<InventoryAlert> {
        val counts = mutableMapOf<String, Int>()
        stacks.forEach { (name, count) -> if (count > 0) counts.merge(SafariRules.strip(name).trim(), count, Int::plus) }
        val current = InventoryAlert.entries.filter { alert -> alert != InventoryAlert.BIRD_FOOD && alert in enabled && alert.biome == biome &&
            alert.items.all { (name, amount) -> (counts[name] ?: 0) >= amount } }.toSet()
        val notices = current.filter { it !in ready }
        ready.clear(); ready.addAll(current)
        return notices
    }
    fun reset() { ready.clear() }
}

object SafariInventoryAlerts {
    private val state = InventoryAlertState()
    private var ticks = 0
    private var level: Any? = null
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        val player = client.player
        if (!SafariAssist.inSafari || player == null) { state.reset(); ticks = 0; return }
        if (++ticks < 10) return
        ticks = 0
        val enabled = buildSet {
            if (ConfigManager.allGemsAlert) add(InventoryAlert.GEMS)
            if (ConfigManager.allIncenseAlert) add(InventoryAlert.INCENSE)
        }
        val inventory = player.inventory
        val stacks = (0 until inventory.containerSize).map { inventory.getItem(it) }.filterNot { it.isEmpty }
            .map { it.hoverName.string to it.count }
        SafariFloorDrops.state.inventory(stacks)
        val birds = SafariTracking.ledger.current?.birds
        birds?.inventory(stacks)
        SafariTracking.ledger.current?.sparklingChecks?.inventory(stacks)
        val notices = state.poll(SafariAssist.biome, stacks, enabled).toMutableList()
        if (birds?.alert(SafariAssist.biome, ConfigManager.allBirdFoodAlert) == true) notices.add(InventoryAlert.BIRD_FOOD)
        notices.forEach {
            SmallAlerts.show(when (it) {
                InventoryAlert.GEMS -> SafariHud.GEMS
                InventoryAlert.BIRD_FOOD -> SafariHud.BIRD_FOOD
                InventoryAlert.INCENSE -> SafariHud.INCENSE
            })
            player.sendSystemMessage(LocalChat.component("[SM] ${it.detail}${if (it == InventoryAlert.BIRD_FOOD) "." else " ready."}"))
        }
    }
    fun reset() { state.reset(); ticks = 0; level = null; SmallAlerts.resetInventory() }
}
