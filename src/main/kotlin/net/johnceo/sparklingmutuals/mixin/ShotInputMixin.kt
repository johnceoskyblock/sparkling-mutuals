package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.ShotObservationHooks
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
class ShotInputMixin {
    @Inject(method=["startUseItem"],at=[At("HEAD")])
    private fun start(info:CallbackInfo) { ShotObservationHooks.begin() }
    @Inject(method=["startUseItem"],at=[At("RETURN")])
    private fun end(info:CallbackInfo) { ShotObservationHooks.end() }
}
