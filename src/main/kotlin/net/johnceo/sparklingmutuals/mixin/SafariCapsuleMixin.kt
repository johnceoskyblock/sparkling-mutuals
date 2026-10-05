package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.CritterCapsuleHider
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(EntityRenderDispatcher::class)
class SafariCapsuleMixin {
    @Inject(method = ["shouldRender"], at = [At("HEAD")], cancellable = true)
    private fun hideCapsule(entity: Entity, frustum: Frustum, x: Double, y: Double, z: Double, result: CallbackInfoReturnable<Boolean>) {
        if (CritterCapsuleHider.hidden(entity, x, y, z)) result.returnValue = false
    }
}
