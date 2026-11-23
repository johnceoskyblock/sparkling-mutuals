package net.johnceo.sparklingmutuals.mixin

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.safari.StandardSafariKeys
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.MouseHandler
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(KeyboardHandler::class)
class StandardInviteKeyboardMixin {
    @Inject(method = ["keyPress"], at = [At("HEAD")])
    private fun inviteOnPress(window: Long, action: Int, event: KeyEvent, info: CallbackInfo) {
        if (action == InputConstants.PRESS) StandardSafariKeys.inviteOnPress(window, event)
    }
}

@Mixin(MouseHandler::class)
class StandardInviteMouseMixin {
    @Inject(method = ["onButton"], at = [At("HEAD")])
    private fun inviteOnPress(window: Long, button: MouseButtonInfo, action: Int, info: CallbackInfo) {
        if (action == InputConstants.PRESS) StandardSafariKeys.inviteOnPress(window, MouseButtonEvent(0.0, 0.0, button))
    }
}
