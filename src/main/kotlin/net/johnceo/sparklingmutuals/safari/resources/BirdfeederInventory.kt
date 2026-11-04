package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

/** SafariUtils' feeder contents slot (22); player slots and cursor are never feeder contents. */
object BirdfeederInventory {
    private val foods=setOf("Bag of Seeds","Wriggleworm","Yogi Berry")
    private var owner: Any?=null
    private var cursor=BirdCursorGuard()
    private var sampledTick: Int?=null
    private var observedMenu: Any?=null
    private var emptyTicks=0
    private fun sync() {
        val current=SafariTracking.ledger.current
        if(owner!==current) { owner=current;cursor=BirdCursorGuard();sampledTick=null;observedMenu=null;emptyTicks=0 }
    }
    fun tick(client: Minecraft) {
        sync()
        val player=client.player ?: return
        if(sampledTick==player.tickCount) return
        sampledTick=player.tickCount
        val screen=client.smScreen as? AbstractContainerScreen<*>
        if(screen==null || screen.title.string!="Birdfeeder") { observedMenu=null;emptyTicks=0;return }
        val menu=screen.menu
        if(observedMenu!==menu) { observedMenu=menu;emptyTicks=0;SafariTracking.ledger.current?.birds?.feederOpened() }
        if (menu.slots.size <= 22) return
        val stack=menu.getSlot(22).item
        val food=SafariRules.strip(stack.hoverName.string).trim()
        if (food in foods && !stack.isEmpty) {
            emptyTicks=0
            SafariTracking.ledger.current?.birds?.feeder(food,stack.count)
        } else if(++emptyTicks>=2) SafariTracking.ledger.current?.birds?.feeder(null,0)
    }
    fun snapshot(client: Minecraft): List<Pair<String,Int>> {
        tick(client)
        val player=client.player ?: return emptyList()
        val screen=client.smScreen as? AbstractContainerScreen<*>
        val stack=player.containerMenu.carried
        val real=if(stack.isEmpty) emptyList() else listOf(SafariRules.strip(stack.hoverName.string).trim() to stack.count)
        val held = (0 until player.inventory.containerSize).map { player.inventory.getItem(it) }.filterNot { it.isEmpty }
            .groupingBy { SafariRules.strip(it.hoverName.string).trim() }.fold(0) { count, item -> count + item.count }
        val extra=cursor.extra(held.filterKeys { it in foods },real.filter { it.first in foods }.toMap(),screen?.title?.string=="Birdfeeder",
            SafariTracking.ledger.current?.birds?.localFeederFood,System.currentTimeMillis())
        return held.toList() + real.filter { it.first !in foods } + extra.toList()
    }
}
