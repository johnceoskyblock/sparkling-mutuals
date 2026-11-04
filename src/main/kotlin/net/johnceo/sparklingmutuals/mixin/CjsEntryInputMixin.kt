package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.automation.entry.CjsSafariEntry
import net.minecraft.client.KeyboardHandler
import net.minecraft.client.MouseHandler
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Observe real events before vanilla updates its bindings; never swallow the user's input. */
@Mixin(KeyboardHandler::class)
class CjsEntryKeyboardMixin {
    @Inject(method=["keyPress"],at=[At("HEAD")])
    private fun cjEntryKey(window: Long, action: Int, event: KeyEvent, info: CallbackInfo) {
        CjsSafariEntry.gameplayInput(window,action)
    }
}

@Mixin(MouseHandler::class)
class CjsEntryMouseMixin {
    @Inject(method=["onButton"],at=[At("HEAD")])
    private fun cjEntryMouse(window: Long, button: MouseButtonInfo, action: Int, info: CallbackInfo) {
        CjsSafariEntry.gameplayInput(window,action)
    }
}
