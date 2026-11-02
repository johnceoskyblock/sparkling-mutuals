// Adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT. See licenses/SafariUtils-MIT.txt.
package net.johnceo.sparklingmutuals.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the extracted GUI state so related quads can be submitted as one batch. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {
	@Accessor("guiRenderState")
	GuiRenderState sm$getGuiRenderState();
}
