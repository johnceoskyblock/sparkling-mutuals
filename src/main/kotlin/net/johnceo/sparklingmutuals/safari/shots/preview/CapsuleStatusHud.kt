package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.hud.HudLayout
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/** One independently positioned text block; guide geometry remains in world space. */
object CapsuleStatusHud {
    val layout=HudLayout(.35f,.55f)
    private const val PREVIEW="Uncalibrated · Provisional\nPossible critter contact\nOther critters unverified"
    val previewWidth get()=PREVIEW.lines().maxOf(Minecraft.getInstance().font::width)
    val previewHeight get()=3*Minecraft.getInstance().font.lineHeight
    fun render(graphics:GuiGraphicsExtractor,status:String) {
        val font=Minecraft.getInstance().font
        val lines=font.split(Component.literal(status),((graphics.guiWidth()-16)/layout.scale).toInt().coerceAtLeast(1))
        val width=lines.maxOfOrNull(font::width) ?: 0
        val bounds=layout.bounds(graphics.guiWidth(),graphics.guiHeight(),width,lines.size*font.lineHeight)
        draw(graphics,lines,width,bounds.x,bounds.y)
    }
    fun renderPreview(graphics:GuiGraphicsExtractor,x:Int,y:Int) {
        val lines=PREVIEW.lines().map{Component.literal(it).visualOrderText}
        draw(graphics,lines,previewWidth,x,y)
    }
    private fun draw(graphics:GuiGraphicsExtractor,lines:List<FormattedCharSequence>,width:Int,x:Int,y:Int) {
        val font=Minecraft.getInstance().font
        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(),y.toFloat());graphics.pose().scale(layout.scale,layout.scale)
        lines.forEachIndexed { index,line -> graphics.text(font,line,(width-font.width(line))/2,index*font.lineHeight,0xFFFFB347.toInt(),true) }
        graphics.pose().popMatrix()
    }
}
