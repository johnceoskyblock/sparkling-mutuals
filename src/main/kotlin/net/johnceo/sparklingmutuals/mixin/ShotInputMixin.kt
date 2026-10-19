package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.shots.ShotObservationHooks
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Minecraft::class)
class ShotInputMixin {
    @Inject(method=["handleKeybinds"],at=[At("HEAD")])
    private fun startingItems(info:CallbackInfo) {
        val client = Minecraft.getInstance()
        if (client.options.keyUse.isDown || client.options.keyAttack.isDown)
            net.johnceo.sparklingmutuals.safari.SafariTracking.beforeResourceInteraction(client)
    }
    @Inject(method=["startAttack"],at=[At("HEAD")])
    private fun attack(info:CallbackInfoReturnable<Boolean>) {
        net.johnceo.sparklingmutuals.safari.SafariTracking.beforeResourceInteraction(Minecraft.getInstance())
    }
    @Inject(method=["runTick"],at=[At("HEAD")])
    private fun previewFrame(renderLevel:Boolean,info:CallbackInfo) {
        net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePreview.beginFrame()
    }
    @Inject(method=["startUseItem"],at=[At("HEAD")])
    private fun start(info:CallbackInfo) {
        net.johnceo.sparklingmutuals.safari.SafariTracking.beforeResourceInteraction(Minecraft.getInstance())
        ShotObservationHooks.begin()
    }
    @Inject(method=["startUseItem"],at=[At("RETURN")])
    private fun end(info:CallbackInfo) { ShotObservationHooks.end() }
}
