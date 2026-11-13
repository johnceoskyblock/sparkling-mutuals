package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import kotlin.math.ceil
import kotlin.math.roundToInt

data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
}

enum class HudExpansion(val horizontal: Float,val vertical: Float) {
    DOWN_RIGHT(0f,0f), DOWN(.5f,0f), DOWN_LEFT(1f,0f), RIGHT(0f,.5f),
    CENTER(.5f,.5f), LEFT(1f,.5f), UP_RIGHT(0f,1f), UP(.5f,1f), UP_LEFT(1f,1f)
}

/** Normalized positions keep panels on screen when Minecraft's GUI resolution changes. */
class HudLayout(private val defaultX: Float, private val defaultY: Float, private val defaultScale: Float = 1f,
    private val defaultReferenceWidth: Float=0f, private val defaultReferenceHeight: Float=0f) {
    var hasSavedPosition = false
        private set
    var x = defaultX
    var y = defaultY
    var scale = defaultScale
        set(value) { field = if (value.isFinite()) value.coerceIn(.15f, 2f) else defaultScale }
    private var basePanelWidth=0
    private var basePanelHeight=0
    private var lastGeometry: IntArray?=null
    var expansion=HudExpansion.DOWN_RIGHT
        set(value) {
            if(field==value)return
            val geometry=lastGeometry
            val previous=geometry?.let {bounds(it[0],it[1],it[2],it[3])}
            field=value
            if(geometry!=null && previous!=null)move(previous.x.toDouble(),previous.y.toDouble(),geometry[0],geometry[1],geometry[2],geometry[3])
        }
    private var referenceWidth = defaultReferenceWidth
    private var referenceHeight = defaultReferenceHeight
    fun effectiveScale(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): Float {
        if (referenceWidth <= 0 || referenceHeight <= 0) return scale
        val adapted = scale * minOf(screenWidth / referenceWidth, screenHeight / referenceHeight)
        return minOf(adapted, screenWidth.coerceAtLeast(1).toFloat() / panelWidth.coerceAtLeast(1),
            screenHeight.coerceAtLeast(1).toFloat() / panelHeight.coerceAtLeast(1))
    }
    fun bounds(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): HudBounds {
        if(basePanelWidth<=0)basePanelWidth=panelWidth.coerceAtLeast(1)
        if(basePanelHeight<=0)basePanelHeight=panelHeight.coerceAtLeast(1)
        lastGeometry=intArrayOf(screenWidth,screenHeight,panelWidth,panelHeight)
        val fittedScale = effectiveScale(screenWidth, screenHeight, panelWidth, panelHeight)
        val w = ceil(panelWidth * fittedScale).toInt()
        val h = ceil(panelHeight * fittedScale).toInt()
        return HudBounds((x * screenWidth-(panelWidth-basePanelWidth)*fittedScale*expansion.horizontal).roundToInt().coerceIn(0, (screenWidth - w).coerceAtLeast(0)),
            (y * screenHeight-(panelHeight-basePanelHeight)*fittedScale*expansion.vertical).roundToInt().coerceIn(0, (screenHeight - h).coerceAtLeast(0)), w, h)
    }
    fun move(px: Double, py: Double, screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int) {
        hasSavedPosition = true
        val bounds = bounds(screenWidth, screenHeight, panelWidth, panelHeight)
        val fitted=effectiveScale(screenWidth,screenHeight,panelWidth,panelHeight)
        x = (px.coerceIn(0.0, (screenWidth - bounds.width).coerceAtLeast(0).toDouble()).toFloat()+(panelWidth-basePanelWidth)*fitted*expansion.horizontal) / screenWidth.coerceAtLeast(1)
        y = (py.coerceIn(0.0, (screenHeight - bounds.height).coerceAtLeast(0).toDouble()).toFloat()+(panelHeight-basePanelHeight)*fitted*expansion.vertical) / screenHeight.coerceAtLeast(1)
    }
    fun load(properties: Properties, id: String) {
        fun read(key: String, default: Float) = properties.getProperty("hud.$id.$key")?.toFloatOrNull()?.takeIf(Float::isFinite) ?: default
        lastGeometry=null
        expansion=runCatching {HudExpansion.valueOf(properties.getProperty("hud.$id.expansion",if(id=="banner") "DOWN" else "DOWN_RIGHT"))}.getOrDefault(HudExpansion.DOWN_RIGHT)
        basePanelWidth=properties.getProperty("hud.$id.basePanelWidth")?.toIntOrNull()?.coerceIn(0,4096) ?: 0
        basePanelHeight=properties.getProperty("hud.$id.basePanelHeight")?.toIntOrNull()?.coerceIn(0,4096) ?: 0
        hasSavedPosition = properties.containsKey("hud.$id.x")
        x = read("x", defaultX).coerceIn(-2f, 2f); y = read("y", defaultY).coerceIn(-2f, 2f); scale = read("scale", defaultScale)
        referenceWidth = read("referenceWidth", defaultReferenceWidth).coerceAtLeast(0f)
        referenceHeight = read("referenceHeight", defaultReferenceHeight).coerceAtLeast(0f)
    }
    fun markPositioned() { hasSavedPosition=true }
    fun save(properties: Properties, id: String) {
        if(id in setOf("shard_guidance","hunting_box_value") && !hasSavedPosition)return
        properties.setProperty("hud.$id.x", x.toString()); properties.setProperty("hud.$id.y", y.toString())
        properties.setProperty("hud.$id.scale", scale.toString())
        properties.setProperty("hud.$id.expansion",expansion.name)
        if(basePanelWidth>0 && basePanelHeight>0) {
            properties.setProperty("hud.$id.basePanelWidth",basePanelWidth.toString())
            properties.setProperty("hud.$id.basePanelHeight",basePanelHeight.toString())
        }
        if (referenceWidth > 0 && referenceHeight > 0) {
            properties.setProperty("hud.$id.referenceWidth", referenceWidth.toString())
            properties.setProperty("hud.$id.referenceHeight", referenceHeight.toString())
        }
    }
    fun reset() { lastGeometry=null; basePanelWidth=0;basePanelHeight=0;expansion=HudExpansion.DOWN_RIGHT;hasSavedPosition=false; x = defaultX; y = defaultY; scale = defaultScale }
}

enum class SafariHud(private val defaultLabel: String, val layout: HudLayout) {
    PROGRESS("Safari progress", HudLayout(.015f, .22f)), MISSING("Missing critters", HudLayout(.72f, .22f)),
    PROFITABLE("Profitable mobs", HudLayout(.72f, .30f, .6f,640f,360f)),
    LAKE_CLEARED("Lake Cleared", HudLayout(.40f, .14f, 1.5f,640f,360f)),
    OBJECTIVES("Biome Objectives", HudLayout(.72f, .62f)),
    CAPTURES("Biome captures", HudLayout(.39f, .22f)), SPARKLINGS("Nearby sparklings", HudLayout(.015f, .72f)),
    BANNER("Safari Banner", HudLayout(.40f, .34f, 1.4f)),
    GEMS("All Gems alert", HudLayout(.36f, .10f, 1.5f)),
    BIRD_FOOD("All Bird Food alert", HudLayout(.30f, .18f, 1.5f)),
    COINS("All Coins alert", HudLayout(.34f, .30f, 1.5f)),
    INCENSE("All Incense alert", HudLayout(.34f, .26f, 1.5f)),
    RUN_COMPLETE("Run completion alert", HudLayout(.35f, .42f, 1.5f)),
    WARP("Warping Now", HudLayout(.37f, .34f, 1.5f)),
    DOOM_THROW("Doomspiral throw timer", HudLayout(.40f, .56f, 1.5f)),
    HOTSPOT("Hotspot GUI", HudLayout(.8109375f, .71944445f, .6f,640f,360f)),
    ENTRY_COOLDOWN("Safari entry cooldown", HudLayout(.40f, .65f)),
    SHARD_GUIDANCE("Shard sale guidance", HudLayout(.75f,.2f)),
    HUNTING_BOX_VALUE("Hunting Box value", HudLayout(.35f,.1f)),
    DEVELOPER("Safari developer", HudLayout(.34f, .24f));
    val label get() = if (this == MISSING && net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode)
        "Unchecked critters" else defaultLabel
    companion object {
        val available get() = entries.filter { when(it) { SHARD_GUIDANCE,HUNTING_BOX_VALUE -> net.johnceo.sparklingmutuals.skyblock.market.HuntingBoxHuds.available(it); DEVELOPER -> net.johnceo.sparklingmutuals.debug.SafariDeveloper.enabled; else -> true } }
        fun load(properties: Properties) = entries.forEach { it.layout.load(properties, it.name.lowercase()) }
        fun save(properties: Properties) = entries.forEach { it.layout.save(properties, it.name.lowercase()) }
        fun resetDefaults() = load(net.johnceo.sparklingmutuals.config.FreshInstallDefaults.main())
    }
}
