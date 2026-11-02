// Contest panel layout and ticket status adapted from SafariUtils ContestTracker/HudBorderStyle (MIT).
package net.johnceo.sparklingmutuals.contest

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.hud.*
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.Locale

object ContestHud {
    const val WIDTH=180
    const val HEIGHT=68
    val previewHeight get()=HEIGHT-(if(ContestConfig.showBracket) 0 else 11)-(if(ContestConfig.showScore) 0 else 11)
    private fun panelHeight(inactive: Boolean)=if(inactive) 34 else previewHeight
    fun register() { }
    fun render(g: GuiGraphicsExtractor,delta: DeltaTracker) {
        val client=Minecraft.getInstance()
        if(client.player==null || client.level==null || client.options.smHideGui || client.smScreen is ContestGui ||
            !ContestConfig.trackContest || !ContestConfig.showContestTimer || !ContestTracker.isActive() || !SkyblockSidebar.inSkyblock(client)) return
        val w=client.window
        val bounds=ContestConfig.layoutFor(w.guiScaledWidth,w.guiScaledHeight).bounds(w.guiScaledWidth,w.guiScaledHeight,WIDTH,panelHeight(ContestTracker.betweenContests()))
        renderAt(g,bounds.x,bounds.y,false)
    }
    fun renderPreview(g: GuiGraphicsExtractor,x: Int,y: Int)=renderAt(g,x,y,true)
    private fun renderAt(g: GuiGraphicsExtractor,x: Int,y: Int,preview: Boolean) {
        val client=Minecraft.getInstance();val w=client.window
        val inactive=!preview && ContestTracker.betweenContests()
        val scale=ContestConfig.layoutFor(w.guiScaledWidth,w.guiScaledHeight).effectiveScale(w.guiScaledWidth,w.guiScaledHeight,WIDTH,panelHeight(inactive))
        val earned=preview || ContestTracker.isComplete()
        val tier=if(preview) "Uncommon" else ContestTracker.currentTier()
        val theme=HudThemes.ticketColor(earned,inactive,tier)
        val style=AppearanceConfig.panels.getValue("miria")
        g.pose().pushMatrix();g.pose().translate(x.toFloat(),y.toFloat());g.pose().scale(scale,scale)
        style.draw(g,WIDTH,panelHeight(inactive),if(BannerConfig.biomeBorders) theme else null)
        g.text(client.font,"Miria's Contest",6,6,if(BannerConfig.biomeBorders) theme else HudRow.GOLD,true)
        fun row(label: String,value: String,top: Int,color: Int) {
            g.text(client.font,label,6,top,0xFFCCCCCC.toInt(),true)
            g.text(client.font,value,WIDTH-6-client.font.width(value),top,color,true)
        }
        val seconds=if(preview) 300 else if(inactive) ContestTracker.secondsToNextStart() else ContestTracker.secondsToEnd()
        row(if(inactive) "Contest Starting" else "Contest Time",ContestTracker.formatRemaining(seconds),18,
            if(inactive || seconds>300) 0xFF55FF55.toInt() else if(seconds>60) 0xFFFFFF55.toInt() else 0xFFFF5555.toInt())
        if(!inactive) {
            var top=29
            if(ContestConfig.showBracket) { row("Bracket",tier.ifBlank { "—" },top,if(tier.isBlank()) HudRow.GRAY else HudThemes.tierColor(tier));top+=11 }
            val score=if(preview) 1234 else ContestTracker.currentPoints()
            if(ContestConfig.showScore) { row("Score",if(score<0) "—" else String.format(Locale.ROOT,"%,d",score),top,0xFFFFCC66.toInt());top+=11 }
            g.text(client.font,if(ContestConfig.firstClassTicket) "First-Class Ticket" else "Basic Ticket",6,top,0xFFCCCCCC.toInt(),true)
            UiIcons.draw(g,WIDTH-17,top,if(earned) UiIcons.Kind.CHECK else UiIcons.Kind.CROSS,
                if(earned) 0xFF55FF55.toInt() else 0xFFFF5555.toInt())
        }
        g.pose().popMatrix()
    }
}
