package net.johnceo.sparklingmutuals.safari

import java.util.UUID
import kotlin.math.abs

data class ShywormPath(val minX: Double, val minZ: Double, val maxX: Double, val maxZ: Double, val height: Double)
data class ShywormTimer(val text: String, val color: Int)
class ShywormProjection {
    private data class Worm(var x: Double, var y: Double, var z: Double, var height: Double?,
        var visible: Boolean = false, var visibleAt: Long? = null, var hiddenAt: Long? = null,
        var dx: Int = 0, var dz: Int = 0, var cornerX: Double = x, var cornerZ: Double = z,
        var movedAt: Long = 0, var restAt: Long? = null)
    private val worms = mutableMapOf<UUID, Worm>()
    private val rests = mutableListOf<Long>()
    private val restMessages = setOf("The Shyworm hid back into the ground.",
        "The Shyworm saw a player and fled!", "The Shyworm fled because it was startled!")
    fun observe(id: UUID, x: Double, y: Double, z: Double, visible: Boolean, now: Long) {
        if (!listOf(x, y, z).all(Double::isFinite)) return
        val w = worms.getOrPut(id) { Worm(x, y, z, null) }
        val surfaced = visible && y > 2 && (w.height == null || y >= w.height!! - .5)
        if (surfaced) {
            w.height = y; w.visibleAt = now; w.hiddenAt = null
            if (w.restAt != null) { w.dx = 0; w.dz = 0; w.cornerX = x; w.cornerZ = z }
            w.restAt = null
        } else if (w.visible) w.hiddenAt = now
        val mx = x - w.x; val mz = z - w.z
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
            }
        }
        w.x = x; w.y = y; w.z = z; w.visible = surfaced
    }
    fun chat(text: String, now: Long) { if (text in restMessages) rests.add(now) }
    /** Server messages have no UUID; pair only with a single freshly hidden loaded model. */
    fun reconcile(now: Long) {
        rests.removeAll { now - it !in 0..2000 }
        for (at in rests.toList()) {
            val candidates = worms.values.filter {
                !it.visible && it.restAt == null && it.height != null &&
                    it.visibleAt?.let { seen -> at - seen in -1500..2000 } == true &&
                    it.hiddenAt?.let { hidden -> hidden - at in -1500..2000 } == true
            }
            candidates.singleOrNull()?.let { it.restAt = at; rests.remove(at) }
        }
    }
    fun hiddenHeight(id: UUID): Double? = worms[id]?.takeIf { !it.visible }?.height
    fun timer(id: UUID, now: Long): ShywormTimer? {
        val at = worms[id]?.takeIf { !it.visible }?.restAt ?: return null
        val elapsed = (now - at).coerceAtLeast(0)
        fun seconds(ms: Long): String { val tenths = (ms.coerceAtLeast(0) + 99) / 100; return "${tenths / 10}.${tenths % 10}" }
        val text = when {
            elapsed >= 15000 -> "Moving soon.."
            elapsed >= 8000 -> "Any moment (<=${seconds(15000 - elapsed)}s)"
            else -> "~${seconds(8000 - elapsed)}-${seconds(15000 - elapsed)}s"
        }
        return ShywormTimer(text, when {
            elapsed >= 15000 -> -1
            elapsed < 5000 -> 0xFF55FF55.toInt()
            elapsed < 8000 -> 0xFFFFFF55.toInt()
            else -> 0xFFFF5555.toInt()
        })
    }
    fun path(id: UUID, now: Long): ShywormPath? {
        val w = worms[id] ?: return null
        val height = w.height ?: return null
        if (w.restAt != null || w.dx == 0 && w.dz == 0 || now - w.movedAt !in 0..3000) return null
        val endX = w.cornerX + w.dx * 7; val endZ = w.cornerZ + w.dz * 7
        return ShywormPath(minOf(w.cornerX, endX) - if (w.dx == 0) .5 else 0.0,
            minOf(w.cornerZ, endZ) - if (w.dz == 0) .5 else 0.0,
            maxOf(w.cornerX, endX) + if (w.dx == 0) .5 else 0.0,
            maxOf(w.cornerZ, endZ) + if (w.dz == 0) .5 else 0.0, height + 1.35)
    }
    fun retain(ids: Set<UUID>) { worms.keys.retainAll(ids) }
    fun reset() { worms.clear(); rests.clear() }
}
