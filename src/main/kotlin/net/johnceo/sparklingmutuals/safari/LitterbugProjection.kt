package net.johnceo.sparklingmutuals.safari

import java.util.UUID
import kotlin.math.abs

/** Display-only emergence heights; entity positions and capture evidence remain untouched. */
data class LitterbugMarker(val height: Double, val text: String, val color: Int)

class LitterbugProjection {
    private val heights = mutableMapOf<UUID, Double>()
    private val hiddenSince = mutableMapOf<UUID, Long>()
    private fun hidden(y: Double) = y.isFinite() && abs(y - 1) <= .01
    fun observe(id: UUID, y: Double, now: Long = System.currentTimeMillis()) {
        if (!y.isFinite()) return
        if (y > 2) heights[id] = y
        if (hidden(y)) hiddenSince.putIfAbsent(id, now) else hiddenSince.remove(id)
    }
    fun hiddenHeight(id: UUID, x: Double, y: Double, z: Double): Double? {
        if (!hidden(y)) return null
        heights[id]?.let { return it }
        if (x in 13.0..19.99 && z in -66.99..-66.0) return 73.0
        val rooms = listOfNotNull(
            69.0.takeIf { x >= 7 && x < 16 && z >= -81 && z < -66 },
            85.0.takeIf { x >= -11 && x < 9 && z >= -67 && z < -60 })
        return rooms.singleOrNull()
    }
    fun hiddenMarker(id: UUID, x: Double, y: Double, z: Double, now: Long): LitterbugMarker? {
        val height = hiddenHeight(id, x, y, z) ?: return null
        val since = hiddenSince[id] ?: return null
        val remaining = (8000 - (now - since).coerceAtLeast(0)).coerceAtLeast(0)
        val tenths = (remaining + 99) / 100
        return LitterbugMarker(height, if (remaining == 0L) "Moving soon.." else "${tenths / 10}.${tenths % 10}s",
            when { remaining == 0L -> -1; remaining > 5000 -> 0xFF55FF55.toInt()
                remaining > 2000 -> 0xFFFFFF55.toInt(); else -> 0xFFFF5555.toInt() })
    }
    fun reset() { heights.clear(); hiddenSince.clear() }
}
