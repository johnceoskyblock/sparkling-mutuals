package net.johnceo.sparklingmutuals.config

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.johnceo.sparklingmutuals.config.ThemedButton as Button
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.ConfirmLinkScreen

/** Read-only status and release notes; updates are opened in the user's browser. */
class UpdateDetailsScreen(private val parent: Screen) : Screen(Component.literal("Sparkling Mutuals updates")) {
    private var offset=0
    private var maximum=0
    private var notesTop=0
    private var thumbY=0
    private var thumbHeight=0
    private var dragging=false
    private var grab=0.0
    private val left get()=(width-(width-32).coerceAtMost(740))/2
    private val right get()=width-left
    private val notesBottom get()=height-52
    override fun init() {
        dragging=false
        ModUpdates.check(force=true)
        addRenderableWidget(Button.builder(Component.literal("GitHub releases")) { ConfirmLinkScreen.confirmLinkNow(this,java.net.URI.create(ModUpdates.REPOSITORY+"/releases")) }.bounds(left,height-34,130,22).build())
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }.bounds(right-60,height-34,60,22).build())
    }
    override fun onClose() { minecraft.smSetScreen(parent) }
    override fun tick() { ModUpdates.check() }
    override fun isPauseScreen()=false
    override fun extractRenderState(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        g.fill(0,0,width,height,SettingsPalette.background or 0xFF000000.toInt())
        g.fill(left,12,right,height-44,SettingsPalette.surface or 0xFF000000.toInt())
        g.fill(left,12,left+3,70,SettingsPalette.accent)
        val x=left+14;val textWidth=right-x-20
        g.text(font,Component.literal("Sparkling Mutuals").withStyle { it.withBold(true) },x,23,SettingsPalette.text,false)
        val status=ModUpdates.label
        g.text(font,status,x,40,if(status=="Up to Date") SettingsPalette.green else SettingsPalette.accent,false)
        val installed=font.split(Component.literal("Installed ${ModUpdates.installed}  ·  Minecraft ${ModUpdates.minecraftVersion}"),textWidth)
        installed.forEachIndexed { i,line -> g.text(font,line,x,56+i*11,SettingsPalette.muted,false) }
        var y=56+installed.size*11+15
        val found=ModUpdates.release
        val releaseTitle=found?.name?.ifBlank { found.tag } ?: "Release notes"
        val heading=font.split(Component.literal(releaseTitle).withStyle { it.withBold(true) },textWidth)
        heading.forEachIndexed { i,line -> g.text(font,line,x,y+i*11,SettingsPalette.gold,false) }
        y+=heading.size*11+10
        g.fill(x,y-3,right-14,y-2,SettingsPalette.border)
        notesTop=y+7
        data class Row(val block: ReleaseNotes.Block,val lines: List<net.minecraft.util.FormattedCharSequence>,val height: Int)
        val rows=ReleaseNotes.parse(found?.notes ?: "Release status is unavailable. View GitHub releases to check manually.").map { block ->
            val component=Component.literal(block.text).withStyle { it.withBold(block.kind==ReleaseNotes.Kind.HEADING) }
            val lines=font.split(component,textWidth-if(block.kind==ReleaseNotes.Kind.BULLET) 12 else 0)
            Row(block,lines,if(block.kind==ReleaseNotes.Kind.SPACE) 8 else lines.size*12+if(block.kind==ReleaseNotes.Kind.HEADING) 12 else 7)
        }
        val viewport=(notesBottom-notesTop).coerceAtLeast(1)
        maximum=(rows.sumOf { it.height }-viewport).coerceAtLeast(0);offset=offset.coerceIn(0,maximum)
        g.enableScissor(x,notesTop,right-14,notesBottom)
        y=notesTop-offset
        rows.forEach { row ->
            if(row.block.kind!=ReleaseNotes.Kind.SPACE) {
                if(row.block.kind==ReleaseNotes.Kind.BULLET) g.text(font,"•",x,y,SettingsPalette.accent,false)
                val indent=if(row.block.kind==ReleaseNotes.Kind.BULLET) 12 else 0
                row.lines.forEachIndexed { i,line -> g.text(font,line,x+indent,y+i*12,
                    if(row.block.kind==ReleaseNotes.Kind.HEADING) SettingsPalette.gold else SettingsPalette.text,false) }
            }
            y+=row.height
        }
        g.disableScissor()
        if(maximum>0) {
            thumbHeight=(viewport.toLong()*viewport/(viewport+maximum)).toInt().coerceIn(minOf(18,viewport),viewport)
            thumbY=notesTop+((viewport-thumbHeight).toLong()*offset/maximum).toInt()
            g.fill(right-7,notesTop,right-4,notesBottom,SettingsPalette.border)
            g.fill(right-7,thumbY,right-4,thumbY+thumbHeight,SettingsPalette.accent)
        }
        super.extractRenderState(g,mx,my,delta)
    }
    override fun mouseScrolled(mouseX: Double,mouseY: Double,scrollX: Double,scrollY: Double): Boolean {
        offset=(offset-(scrollY*30).toInt()).coerceIn(0,maximum);return true
    }
    override fun mouseClicked(event: MouseButtonEvent,doubleClick: Boolean): Boolean {
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT && maximum>0 && event.x()>=right-11 && event.x()<right && event.y()>=notesTop && event.y()<notesBottom) {
            dragging=true;grab=if(event.y()>=thumbY && event.y()<thumbY+thumbHeight) event.y()-thumbY else thumbHeight/2.0
            drag(event.y());return true
        }
        return super.mouseClicked(event,doubleClick)
    }
    private fun drag(y: Double) { offset=(((y-notesTop-grab)/(notesBottom-notesTop-thumbHeight).coerceAtLeast(1))*maximum).toInt().coerceIn(0,maximum) }
    override fun mouseDragged(event: MouseButtonEvent,dx: Double,dy: Double): Boolean {
        if(dragging) { drag(event.y());return true };return super.mouseDragged(event,dx,dy)
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if(dragging && event.button()==InputConstants.MOUSE_BUTTON_LEFT) { dragging=false;return true };return super.mouseReleased(event)
    }
}
