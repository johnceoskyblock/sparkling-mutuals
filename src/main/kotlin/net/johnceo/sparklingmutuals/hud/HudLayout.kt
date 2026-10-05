package net.johnceo.sparklingmutuals.hud

import java.util.Properties
import kotlin.math.ceil

data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
}

/** Normalized positions keep panels on screen when Minecraft's GUI resolution changes. */
class HudLayout(private val defaultX: Float, private val defaultY: Float) {
    var x = defaultX
    var y = defaultY
    var scale = 1f
        set(value) { field = if (value.isFinite()) value.coerceIn(.15f, 2f) else 1f }
    fun bounds(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): HudBounds {
        val w = ceil(panelWidth * scale).toInt()
        val h = ceil(panelHeight * scale).toInt()
        return HudBounds((x * screenWidth).toInt().coerceIn(0, (screenWidth - w).coerceAtLeast(0)),
            (y * screenHeight).toInt().coerceIn(0, (screenHeight - h).coerceAtLeast(0)), w, h)
    }
    fun move(px: Double, py: Double, screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int) {
        val bounds = bounds(screenWidth, screenHeight, panelWidth, panelHeight)
        x = px.coerceIn(0.0, (screenWidth - bounds.width).coerceAtLeast(0).toDouble()).toFloat() / screenWidth.coerceAtLeast(1)
        y = py.coerceIn(0.0, (screenHeight - bounds.height).coerceAtLeast(0).toDouble()).toFloat() / screenHeight.coerceAtLeast(1)
    }
    fun load(properties: Properties, id: String) {
        fun read(key: String, default: Float) = properties.getProperty("hud.$id.$key")?.toFloatOrNull()?.takeIf(Float::isFinite) ?: default
        x = read("x", defaultX).coerceIn(0f, 1f); y = read("y", defaultY).coerceIn(0f, 1f); scale = read("scale", 1f)
    }
    fun save(properties: Properties, id: String) {
        properties.setProperty("hud.$id.x", x.toString()); properties.setProperty("hud.$id.y", y.toString())
        properties.setProperty("hud.$id.scale", scale.toString())
    }
    fun reset() { x = defaultX; y = defaultY; scale = 1f }
}

enum class SafariHud(val label: String, val layout: HudLayout) {
    PROGRESS("Safari progress", HudLayout(.015f, .22f)), MISSING("Missing critters", HudLayout(.72f, .22f)),
    CAPTURES("Biome captures", HudLayout(.39f, .22f)), SPARKLINGS("Nearby sparklings", HudLayout(.015f, .72f));
    companion object {
        fun load(properties: Properties) = entries.forEach { it.layout.load(properties, it.name.lowercase()) }
        fun save(properties: Properties) = entries.forEach { it.layout.save(properties, it.name.lowercase()) }
    }
}
