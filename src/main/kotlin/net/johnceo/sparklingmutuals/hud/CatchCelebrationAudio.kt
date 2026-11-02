package net.johnceo.sparklingmutuals.hud

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.ui.SparklingAlertStyle
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import kotlin.math.pow

/** SafariUtils synthesized catch score, with owned voices and no changes to any other mod sounds. */
object CatchCelebrationAudio {
    private class Score { var kinds=arrayOf<String>();var songs=arrayOf<Array<Array<FloatArray>>>() }
    private val scores by lazy {
        runCatching { javaClass.getResourceAsStream("/assets/sparkling-mutuals/sparkling_scores.json")!!.reader().use { Gson().fromJson(it,Score::class.java) } }
            .getOrElse { Score() }
    }
    private val stemGains=floatArrayOf(1f,.76f,.72f,.78f,.88f,.82f,1.04f,.9f,.72f,.76f,.66f,.86f,.8f)
    private val anchors=intArrayOf(48,66,84)
    private var started=0L
    private var ends=0L
    private var cycle=-1L
    private var indices=IntArray(13)
    private var current: SparklingAlertStyle?=null
    private val voices=mutableListOf<Voice>()
    private fun now()=System.nanoTime()/1_000_000
    private class Voice(event: SoundEvent,gain: Float,pitch: Float,private val deadline: Long) : AbstractTickableSoundInstance(event,SoundSource.MASTER,RandomSource.create()) {
        val born=now()
        private val initial=gain
        init { volume=gain;this.pitch=pitch;relative=true;attenuation=net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE }
        override fun tick() {
            val left=deadline-now()
            if(left<=0) { stop();return }
            volume=initial*(left/1000f).coerceIn(0f,1f)
        }
    }
    fun start(style: SparklingAlertStyle) { stop();current=style;started=now();ends=started+style.displayMillis();indices=IntArray(13);cycle=-1 }
    fun tick(client: Minecraft) {
        val style=current?:return
        if(client.player==null || now()>=ends) { stop();return }
        val stems=scores.songs.getOrNull(style.soundSong())?:return
        val age=now()-started;val round=age/30000;val within=age%30000
        if(round!=cycle) { indices.fill(0);cycle=round }
        voices.removeAll { it.isStopped || now()-it.born>250 && !client.soundManager.isActive(it) }
        stems.take(13).forEachIndexed { stem,events ->
            val progress=style.soundTheme()-stem
            if(progress>=0) {
                val gain=ConfigManager.sparklingSoundVolume/100f*3.75f*stemGains[stem]*(.58f+minOf(4,progress)*.12f)
                var index=indices[stem]
                while(index<events.size && events[index][0]<=within) {
                    val e=events[index++]
                    // Skip stale notes after loading freezes; do not burst a backlog of sounds.
                    if(e.size>=5 && within-e[0]<150 && gain>0) {
                        val kind=e[1].toInt().coerceIn(scores.kinds.indices)
                        val note=e[2].toInt();val anchor=anchors.minBy { kotlin.math.abs(note-it) }
                        val id=if(kind<9) "sparkling.sample.${scores.kinds[kind]}.$anchor.${e[3].toInt().coerceIn(0,3)}" else "sparkling.sample.${scores.kinds[kind]}"
                        val event=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("sparkling-mutuals",id))
                        val pitch=if(kind<9) 2.0.pow((note-anchor)/12.0).toFloat() else 1f
                        var volume=gain*e[4]
                        while(volume>.01f && voices.size<128) {
                            val sound=Voice(event,minOf(1f,volume),pitch,ends)
                            voices.add(sound);client.soundManager.play(sound);volume-=1f
                        }
                    }
                }
                indices[stem]=index
            }
        }
    }
    fun stop() { val client=Minecraft.getInstance();voices.forEach { client.soundManager.stop(it) };voices.clear();current=null }
}
