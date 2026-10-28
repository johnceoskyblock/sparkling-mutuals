package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import com.llamalad7.mixinextras.sugar.Local
import net.johnceo.sparklingmutuals.safari.ClearWater
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.material.FluidState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At

/** Sodium's own fluid mesher does not call the vanilla surface renderer. */
@Pseudo
@Mixin(targets=["net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"],remap=false)
class SafariSodiumWaterMixin {
    @ModifyExpressionValue(method=["updateQuad"],at=[At(value="INVOKE",target="Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I")],require=0)
    private fun transparentWater(color: Int, @Local(argsOnly=true) fluid: FluidState): Int =
        ClearWater.surfaceColor(color,fluid.`is`(FluidTags.WATER))
}
