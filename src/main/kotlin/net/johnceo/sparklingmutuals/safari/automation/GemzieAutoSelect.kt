package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariIsland
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/** Handles one genuine use press before Minecraft snapshots the old main-hand stack. */
object GemzieAutoSelect {
    private val action=GemzieUseAction()
    private var lastNotice:Long?=null

    fun handle(client:Minecraft,fresh:Boolean):Boolean {
        val player=client.player ?: return false
        val level=client.level ?: return false
        val gameMode=client.gameMode ?: return false
        if(client.smScreen!=null || gameMode.isDestroying || player.isHandsBusy) return false
        val hit=client.hitResult as? BlockHitResult ?: return false
        if(hit.type!=HitResult.Type.BLOCK) return false
        val pos=hit.blockPos
        val click=GemzieClick(pos.x,pos.y,pos.z,ConfigManager.gemzieGemAutoSelect,
            SafariIsland.onIsland, SafariAssist.biome==SafariBiome.CAVERN,hit.direction==Direction.UP)
        val gem=GemzieSelection.target(click) ?: return false
        if(!level.hasChunkAt(pos) || !player.isWithinBlockInteractionRange(pos,0.0) || !level.worldBorder.isWithinBounds(pos)) return false
        val inventory=player.inventory
        val stacks=if(fresh) (0..8).map { snapshot(inventory.getItem(it)) } else emptyList()
        return action.handle(click,inventory.selectedSlot,stacks,fresh,
            select={ inventory.selectedSlot=it },
            use=dispatch@ {
                val stack=player.mainHandItem
                if(!GemzieSelection.matches(snapshot(stack),gem) || !stack.isItemEnabled(level.enabledFeatures())) return@dispatch
                val count=stack.count
                //? if >=26.3 {
                /*val animation=stack.interactAnimation
                *///? }
                // The normal method synchronizes the selected slot before its block-use packet.
                val result=gameMode.useItemOn(player,InteractionHand.MAIN_HAND,hit)
                val swingSource=
                    //? if >=26.3 {
                    /*InteractionResult.SwingSource.PREDICTED
                    *///? } else {
                    InteractionResult.SwingSource.CLIENT
                    //? }
                if(result is InteractionResult.Success && result.swingSource()==swingSource) {
                    //? if >=26.3 {
                    /*player.swing(InteractionHand.MAIN_HAND,animation,false)
                    *///? } else {
                    player.swing(InteractionHand.MAIN_HAND)
                    //? }
                    if(!stack.isEmpty && (stack.count!=count || player.hasInfiniteMaterials())) {
                        //? if >=26.3 {
                        /*player.itemUsed(InteractionHand.MAIN_HAND)
                        *///? } else {
                        client.gameRenderer.itemInHandRenderer.itemUsed(InteractionHand.MAIN_HAND)
                        //? }
                    }
                }
                // Consume PASS/FAIL too: no air-use, off-hand fallback, or delayed replay.
            },
            missing={ missing ->
                val now=System.nanoTime()/1_000_000
                if(lastNotice?.let { now-it>=3000 }!=false) {
                    lastNotice=now
                    player.sendSystemMessage(LocalChat.component("Put a whole ${missing.label} in your hotbar to use this podium."))
                }
            })
    }

    private fun snapshot(stack:ItemStack):GemzieStack {
        val tag=stack.get(DataComponents.CUSTOM_DATA)?.copyTag()
        val id=if(tag?.contains("id")==true) tag.getStringOr("id","") else null
        return GemzieStack(id,stack.hoverName.string,stack.count)
    }
}
