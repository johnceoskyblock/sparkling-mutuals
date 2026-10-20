package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.automation.SafariTicketSelect
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Screen::class)
abstract class SafariTicketScreenMixin {
    @Inject(method = ["extractRenderStateWithTooltipAndSubtitles"], at = [At("HEAD")], cancellable = true)
    private fun hideAutoSelectedTicketMenu(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int,
        partialTick: Float, info: CallbackInfo) {
        if (SafariTicketSelect.hideScreen(this as Screen)) info.cancel()
    }
}
