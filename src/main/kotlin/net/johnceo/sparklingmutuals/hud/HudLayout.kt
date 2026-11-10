package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import kotlin.math.ceil
import kotlin.math.roundToInt

data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
}

/** Normalized positions keep panels on screen when Minecraft's GUI resolution changes. */
class HudLayout(private val defaultX: Float, private val defaultY: Float, private val defaultScale: Float = 1f,
    private val defaultReferenceWidth: Float=0f, private val defaultReferenceHeight: Float=0f) {
    var x = defaultX
    var y = defaultY
    var scale = defaultScale
        set(value) { field = if (value.isFinite()) value.coerceIn(.15f, 2f) else defaultScale }
    private var referenceWidth = defaultReferenceWidth
    private var referenceHeight = defaultReferenceHeight
    fun effectiveScale(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): Float {
        if (referenceWidth <= 0 || referenceHeight <= 0) return scale
        val adapted = scale * minOf(screenWidth / referenceWidth, screenHeight / referenceHeight)
        return minOf(adapted, screenWidth.coerceAtLeast(1).toFloat() / panelWidth.coerceAtLeast(1),
            screenHeight.coerceAtLeast(1).toFloat() / panelHeight.coerceAtLeast(1))
    }
    fun bounds(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): HudBounds {
        val fittedScale = effectiveScale(screenWidth, screenHeight, panelWidth, panelHeight)
        val w = ceil(panelWidth * fittedScale).toInt()
        val h = ceil(panelHeight * fittedScale).toInt()
        return HudBounds((x * screenWidth).roundToInt().coerceIn(0, (screenWidth - w).coerceAtLeast(0)),
            (y * screenHeight).roundToInt().coerceIn(0, (screenHeight - h).coerceAtLeast(0)), w, h)
    }
    fun move(px: Double, py: Double, screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int) {
        val bounds = bounds(screenWidth, screenHeight, panelWidth, panelHeight)
        x = px.coerceIn(0.0, (screenWidth - bounds.width).coerceAtLeast(0).toDouble()).toFloat() / screenWidth.coerceAtLeast(1)
        y = py.coerceIn(0.0, (screenHeight - bounds.height).coerceAtLeast(0).toDouble()).toFloat() / screenHeight.coerceAtLeast(1)
    }
    fun load(properties: Properties, id: String) {
        fun read(key: String, default: Float) = properties.getProperty("hud.$id.$key")?.toFloatOrNull()?.takeIf(Float::isFinite) ?: default
        x = read("x", defaultX).coerceIn(0f, 1f); y = read("y", defaultY).coerceIn(0f, 1f); scale = read("scale", defaultScale)
        referenceWidth = read("referenceWidth", defaultReferenceWidth).coerceAtLeast(0f)
        referenceHeight = read("referenceHeight", defaultReferenceHeight).coerceAtLeast(0f)
    }
    fun save(properties: Properties, id: String) {
        properties.setProperty("hud.$id.x", x.toString()); properties.setProperty("hud.$id.y", y.toString())
        properties.setProperty("hud.$id.scale", scale.toString())
        if (referenceWidth > 0 && referenceHeight > 0) {
            properties.setProperty("hud.$id.referenceWidth", referenceWidth.toString())
            properties.setProperty("hud.$id.referenceHeight", referenceHeight.toString())
        }
    }
    fun reset() { x = defaultX; y = defaultY; scale = defaultScale }
}

enum class SafariHud(private val defaultLabel: String, val layout: HudLayout) {
    PROGRESS("Safari progress", HudLayout(.015f, .22f)), MISSING("Missing critters", HudLayout(.72f, .22f)),
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
    HOTSPOT("Hotspot GUI", HudLayout(.0140625f, .50f, .7f,640f,360f)),
    ENTRY_COOLDOWN("Safari entry cooldown", HudLayout(.40f, .65f)),
    DEVELOPER("Safari developer", HudLayout(.34f, .24f));
    val label get() = if (this == MISSING && net.johnceo.sparklingmutuals.config.ConfigManager.sparklingMode)
        "Unchecked critters" else defaultLabel
    companion object {
        val available get() = entries.filter { it != DEVELOPER || net.johnceo.sparklingmutuals.debug.SafariDeveloper.enabled }
        fun load(properties: Properties) = available.forEach { it.layout.load(properties, it.name.lowercase()) }
        fun save(properties: Properties) = available.forEach { it.layout.save(properties, it.name.lowercase()) }
        fun resetDefaults() = load(net.johnceo.sparklingmutuals.config.FreshInstallDefaults.main())
    }
}
