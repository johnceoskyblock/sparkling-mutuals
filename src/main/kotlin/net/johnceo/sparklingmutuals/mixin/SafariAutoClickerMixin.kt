package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.AutoClicker
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
class SafariAutoClickerMixin {
    @Inject(method = ["handleKeybinds"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0)])
    private fun clicks(info: CallbackInfo) { AutoClicker.prepare(Minecraft.getInstance()) }
}
