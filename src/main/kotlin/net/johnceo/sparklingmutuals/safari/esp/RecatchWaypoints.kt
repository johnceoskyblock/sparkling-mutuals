// Adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT. See licenses/SafariUtils-MIT.txt.
package net.johnceo.sparklingmutuals.safari.esp

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.johnceo.sparklingmutuals.compat.WorldDepthMode
import net.johnceo.sparklingmutuals.compat.WorldRenderCompat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

internal object RecatchWaypoints {
    private val lines = WorldRenderCompat.renderType("safari_recatch", depth = WorldDepthMode.OCCLUDED)

    fun render(context: LevelRenderContext, captures: EspCaptureMemory, visible: (Vec3) -> Boolean) {
        val pins = captures.recatchPins(System.currentTimeMillis())
        if (!ConfigManager.recatchWaypoint) return
        for (pin in pins) {
            val center = pin.position.add(0.0, .35, 0.0)
            if (!visible(center)) continue
            val color = SafariEspConfig.rgb(SafariEspConfig.entityColor(pin.species))
            WorldRenderCompat.box(context, lines, AABB.ofSize(center, .55, .55, .55), color)
            WorldRenderCompat.markerLabel(context, "Recatch · ${pin.species}", color, center.x, center.y + .5, center.z, false)
        }
        WorldRenderCompat.flush(context, lines)
    }
}
