package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.PanelStyle
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Fixed-width HUDs fit custom headings without painting outside their editor bounds. */
object HudHeader {
    fun draw(g: GuiGraphicsExtractor,style: PanelStyle,title: String,color: Int,width: Int,x: Int=6,y: Int=6) {
        val text=style.title(title);val font=HudFonts.font
        val scale=minOf(1f,(width-x-6).coerceAtLeast(1).toFloat()/font.width(text).coerceAtLeast(1))
        g.pose().pushMatrix();g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale)
        g.text(font,text,0,0,style.header.resolve(color),HudFonts.shadow)
        g.pose().popMatrix()
    }
}
