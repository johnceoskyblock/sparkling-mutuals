package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.johnceo.sparklingmutuals.skyblock.market.HuntingBoxShardParsing
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyArg

//? if >=26.3 {
/*private const val BOX_TOOLTIP_TARGET="Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"
*///? } else {
private const val BOX_TOOLTIP_TARGET="Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
//? }

@Mixin(GuiGraphicsExtractor::class)
class HuntingBoxNamesMixin {
    // Only the rendered tooltip copy changes, after item tooltip callbacks and item parsing.
    @ModifyArg(method=["setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"],
        at=[At(value="INVOKE",target=BOX_TOOLTIP_TARGET)],index=1)
    private fun names(lines: List<Component>): List<Component> {
        val client=Minecraft.getInstance()
        val screen=client.smScreen as? AbstractContainerScreen<*> ?: return lines
        if(!SkyblockSidebar.inSkyblock(client) || !HuntingBoxShardParsing.isBox(screen.title.string))return lines
        return CritterNames.tooltip(lines)
    }
}
