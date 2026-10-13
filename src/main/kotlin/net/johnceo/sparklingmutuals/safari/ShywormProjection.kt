package net.johnceo.sparklingmutuals.safari

import java.util.UUID
import kotlin.math.abs

data class ShywormPath(val minX: Double, val minZ: Double, val maxX: Double, val maxZ: Double, val height: Double)
data class ShywormTimer(val text: String, val color: Int)
class ShywormProjection {
    private data class Worm(var x: Double, var y: Double, var z: Double, var height: Double?,
        var visible: Boolean = false, var visibleAt: Long? = null, var hiddenAt: Long? = null,
        var dx: Int = 0, var dz: Int = 0, var cornerX: Double = x, var cornerZ: Double = z,
        var movedAt: Long = Long.MIN_VALUE, var restAt: Long? = null, var groundHeight: Double? = null,
        val startX: Double = x, val startZ: Double = z, var firstDx: Int = 0, var firstDz: Int = 0,
        var returnedToStart: Boolean = false)
    private val worms = mutableMapOf<UUID, Worm>()
    private data class Rest(val at: Long, val playerX: Double, val playerZ: Double)
    private val rests = mutableListOf<Rest>()
    private val restMessages = setOf("The Shyworm hid back into the ground.",
        "The Shyworm saw a player and fled!", "The Shyworm fled because it was startled!")
    fun observe(id: UUID, x: Double, y: Double, z: Double, visible: Boolean, now: Long) {
        if (!listOf(x, y, z).all(Double::isFinite)) return
        val w = worms.getOrPut(id) { Worm(x, y, z, null) }
        val mx = x - w.x; val mz = z - w.z
        val changed = maxOf(abs(mx), abs(mz)) > .02
        val atStart = abs(x - w.startX) <= .15 && abs(z - w.startZ) <= .15
        val returning = changed && atStart && (w.firstDx != 0 || w.firstDz != 0)
        if (changed && !atStart) w.returnedToStart = false
        // Labels can linger during hiding. Only actual departure ends a confirmed rest.
        if (w.restAt != null && y > 2 && (changed && !atStart || visible && abs(w.y - 1.0) <= .01)) {
            w.restAt = null; w.dx = 0; w.dz = 0; w.cornerX = w.startX; w.cornerZ = w.startZ
        }
        val surfaced = visible && y > 2
        if (surfaced && w.restAt == null && !returning && !w.returnedToStart) {
            w.height = y
            if (w.groundHeight == null) w.groundHeight = listOf(40.0, 43.0, 60.0).minBy { abs(it - (y + 1.35)) }
            w.visibleAt = now; w.hiddenAt = null
        } else if (w.visible) w.hiddenAt = now
        if (returning) {
            // Returning to the origin is a reset, not evidence for the next movement side.
            w.dx = 0; w.dz = 0; w.cornerX = w.startX; w.cornerZ = w.startZ
            w.returnedToStart = true
            w.x = x; w.y = y; w.z = z; w.visible = surfaced
            return
        }
        if (maxOf(abs(mx), abs(mz)) > 8) { w.dx = 0; w.dz = 0 }
        if (maxOf(abs(mx), abs(mz)) > .02 && maxOf(abs(mx), abs(mz)) <= 8) {
            val direction = when {
                abs(mx) > abs(mz) * 3 -> (if (mx > 0) 1 else -1) to 0
                abs(mz) > abs(mx) * 3 -> 0 to (if (mz > 0) 1 else -1)
                else -> null
            }
            if (direction != null) {
                val (dx, dz) = direction
                if (w.dx == 0 && w.dz == 0 || w.dx * dz - w.dz * dx == 1) {
                    w.cornerX = w.x; w.cornerZ = w.z
                } else if (dx != w.dx || dz != w.dz) {
                    // A reset/teleport is not evidence for another side of the square.
                    w.dx = 0; w.dz = 0; w.x = x; w.y = y; w.z = z; w.visible = surfaced
                    return
                }
                w.dx = dx; w.dz = dz; w.movedAt = now
                if (w.firstDx == 0 && w.firstDz == 0) { w.firstDx = dx; w.firstDz = dz }
            }
        }
        w.x = x; w.y = y; w.z = z; w.visible = surfaced
    }
    fun chat(text: String, now: Long, playerX: Double = 0.0, playerZ: Double = 0.0) { if (text in restMessages && playerX.isFinite() && playerZ.isFinite()) rests.add(Rest(now, playerX, playerZ)) }
    /** Queue server messages until a loaded underground model stops moving, then use player proximity. */
    fun reconcile(now: Long) {
        rests.removeAll { now - it.at !in 0..2000 }
        for (rest in rests.toList()) {
            val candidates = worms.values.filter {
                (abs(it.y - 1.0) <= .01 || (!it.visible || it.returnedToStart) && abs(it.x - it.startX) <= .15 && abs(it.z - it.startZ) <= .15) &&
                    it.restAt == null && it.height != null &&
                    (it.movedAt == Long.MIN_VALUE || now - it.movedAt >= 250)
            }.sortedBy { (it.x - rest.playerX) * (it.x - rest.playerX) + (it.z - rest.playerZ) * (it.z - rest.playerZ) }
            val closest = candidates.firstOrNull() ?: continue
            fun distance(w: Worm) = (w.x - rest.playerX) * (w.x - rest.playerX) + (w.z - rest.playerZ) * (w.z - rest.playerZ)
            // An exact tie does not identify which worm the message belongs to.
            if (candidates.size > 1 && abs(distance(candidates[1]) - distance(closest)) < .0001) continue
            closest.restAt = rest.at; rests.remove(rest)
        }
    }
    fun hiddenHeight(id: UUID): Double? = worms[id]?.takeIf { it.restAt != null || abs(it.y - 1.0) <= .01 }?.height
    fun timer(id: UUID, now: Long): ShywormTimer? {
        val at = worms[id]?.restAt ?: return null
        val remaining = (8000 - (now - at).coerceAtLeast(0)).coerceAtLeast(0)
        val tenths = (remaining + 99) / 100
        return ShywormTimer(if (remaining == 0L) "moving soon..." else "${tenths / 10}.${tenths % 10}s", when {
            remaining == 0L -> -1
            remaining > 5000 -> 0xFF55FF55.toInt()
            remaining > 2000 -> 0xFFFFFF55.toInt()
            else -> 0xFFFF5555.toInt()
        })
    }

    fun path(id: UUID, now: Long): ShywormPath? {
        val w = worms[id] ?: return null
        val height = w.groundHeight ?: return null
        val resting = w.restAt != null
        if (resting && w.firstDx == 0 && w.firstDz == 0 || !resting && (w.dx == 0 && w.dz == 0 || now - w.movedAt !in 0..3000)) return null
        val startX = if (resting) w.startX else w.cornerX + w.dx * 7
        val startZ = if (resting) w.startZ else w.cornerZ + w.dz * 7
        val dx = if (resting) w.firstDx else -w.dz
        val dz = if (resting) w.firstDz else w.dx
        val endX = startX + dx * 7; val endZ = startZ + dz * 7
        return ShywormPath(minOf(startX, endX) - if (dx == 0) .5 else 0.0,
            minOf(startZ, endZ) - if (dz == 0) .5 else 0.0,
            maxOf(startX, endX) + if (dx == 0) .5 else 0.0,
            maxOf(startZ, endZ) + if (dz == 0) .5 else 0.0, height)
    }

    fun retain(ids: Set<UUID>) { worms.keys.retainAll(ids) }
    fun reset() { worms.clear(); rests.clear() }
}
