package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.blaze3d.vertex.PoseStack
import net.johnceo.sparklingmutuals.safari.ClearWater
import net.minecraft.client.Minecraft
//? if <26.2 {
import net.minecraft.client.renderer.MultiBufferSource
//? } else {
/*import net.minecraft.client.renderer.SubmitNodeCollector
*///? }
//? if >=26.3 {
/*import net.minecraft.client.renderer.state.level.PlayerRenderState
*///? }
import net.minecraft.client.renderer.ScreenEffectRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(ScreenEffectRenderer::class)
class SafariWaterOverlayMixin {
    //? if >=26.3 {
    /*@WrapOperation(method=["submit"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/renderer/state/level/PlayerRenderState\$WaterOverlay;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V")])
    private fun noWaterOverlay(overlay: PlayerRenderState.WaterOverlay, pose: PoseStack, buffers: SubmitNodeCollector, original: Operation<Void>) {
        if (!ClearWater.active) original.call(overlay,pose,buffers)
    }
    *///? } else if >=26.2 {
    /*@WrapOperation(method=["submit"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V")])
    private fun noWaterOverlay(client: Minecraft, pose: PoseStack, buffers: SubmitNodeCollector, original: Operation<Void>) {
        if (!ClearWater.active) original.call(client,pose,buffers)
    }
    *///? } else {
    @WrapOperation(method=["renderScreenEffect"],at=[At(value="INVOKE",target="Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")])
    private fun noWaterOverlay(client: Minecraft, pose: PoseStack, buffers: MultiBufferSource, original: Operation<Void>) {
        if (!ClearWater.active) original.call(client,pose,buffers)
    }
    //? }
}
