package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

/** SafariUtils' feeder contents slot (22); player slots and cursor are never feeder contents. */
object BirdfeederInventory {
    fun tick(client: Minecraft) {
        val screen=client.smScreen as? AbstractContainerScreen<*> ?: return
        if (screen.title.string != "Birdfeeder") return
        val menu=screen.menu
        if (menu.slots.size <= 22) return
        val stack=menu.getSlot(22).item
        val food=SafariRules.strip(stack.hoverName.string).trim()
        if (food in setOf("Bag of Seeds","Wriggleworm","Yogi Berry"))
            SafariTracking.ledger.current?.birds?.feeder(food,stack.count)
        else if (stack.isEmpty) SafariTracking.ledger.current?.birds?.feeder(null,0)
    }
    fun carried(client: Minecraft): List<Pair<String,Int>> {
        val screen=client.smScreen as? AbstractContainerScreen<*> ?: return emptyList()
        val stack=screen.menu.carried
        return if (stack.isEmpty) emptyList() else listOf(stack.hoverName.string to stack.count)
    }
}
