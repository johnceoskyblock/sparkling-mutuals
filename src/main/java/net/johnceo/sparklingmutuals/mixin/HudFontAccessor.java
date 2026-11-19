package net.johnceo.sparklingmutuals.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Font.class)
public interface HudFontAccessor {
    @Accessor("provider")
    Font.Provider sm$getProvider();
}
