package net.johnceo.sparklingmutuals.mixin

import io.github.notenoughupdates.moulconfig.platform.MoulConfigRenderContext
import net.johnceo.sparklingmutuals.config.SafariTheme
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.*

@Mixin(value = [MoulConfigRenderContext::class], remap = false)
class SafariMoulThemeMixin {
    @ModifyVariable(method = ["drawColoredRect"], at = [At("HEAD")], argsOnly = true, ordinal = 0)
    private fun rectangle(color: Int) = SafariTheme.color(color)
    @ModifyVariable(method = ["drawString"], at = [At("HEAD")], argsOnly = true, ordinal = 2)
    private fun text(color: Int) = SafariTheme.color(color)
    @ModifyConstant(method = ["drawDarkRect"], constant = [Constant(intValue = -0xDFDFDA), Constant(intValue = -0xCFCFCA), Constant(intValue = -0xEFEFEA)])
    private fun panel(color: Int) = SafariTheme.color(color)
}
