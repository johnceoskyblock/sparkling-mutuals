package net.johnceo.sparklingmutuals.mixin

import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState
import net.minecraft.world.entity.Display
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(targets=["net.minecraft.client.renderer.entity.DisplayRenderer\$TextDisplayRenderer"])
class CritterTextDisplayMixin {
    @Inject(method=["extractRenderState(Lnet/minecraft/world/entity/Display\$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V"],at=[At("TAIL")])
    private fun names(entity: Display.TextDisplay,state: TextDisplayEntityRenderState,delta: Float,ci: CallbackInfo) {
        val client=Minecraft.getInstance()
        if(!SafariAssist.visualsActive(client))return
        if(net.johnceo.sparklingmutuals.safari.esp.SafariEsp.hideVanillaNameTag(entity)) {
            state.cachedInfo=null
            state.textRenderState=null
            return
        }
        val original=state.cachedInfo ?: return
        var changed=false
        val lines=original.lines().map { line ->
            val shown=CritterNames.sequence(line.contents())
            if(shown===line.contents()) line else {
                changed=true
                Display.TextDisplay.CachedLine(shown,client.font.width(shown))
            }
        }
        if(changed)state.cachedInfo=Display.TextDisplay.CachedInfo(lines,lines.maxOfOrNull {it.width()} ?: 0)
    }
}
