/*
 * Adapted from SkyHanni PhantomleafSolver.kt, commit b3543887dfb80d95ff9880b3f610167ac2c104f2.
 * Copyright SkyHanni contributors. Licensed under LGPL-2.1; see licenses/SkyHanni-LGPL-2.1.txt.
 * Changes: standalone state, Fabric lifecycle/sound hook, native settings and world rendering.
 */
package net.johnceo.sparklingmutuals.misc.phantomleaf

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

/** SkyHanni's horizontal sound-volume search, kept separate from client integration. */
class PhantomleafState {
    data class Candidate(val x: Int, val y: Int = 74, val z: Int)
    enum class Hint { NONE_FOUND, MULTIPLE }
    var searching = false
        private set
    private var lastX: Double? = null
    private var lastZ: Double? = null
    private val found = mutableListOf<Candidate>()
    val candidates: List<Candidate> get() = found.toList()

    fun chat(message: String) {
        when {
            message.contains("Phantomleaf: Poof! Try and find me!") -> searching = true
            message.contains("Phantomleaf: You found me!") ||
                message.contains("Phantomleaf: That's not me! Better luck next time!") -> reset()
        }
    }
    fun sound(name: String, pitch: Float, volume: Float, x: Double, z: Double): Hint? {
        if (!searching || name != "minecraft:block.note_block.basedrum" ||
            pitch.toDouble() !in 0.61..0.62 || !volume.isFinite() || volume !in 0f..1f ||
            !x.isFinite() || !z.isFinite()) return null
        var hint: Hint? = null
        // Like upstream, solve only after two qualifying sounds at the same horizontal position.
        if (lastX == x && lastZ == z) {
            val radius = 30 * (1.0 - volume)
            val centerX = floor(x) + .5
            val centerZ = floor(z) + .5
            found.clear()
            for (dx in -31..31) for (dz in -31..31) {
                val distance = hypot(centerX + dx - x, centerZ + dz - z)
                if (abs(distance - radius) < .001)
                    found.add(Candidate(floor(centerX + dx).toInt(), z = floor(centerZ + dz).toInt()))
            }
            hint = when (found.size) {
                0 -> Hint.NONE_FOUND
                1 -> null
                else -> Hint.MULTIPLE
            }
        }
        lastX = x; lastZ = z
        return hint
    }
    fun reset() { searching = false; found.clear(); lastX = null; lastZ = null }
}
