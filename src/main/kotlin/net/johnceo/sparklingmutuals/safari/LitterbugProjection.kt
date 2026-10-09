package net.johnceo.sparklingmutuals.safari

import java.util.UUID
import kotlin.math.abs

/** Display-only emergence heights; entity positions and capture evidence remain untouched. */
class LitterbugProjection {
    private val heights = mutableMapOf<UUID, Double>()
    fun observe(id: UUID, y: Double) { if (y.isFinite() && y > 2) heights[id] = y }
    fun hiddenHeight(id: UUID, x: Double, y: Double, z: Double): Double? {
        if (!y.isFinite() || abs(y - 1) > .01) return null
        heights[id]?.let { return it }
        if (x in 13.0..19.99 && z in -66.99..-66.0) return 73.0
        val rooms = listOfNotNull(
            69.0.takeIf { x >= 7 && x < 16 && z >= -81 && z < -66 },
            85.0.takeIf { x >= -11 && x < 9 && z >= -67 && z < -60 })
        return rooms.singleOrNull()
    }
    fun reset() = heights.clear()
}
