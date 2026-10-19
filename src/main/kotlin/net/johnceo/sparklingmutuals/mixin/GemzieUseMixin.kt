package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.automation.GemzieAutoSelect
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(Minecraft::class)
abstract class GemzieUseMixin {
    @Shadow protected abstract fun startUseItem()
    @Shadow private var rightClickDelay:Int=0

    // Ordinal 0 follows keyUse.consumeClick(); ordinal 1 is the vanilla held-input repeat.
    @Redirect(method=["handleKeybinds"],at=[At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;startUseItem()V",ordinal=0)])
    private fun gemzieFreshUse(client:Minecraft) {
        if(GemzieAutoSelect.handle(client,true)) rightClickDelay=4 else startUseItem()
    }

    @Redirect(method=["handleKeybinds"],at=[At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;startUseItem()V",ordinal=1)])
    private fun gemzieHeldUse(client:Minecraft) {
        if(GemzieAutoSelect.handle(client,false)) rightClickDelay=4 else startUseItem()
    }
}
