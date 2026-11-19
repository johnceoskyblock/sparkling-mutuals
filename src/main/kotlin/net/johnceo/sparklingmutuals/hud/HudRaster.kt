package net.johnceo.sparklingmutuals.hud

import kotlin.math.floor
import kotlin.math.round

/** Keep bitmap font pixels equally wide on the framebuffer, without changing saved sizes. */
object HudRaster {
    fun scale(requested: Float, pixelRatio: Float, limit: Float): Float {
        if (!pixelRatio.isFinite() || pixelRatio <= 0f) return requested
        val maximum = floor(limit * pixelRatio)
        // Tiny viewports must still fit even when less than one screen pixel per font pixel fits.
        if (maximum < 1f) return requested
        return round(requested * pixelRatio).coerceIn(1f, maximum) / pixelRatio
    }
}
