package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.ClearWater
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.fog.FogData
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(WaterFogEnvironment::class)
class SafariWaterFogMixin {
    @Inject(method=["setupFog"], at=[At("RETURN")])
    private fun clearWaterFog(fog: FogData, camera: Camera, level: ClientLevel, renderDistance: Float, delta: DeltaTracker, ci: CallbackInfo) {
        if (!ClearWater.active) return
        val far = maxOf(4096f, renderDistance * 2f)
        fog.environmentalStart = far
        fog.environmentalEnd = far + 1024f
        fog.skyEnd = far + 1024f
        fog.cloudEnd = far + 1024f
    }
}
