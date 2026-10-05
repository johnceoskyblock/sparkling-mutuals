package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.painting.Painting
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Render-only painting suppression adapted from ShinyHunter (MIT). */
@Mixin(EntityRenderDispatcher::class)
class SafariPaintingMixin {
    @Inject(method = ["shouldRender"], at = [At("HEAD")], cancellable = true)
    private fun hidePainting(entity: Entity, frustum: Frustum, x: Double, y: Double, z: Double, result: CallbackInfoReturnable<Boolean>) {
        if (entity is Painting && SafariAssist.hidePaintings()) result.returnValue = false
    }
}
