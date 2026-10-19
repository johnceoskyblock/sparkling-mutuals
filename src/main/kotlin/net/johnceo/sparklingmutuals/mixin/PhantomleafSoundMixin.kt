package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.misc.phantomleaf.PhantomleafSolver
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundEngine
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Read resolved sound values, before the player's volume sliders or distance attenuation. */
@Mixin(SoundEngine::class)
class PhantomleafSoundMixin {
    // getPitch/getVolume dereference the resolved Sound. HEAD is unsafe, including for menu music.
    @Inject(method = ["play"], at = [At(value = "INVOKE",
        target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F", ordinal = 0)])
    private fun phantomleafSound(sound: SoundInstance, ci: CallbackInfoReturnable<SoundEngine.PlayResult>) {
        PhantomleafSolver.sound(sound.identifier.toString(), sound.pitch, sound.volume)
    }
}
