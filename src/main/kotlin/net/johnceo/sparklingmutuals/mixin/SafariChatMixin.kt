package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariChatFilter
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** This hook runs after Fabric's message events, and never intercepts local PB notifications. */
@Mixin(ChatComponent::class)
class SafariChatMixin {
    @Inject(method = ["addServerSystemMessage"], at = [At("HEAD")], cancellable = true)
    private fun hideCaptureSpam(message: Component, result: CallbackInfo) {
        if (SafariChatFilter.hidden(message.string, ConfigManager.hideCaptureChat)) result.cancel()
    }
}
