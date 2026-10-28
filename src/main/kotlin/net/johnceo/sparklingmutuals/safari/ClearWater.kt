package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.Minecraft

/** Render workers read a snapshot; they never inspect live client state while building fluid meshes. */
object ClearWater {
    @Volatile var active = false
        private set
    @Volatile private var opacity = 60
    fun enabled(setting: Boolean, inSafari: Boolean) = setting && inSafari
    fun surfaceColor(color: Int, water: Boolean, enabled: Boolean = active, opacityPercent: Int = opacity): Int =
        if (!water || !enabled) color else (minOf(color ushr 24, (opacityPercent.coerceIn(0, 100) * 255 + 50) / 100) shl 24) or (color and 0xFFFFFF)
    fun tick(client: Minecraft) {
        val next = enabled(ConfigManager.clearWater, SafariAssist.inSafari) && client.level != null
        val nextOpacity = ConfigManager.waterOpacity
        val changed = next != active || next && nextOpacity != opacity
        opacity = nextOpacity
        active = next
        // Transparency is stored in chunk meshes. Rebuild once when its effective setting changes.
        if (changed && client.level != null) {
            //? if >=26.2 {
            /*client.levelRenderer.invalidateCompiledGeometry(client.level!!, client.options, client.gameRenderer.smCamera, client.blockColors)
            *///? } else {
            client.levelRenderer.allChanged()
            //? }
        }
    }
    fun reset() { active = false; opacity = 60 }
}
