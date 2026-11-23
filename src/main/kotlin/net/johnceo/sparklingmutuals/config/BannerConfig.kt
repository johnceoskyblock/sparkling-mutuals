package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.hud.BannerKind
import java.util.Properties

data class BannerPreference(val enabled: Boolean,val playback: Int,val text: String,val color: String,val duration: Float,val sound: Int,val volume: Int=85,val pitch: Float=1.2f)
object BannerConfig {
    private fun prefix(kind: BannerKind) = if (kind == BannerKind.PARTY_KICKED) "standard.banner.party_kicked."
        else "banner.${kind.name.lowercase()}."
    val options=BannerKind.entries.associateWith { defaults(it) }.toMutableMap()
    private fun defaults(kind: BannerKind)=BannerPreference(true,0,kind.text,AppearanceConfig.color(kind.rgb),kind.seconds,kind.sound)
    var enabled=true
    var background=true;var color="0:155:18:25:37";var gradient=2;var tint=true;var border=true
    var thickness=1;var font=0;var shadow=false;var topBar=0;var bottomBar=1
    var biomeBorders=false;var localEncounters=true
    fun appearanceSnapshot()=listOf(background,color,gradient,tint,border,thickness,font,shadow,topBar,bottomBar,biomeBorders,localEncounters)
    fun load(p: Properties) {
        fun bool(key: String,default: Boolean)=p.getProperty("banner.$key")?.toBooleanStrictOrNull() ?: default
        fun int(key: String,default: Int,min: Int,max: Int)=p.getProperty("banner.$key")?.toIntOrNull()?.coerceIn(min,max) ?: default
        enabled=bool("enabled",true)
        background=bool("background",true);color=SafariEspConfig.validColor(p.getProperty("banner.color"),"0:155:18:25:37")
        gradient=int("gradient",2,0,2);tint=bool("tint",true);border=bool("border",true);thickness=int("thickness",1,1,4)
        font=int("font",0,0,3);shadow=bool("shadow",false);topBar=int("topBar",0,0,2);bottomBar=int("bottomBar",1,0,2)
        biomeBorders=bool("biomeBorders",false);localEncounters=bool("localEncounters",true)
        val legacy=mapOf(BannerKind.HIDEYHO_LOCATION to "hideyhoInsideOutside",BannerKind.GEMS to "allGemsAlert",BannerKind.BIRD_FOOD to "allBirdFoodAlert",BannerKind.COINS to "allCoinsAlert",BannerKind.INCENSE to "allIncenseAlert",BannerKind.RUN_COMPLETE to "runCompleteAlert")
        BannerKind.entries.filter { it.availableInEdition }.forEach { kind ->
            val base=defaults(kind);val key=prefix(kind)
            fun value(suffix: String) = p.getProperty(key + suffix)
            fun number(suffix: String, default: Int, max: Int) = value(suffix)?.toIntOrNull()?.coerceIn(0,max) ?: default
            options[kind]=base.copy(enabled=value("enabled")?.toBooleanStrictOrNull()
                    ?: legacy[kind]?.let { p.getProperty(it)?.toBooleanStrictOrNull() } ?: true,
                playback=number("playback",0,2),text=(value("text")
                    ?: if(kind==BannerKind.RUN_COMPLETE) p.getProperty("runCompleteTitle",kind.text) else kind.text).take(160).ifBlank { kind.text },
                color=SafariEspConfig.validColor(value("color"),base.color),
                duration=value("duration")?.toFloatOrNull()?.takeIf(Float::isFinite)?.coerceIn(1f,15f) ?: base.duration,
                sound=number("sound",base.sound,55),volume=number("volume",85,100),
                pitch=value("pitch")?.toFloatOrNull()?.takeIf(Float::isFinite)?.coerceIn(.5f,2f) ?: base.pitch)
        }
    }
    fun save(p: Properties) {
        listOf("enabled" to enabled,"background" to background,"color" to color,"gradient" to gradient,"tint" to tint,"border" to border,"thickness" to thickness,"font" to font,
            "shadow" to shadow,"topBar" to topBar,"bottomBar" to bottomBar,"biomeBorders" to biomeBorders,"localEncounters" to localEncounters).forEach { (key,value) -> p.setProperty("banner.$key",value.toString()) }
        options.filterKeys { it.availableInEdition }.forEach { (kind,value) ->
            val prefix=prefix(kind)
            listOf("enabled" to value.enabled,"playback" to value.playback,"text" to value.text,"color" to value.color,"duration" to value.duration,
                "sound" to value.sound,"volume" to value.volume,"pitch" to value.pitch).forEach { (key,setting) -> p.setProperty(prefix+key,setting.toString()) }
        }
    }
}
