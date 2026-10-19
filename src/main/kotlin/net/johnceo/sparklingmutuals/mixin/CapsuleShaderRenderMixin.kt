package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreviewRenderer
import net.minecraft.client.renderer.LevelRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LevelRenderer::class)
abstract class CapsuleShaderRenderMixin {
    // Later targets submit geometry through the frame graph and need their own validated shader integration.
    //? if <26.2 {
    @Inject(method = ["renderLevel"], at = [At("RETURN")])
    private fun capsuleAfterShaders(ci: CallbackInfo) { CapsulePreviewRenderer.afterShaders() }
    //? }
}
