package net.johnceo.sparklingmutuals.safari

import kotlin.math.abs
import kotlin.math.floor

/** CritterMod 0.9.0 MoundSpotter, MoundTracker and WallTracker rules (MIT). */
data class MoundShape(val x: Double, val y: Double, val z: Double, val width: Double, val height: Double,
    val distanceSquared: Double, val occupied: Boolean = false)
object SafariMounds {
    fun detected(shape: MoundShape) = shape.distanceSquared <= 64.0 * 64 && shape.y <= 65 &&
        shape.width in .35..1.10 && shape.height in .25.. .95 && shape.height <= shape.width + .1 &&
        abs(shape.x - (floor(shape.x) + .5)) < .05 && abs(shape.z - (floor(shape.z) + .5)) < .05 && !shape.occupied
    fun outcome(raw: String): Boolean? {
        val text = SafariRules.strip(raw)
        return when {
            text.startsWith("The mound falls apart, but nothing is inside") -> false
            text.startsWith("The mound fell apart, revealing a Rockmite hidden inside") -> true
            else -> null
        }
    }
}
enum class WallState { INTACT, BROKEN, UNKNOWN }
data class WallSummary(val states: List<WallState>) {
    val intact get() = states.count { it == WallState.INTACT }
    val unknown get() = states.count { it == WallState.UNKNOWN }
    val allBroken get() = states.isNotEmpty() && states.all { it == WallState.BROKEN }
    val remaining get() = "$intact" + if (unknown > 0) " (+$unknown?)" else ""
}

/** Retain unopened sites until they can be checked again, even across biome changes. */
class MoundSurvey {
    private val sites = mutableMapOf<Triple<Int, Int, Int>, Boolean>()
    val allBroken get() = sites.size >= SafariFullClear.MOUND_MINIMUM && sites.values.all { it }
    val remaining get() = sites.values.count { !it }
    fun scan(present: Set<Triple<Int, Int, Int>>, observable: (Triple<Int, Int, Int>) -> Boolean) {
        present.forEach { sites[it] = false }
        sites.keys.filter { it !in present && observable(it) }.forEach { sites[it] = true }
    }
}
