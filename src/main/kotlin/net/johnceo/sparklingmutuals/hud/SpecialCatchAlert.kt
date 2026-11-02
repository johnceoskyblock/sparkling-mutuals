package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
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
    private fun now()=System.nanoTime()/1_000_000
    fun caught(player: String,species: String,unique: Boolean) {
        if(!unique || !SafariAssist.inSafari || !ConfigManager.specialCatchAlert) return
        val current=SafariTracking.ledger.current?:return
        val key="$player:$species"
        val time=now()
        if(current===run && key==lastCatch && time-lastAt<2000) return
        lastCatch=key;lastAt=time;run=current
        show(species,false)
    }
    fun preview() { show("Macaw",true) }
    private fun show(species: String,preview: Boolean) {
        subject=species;shownAt=now();previewing=preview
        style=SparklingAlertStyle.preset(ConfigManager.specialCatchStyle)
        CatchCelebrationAudio.start(style)
    }
    fun tick(client: Minecraft) {
        if(subject!=null && (client.player==null && !previewing || now()-shownAt>=style.displayMillis() || !previewing && !SafariAssist.inSafari)) { reset();return }
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
        SparklingCatchEffects.render(g,w,h,alpha,age,ConfigManager.specialCatchStyle)
        fun centered(text: String,desired: Float,y: Float,color: Int,rainbow: Boolean=false) {
            val scale=min(desired,(w-20f)/client.font.width(text).coerceAtLeast(1))
            g.pose().pushMatrix()
            val shake=SparklingCatchEffects.textShakeOffset(age,style.textShake(),text.hashCode())
            g.pose().translate(shake.toFloat(),SparklingCatchEffects.textShakeOffset(age,style.textShake(),text.hashCode() xor 0x27D4).toFloat())
            g.pose().scale(scale,scale)
            val x=(w/scale-client.font.width(text))/2
            if(rainbow) {
                var cursor=x
                val phase=if(style.textGradientSpeed()==0) 0f else age.toFloat()/(8000f/style.textGradientSpeed())
                text.forEach { ch ->
                    val letter=ch.toString();val rgb=java.awt.Color.HSBtoRGB(phase+(cursor-x)/client.font.width(text).coerceAtLeast(1)*.5f,.5f,1f)
                    g.text(client.font,letter,cursor.toInt(),(y/scale).toInt(),(alpha shl 24) or (rgb and 0xFFFFFF),true);cursor+=client.font.width(letter)
                }
            } else g.centeredText(client.font,Component.literal(text),(w/(2*scale)).toInt(),(y/scale).toInt(),(alpha shl 24) or (color and 0xFFFFFF))
            g.pose().popMatrix()
        }
        val titleY=h*.396f
        val titleScale=min(4.7f,(w-20f)/client.font.width("SPARKLING!").coerceAtLeast(1))
        val subjectY=maxOf(h*.517f,titleY+client.font.lineHeight*titleScale+6)
        centered("SPARKLING!",titleScale,titleY,0xFFD700,true)
        centered(name,3f,subjectY,SafariRoster.color(name))
        if(style.callout()>0) {
            centered("SPARKLING CAPTURE",1f,h/7f,-1,true)
            if(style.callout()>=2) centered("SPARKLING CAPTURE",1f,h-h/7f-9,-1,true)
        }
    }
    fun reset() { subject=null;run=null;lastCatch="";lastAt=0;previewing=false;CatchCelebrationAudio.stop() }
}
