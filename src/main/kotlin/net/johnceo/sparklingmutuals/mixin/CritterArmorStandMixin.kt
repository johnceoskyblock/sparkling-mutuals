package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.esp.CritterGlowState
import net.minecraft.client.renderer.entity.ArmorStandRenderer
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ArmorStandRenderer::class)
class CritterArmorStandMixin {
    // Suppress only the wooden body. LivingEntityRenderer still submits head/equipment layers
    // with outlineColor, including skulls on otherwise invisible armor stands.
    @Inject(method=["getRenderType(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;"],at=[At("HEAD")],cancellable=true)
    private fun headOnly(state: ArmorStandRenderState, visible: Boolean, translucent: Boolean, glowing: Boolean,
        ci: CallbackInfoReturnable<RenderType?>) {
        if ((state as CritterGlowState).`sm$isCritterGlow`()) ci.returnValue = null
    }
}
