package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.compat.redCandle
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.automation.SafariCandleRules
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CandleBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Change only client selection/raycast shape, preserving collision and server block state. */
@Mixin(BlockBehaviour.BlockStateBase::class)
abstract class SafariCandleMixin {
    @Shadow abstract fun getBlock(): Block
    @Inject(method = ["getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"], at = [At("HEAD")], cancellable = true)
    private fun candle(world: BlockGetter, pos: BlockPos, context: CollisionContext, result: CallbackInfoReturnable<VoxelShape>) {
        if (world !is ClientLevel || getBlock() !is CandleBlock) return
        if (!SafariCandleRules.target(getBlock() === redCandle)) return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (listOf(player.mainHandItem, player.offhandItem).any { SafariCandleRules.enabled(ConfigManager.candleHitbox,
            SafariAssist.inSafari && SafariAssist.biome == SafariBiome.HAUNTED, world === client.level, it.hoverName.string) })
            result.returnValue = Shapes.block()
    }
}
