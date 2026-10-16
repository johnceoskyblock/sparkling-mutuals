package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.automation.SafariManagerGuard
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.phys.EntityHitResult
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Right-clicking an entity has no client-side Fabric event; returning FAIL here means no interact packet is sent. */
@Mixin(MultiPlayerGameMode::class)
class SafariManagerMixin {
    @Inject(method = ["handleContainerInput"], at = [At("HEAD")], cancellable = true)
    private fun guardEntry(containerId: Int, slotId: Int, button: Int, input: ContainerInput, player: Player, info: CallbackInfo) {
        if (SafariManagerGuard.shouldBlockEntry(containerId, slotId)) info.cancel()
    }
    @Inject(method = ["interact"], at = [At("HEAD")], cancellable = true)
    private fun guard(player: Player, entity: Entity, hit: EntityHitResult, hand: InteractionHand, info: CallbackInfoReturnable<InteractionResult>) {
        if (SafariManagerGuard.shouldBlock(entity)) info.returnValue = InteractionResult.FAIL
    }
    @Inject(method = ["attack"], at = [At("HEAD")], cancellable = true)
    private fun guardAttack(player: Player, entity: Entity, info: CallbackInfo) {
        if (SafariManagerGuard.shouldBlock(entity)) info.cancel()
    }
    @Inject(method = ["useItem"], at = [At("HEAD")], cancellable = true)
    private fun guardTicket(player: Player, hand: InteractionHand, info: CallbackInfoReturnable<InteractionResult>) {
        if (SafariManagerGuard.shouldBlockTicket(player, hand)) info.returnValue = InteractionResult.FAIL
    }
    @Inject(method = ["useItemOn"], at = [At("HEAD")], cancellable = true)
    private fun guardTicketOn(player: LocalPlayer, hand: InteractionHand, hit: BlockHitResult, info: CallbackInfoReturnable<InteractionResult>) {
        if (SafariManagerGuard.shouldBlockTicket(player, hand)) info.returnValue = InteractionResult.FAIL
    }
}
