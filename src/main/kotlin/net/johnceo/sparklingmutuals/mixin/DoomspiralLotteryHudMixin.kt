package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.DoomspiralLottery
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud::class)
*///? } else {
@Mixin(net.minecraft.client.gui.Gui::class)
//? }
class DoomspiralLotteryHudMixin {
    @Inject(method = ["extractRenderState"], at = [At("HEAD")], cancellable = true)
    private fun renderLottery(graphics: GuiGraphicsExtractor, delta: DeltaTracker, ci: CallbackInfo) {
        if (DoomspiralLottery.render(graphics)) ci.cancel()
    }
}
