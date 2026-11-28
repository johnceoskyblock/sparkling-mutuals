package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.DoomspiralLottery
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundEngine
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(SoundEngine::class)
class DoomspiralLotterySoundMixin {
    @Inject(method = ["play"], at = [At("HEAD")], cancellable = true)
    private fun delaySparklingChime(sound: SoundInstance, result: CallbackInfoReturnable<SoundEngine.PlayResult>) {
        if (DoomspiralLottery.deferChime(sound)) result.returnValue = SoundEngine.PlayResult.NOT_STARTED
    }
}
