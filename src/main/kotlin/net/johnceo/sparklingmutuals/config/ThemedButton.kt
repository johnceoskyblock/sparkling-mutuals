package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.ui.CritterNames
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

/** Native button interaction/narration, with the same surfaces as the settings workspace. */
class ThemedButton private constructor(x: Int,y: Int,w: Int,h: Int,label: Component,action: OnPress, private val primary: Boolean) : Button(x,y,w,h,label,action,DEFAULT_NARRATION) {
    override fun extractContents(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val highlighted=isHoveredOrFocused
        val fill = when {
            highlighted && active -> if (primary) SettingsPalette.selected else SettingsPalette.hover
            primary && active -> SettingsPalette.hover
            else -> SettingsPalette.card
        }
        g.fill(x,y,x+width,y+height,fill)
        val border=if((highlighted || primary) && active) SettingsPalette.accent else SettingsPalette.border
        g.fill(x,y,x+width,y+1,border);g.fill(x,y+height-1,x+width,y+height,border)
        g.fill(x,y,x+1,y+height,border);g.fill(x+width-1,y,x+width,y+height,border)
        val font=net.johnceo.sparklingmutuals.hud.HudFonts.font
        val shown=font.plainSubstrByWidth(CritterNames.display(message.string),(width-8).coerceAtLeast(1))
        val text=CritterNames.mark(Component.literal(shown).visualOrderText)
        g.text(font,text,x+(width-font.width(text))/2,y+(height-8)/2,if(active) SettingsPalette.text else SettingsPalette.muted,false)
    }
    class Builder(private val message: Component,private val action: OnPress) {
        private var x=0;private var y=0;private var w=150;private var h=20
        private var primary = false
        fun bounds(x: Int,y: Int,w: Int,h: Int)=apply { this.x=x;this.y=y;this.w=w;this.h=h }
        fun width(w: Int)=apply { this.w=w }
        fun primary()=apply { primary=true }
        fun build()=ThemedButton(x,y,w,h,message,action,primary)
    }
    companion object { fun builder(message: Component,action: OnPress)=Builder(message,action) }
}
