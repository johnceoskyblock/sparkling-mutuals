package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.*
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(MultiPlayerGameMode::class)
class ShotUseMixin {
    @Unique private var shotAir:ShotInputToken?=null
    @Unique private var shotBlock:ShotInputToken?=null
    @Inject(method=["useItem"],at=[At("HEAD")])
    private fun air(player:Player,hand:InteractionHand,info:CallbackInfoReturnable<InteractionResult>) {
        shotAir=ShotObservationHooks.use(player,hand,ShotUseRoute.AIR)
    }
    @Inject(method=["useItem"],at=[At("RETURN")])
    private fun airReturn(player:Player,hand:InteractionHand,info:CallbackInfoReturnable<InteractionResult>) {
        ShotObservationHooks.returned(shotAir,info.returnValue);shotAir=null
    }
    @Inject(method=["useItemOn"],at=[At("HEAD")])
    private fun block(player:LocalPlayer,hand:InteractionHand,hit:BlockHitResult,info:CallbackInfoReturnable<InteractionResult>) {
        shotBlock=ShotObservationHooks.use(player,hand,ShotUseRoute.BLOCK)
    }
    @Inject(method=["useItemOn"],at=[At("RETURN")])
    private fun blockReturn(player:LocalPlayer,hand:InteractionHand,hit:BlockHitResult,info:CallbackInfoReturnable<InteractionResult>) {
        ShotObservationHooks.returned(shotBlock,info.returnValue);shotBlock=null
    }
}
