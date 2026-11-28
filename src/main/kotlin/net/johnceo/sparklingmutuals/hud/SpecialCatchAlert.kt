package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.CatchCelebrationPreviewScreen
import net.johnceo.sparklingmutuals.config.CelebrationRecipe
import net.johnceo.sparklingmutuals.config.CustomCatchCelebrations
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.ui.SparklingAlertStyle
import net.johnceo.sparklingmutuals.ui.SparklingCatchEffects
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import kotlin.math.min

/** Only confirmed server catch lines reach this presentation; it never changes completion or history. */
object SpecialCatchAlert {
    private var subject: String?=null
    private var shownAt=0L
    private var run: Any?=null
    private var lastCatch=""
    private var lastAt=0L
    private var previewing=false
    private var style=SparklingAlertStyle.preset(0)
    private var caption = "SPARKLING CAPTURE"
    private var detail = 2
    private fun now()=System.nanoTime()/1_000_000
    fun caught(player: String,species: String,unique: Boolean) {
        if (!SafariAssist.inSafari || !(ConfigManager.alwaysCelebrateSparkling || unique && ConfigManager.specialCatchAlert)) return
        val current=SafariTracking.ledger.current?:return
        val key="$player:$species"
        val time=now()
        if(current===run && key==lastCatch && time-lastAt<2000) return
        lastCatch=key;lastAt=time;run=current
        enqueue(species, false, null) { SafariTracking.ledger.current === current && SafariAssist.inSafari }
    }
    internal fun preview(recipe: CelebrationRecipe? = null) {
        closePreview()
        enqueue("Macaw", true, recipe) { Minecraft.getInstance().smScreen is CatchCelebrationPreviewScreen }
    }
    private var previewAudio = false
    fun closePreview() {
        SafariAlertSequence.queue.clear(this) { it == "preview" }
        if (previewAudio) CatchCelebrationAudio.stop()
        previewAudio = false
    }
    private fun enqueue(species: String, preview: Boolean, previewRecipe: CelebrationRecipe?, valid: () -> Boolean) {
        val recipe = previewRecipe ?: CustomCatchCelebrations.active.takeIf { ConfigManager.specialCatchStyle == SparklingAlertStyle.CUSTOM_INDEX }
        val selected = recipe?.style() ?: SparklingAlertStyle.preset(ConfigManager.specialCatchStyle)
        SafariAlertSequence.queue.submit(this, selected.displayMillis(), if (preview) "preview" else null,
            valid = valid, finish = ::finish) { show(species, preview, selected, recipe) }
    }
    private fun show(species: String, preview: Boolean, selected: SparklingAlertStyle, recipe: CelebrationRecipe?) {
        subject=species;shownAt=now();previewing=preview
        style=selected
        caption = recipe?.caption ?: "SPARKLING CAPTURE"
        detail = recipe?.detail ?: 2
        previewAudio = preview
        CatchCelebrationAudio.start(style)
    }
    fun tick(client: Minecraft) {
        CatchCelebrationAudio.tick(client)
    }
    fun render(g: GuiGraphicsExtractor) {
        val name=subject?:return
        val client=Minecraft.getInstance()
        if(client.player==null && !previewing || client.options.smHideGui) return
        val age=now()-shownAt;val duration=style.displayMillis()
        if(age>=duration) return
        val alpha=(255*min(1.0,(duration-age)/min(1200.0,duration/3.0))).toInt().coerceIn(0,255)
        val w=client.window.guiScaledWidth;val h=client.window.guiScaledHeight
        val cycle=if(style.pulse()==0) 3600 else (1180-style.pulse()*130).coerceAtLeast(480)
        val washAlpha=if(style.pulse()==0) 22 else (18+style.pulse()*7+(18+style.pulse()*7)*(.5+.5*kotlin.math.sin(age*Math.PI/120))).toInt()
        val hue=(age%cycle).toFloat()/cycle
        g.fill(0,0,w,h,(min(alpha,washAlpha) shl 24) or (java.awt.Color.HSBtoRGB(hue,.72f,1f) and 0xFFFFFF))
        SparklingCatchEffects.render(g,w,h,alpha,age,style,detail)
        fun centered(text: String,desired: Float,y: Float,color: Int,rainbow: Boolean=false) {
            val scale=min(desired,(w-20f)/HudFonts.font.width(text).coerceAtLeast(1))
            g.pose().pushMatrix()
            val shake=SparklingCatchEffects.textShakeOffset(age,style.textShake(),text.hashCode())
            g.pose().translate(shake.toFloat(),SparklingCatchEffects.textShakeOffset(age,style.textShake(),text.hashCode() xor 0x27D4).toFloat())
            g.pose().scale(scale,scale)
            val x=(w/scale-HudFonts.font.width(text))/2
            if(rainbow) {
                var cursor=x
                val phase=if(style.textGradientSpeed()==0) 0f else age.toFloat()/(8000f/style.textGradientSpeed())
                text.forEach { ch ->
                    val letter=ch.toString();val rgb=java.awt.Color.HSBtoRGB(phase+(cursor-x)/HudFonts.font.width(text).coerceAtLeast(1)*.5f,.5f,1f)
                    g.text(HudFonts.font,letter,cursor.toInt(),(y/scale).toInt(),(alpha shl 24) or (rgb and 0xFFFFFF),HudFonts.shadow);cursor+=HudFonts.font.width(letter)
                }
            } else g.text(HudFonts.font,Component.literal(text),(w/(2*scale)).toInt()-HudFonts.font.width(text)/2,(y/scale).toInt(),(alpha shl 24) or (color and 0xFFFFFF),HudFonts.shadow)
            g.pose().popMatrix()
        }
        val titleY=h*.396f
        val titleScale=min(4.7f,(w-20f)/HudFonts.font.width("SPARKLING!").coerceAtLeast(1))
        val subjectY=maxOf(h*.517f,titleY+HudFonts.font.lineHeight*titleScale+6)
        centered("SPARKLING!",titleScale,titleY,0xFFD700,true)
        centered(name,3f,subjectY,SafariRoster.color(name))
        if(style.callout()>0 && caption.isNotBlank()) {
            centered(caption,1f,h/7f,-1,true)
            if(style.callout()>=2) centered(caption,1f,h-h/7f-9,-1,true)
        }
    }
    private fun finish() { subject=null;previewing=false }
    fun reset() { SafariAlertSequence.queue.clear(this);CatchCelebrationAudio.stop();previewAudio=false;run=null;lastCatch="";lastAt=0 }
}
