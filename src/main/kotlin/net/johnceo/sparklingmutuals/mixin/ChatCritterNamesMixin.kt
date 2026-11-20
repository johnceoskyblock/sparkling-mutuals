package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.network.chat.FormattedText
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyArg

@Mixin(GuiMessage::class)
abstract class ChatCritterNamesMixin {
    @Shadow abstract fun source(): GuiMessageSource
    // Change only wrapped display lines, never stored chat, log output, or Fabric receive events.
    @ModifyArg(method=["splitLines"],at=[At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;")],index=0)
    private fun visualNames(text: FormattedText): FormattedText =
        if(source()==GuiMessageSource.SYSTEM_SERVER && SkyblockSidebar.inSkyblock(Minecraft.getInstance())) CritterNames.formatted(text) else text
}
