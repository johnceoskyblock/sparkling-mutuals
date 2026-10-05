package net.johnceo.sparklingmutuals.mixin;

import io.github.notenoughupdates.moulconfig.platform.MoulConfigRenderContext;
import net.johnceo.sparklingmutuals.config.SafariTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MoulConfigRenderContext.class, remap = false)
public class SafariMoulThemeMixin {
    @ModifyVariable(method = "drawColoredRect", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparkling$rectangle(int color) { return SafariTheme.color(color); }
    @ModifyVariable(method = "drawString", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int sparkling$text(int color) { return SafariTheme.color(color); }
    @ModifyConstant(method = "drawDarkRect", constant = {
        @Constant(intValue = 0xFF202026), @Constant(intValue = 0xFF303036), @Constant(intValue = 0xFF101016)
    })
    private int sparkling$panel(int color) { return SafariTheme.color(color); }
}
