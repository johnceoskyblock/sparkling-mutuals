package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.esp.TeammateEsp
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(EntityRenderer::class)
class SafariShinyMixin {
    @Inject(method = ["extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"], at = [At("TAIL")])
    private fun highlight(entity: Entity, state: EntityRenderState, delta: Float, info: CallbackInfo) {
        val critterState = state as net.johnceo.sparklingmutuals.safari.esp.CritterGlowState
        critterState.`sm$setCritterGlow`(false)
        if ((ConfigManager.highlightTeammates || ConfigManager.showTeammateNames) && TeammateEsp.isTeammate(entity)) {
            if (ConfigManager.highlightTeammates)
                state.outlineColor = TeammateEsp.COLOR
            if (ConfigManager.showTeammateNames) state.nameTag = null
        }
        val color = if (SafariAssist.isGlowHighlighted(entity.id)) SafariEspConfig.rgb(AppearanceConfig.sparklingColor)
            else net.johnceo.sparklingmutuals.safari.esp.SafariEsp.sparklingDuplicoColor(entity)
                ?: net.johnceo.sparklingmutuals.safari.esp.SafariEsp.outlineColor(entity)
        if (color != null) {
            state.outlineColor = color
            critterState.`sm$setCritterGlow`(true)
        }
        if (SafariAssist.visualsActive(net.minecraft.client.Minecraft.getInstance())) state.nameTag = state.nameTag?.let {
            net.johnceo.sparklingmutuals.safari.ui.CritterNames.component(it)
        }
        if (state.nameTag != null && net.johnceo.sparklingmutuals.safari.esp.SafariEsp.hideVanillaNameTag(entity)) state.nameTag = null
    }

    @Inject(method=["submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"],at=[At("TAIL")])
    private fun blockGlow(state: EntityRenderState, poses: com.mojang.blaze3d.vertex.PoseStack,
        collector: net.minecraft.client.renderer.SubmitNodeCollector,
        camera: net.minecraft.client.renderer.state.level.CameraRenderState, info: CallbackInfo) {
        net.johnceo.sparklingmutuals.safari.esp.DuplicoGlow.submit(state,poses,collector)
    }
}
