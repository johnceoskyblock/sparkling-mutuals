package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariChatFilter
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** This hook runs after Fabric's message events, and never intercepts local PB notifications. */
@Mixin(ChatComponent::class)
abstract class SafariChatMixin {
    @Unique private var smNameRevision=-1L
    @Shadow abstract fun rescaleChat()
    @Inject(method=["extractRenderState"],at=[At("HEAD")])
    private fun refreshNames(info: CallbackInfo) {
        val revision=net.johnceo.sparklingmutuals.safari.ui.CritterNames.revision
        if(smNameRevision!=revision) {smNameRevision=revision;rescaleChat()}
    }
    @Inject(method = ["addServerSystemMessage"], at = [At("HEAD")], cancellable = true)
    private fun hideCaptureSpam(message: Component, result: CallbackInfo) {
        if (net.johnceo.sparklingmutuals.safari.SafariAssist.inSafari &&
            SafariChatFilter.hidden(message.string, ConfigManager.hideCaptureChat)) result.cancel()
    }
}
