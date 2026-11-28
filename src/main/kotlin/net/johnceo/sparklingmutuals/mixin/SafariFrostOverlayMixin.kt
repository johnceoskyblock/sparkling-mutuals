// Adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT. See licenses/SafariUtils-MIT.txt.
package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.minecraft.client.Minecraft
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud as CameraHud
*///? } else {
import net.minecraft.client.gui.Gui as CameraHud
//? }
import net.minecraft.client.player.LocalPlayer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At

@Mixin(CameraHud::class)
class SafariFrostOverlayMixin {
    @WrapOperation(method = ["extractCameraOverlays"],
        at = [At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTicksFrozen()I")])
    private fun frostTicks(player: LocalPlayer, original: Operation<Int>): Int =
        if (ConfigManager.removeIcyFrost && SafariAssist.biome == SafariBiome.ICY &&
            SafariAssist.visualsActive(Minecraft.getInstance())) 0
        else original.call(player)
}
