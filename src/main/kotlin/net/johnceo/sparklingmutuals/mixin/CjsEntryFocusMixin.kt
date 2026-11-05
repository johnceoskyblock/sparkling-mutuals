package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.ModifyExpressionValue
import net.johnceo.sparklingmutuals.safari.automation.entry.CjsSafariEntry
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(Minecraft::class)
class CjsEntryFocusMixin {
    @ModifyExpressionValue(method = ["pauseIfInactive"], at = [At(value = "FIELD",
        target = "Lnet/minecraft/client/Options;pauseOnLostFocus:Z")])
    private fun pauseOnFocusLoss(enabled: Boolean): Boolean =
        enabled && !CjsSafariEntry.allowsBackgroundEntry(Minecraft.getInstance())
}
