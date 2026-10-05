package net.johnceo.sparklingmutuals.mixin;

import net.johnceo.sparklingmutuals.safari.SafariAssist;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class SafariShinyMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void sparkling$highlight(Entity entity, EntityRenderState state, float delta, CallbackInfo info) {
        if (SafariAssist.isHighlighted(entity.getId())) state.outlineColor = 0xFFFFD700;
    }
}
