package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import kotlin.math.hypot

/** Fixed observed spatial curve. Its parameter is an update ordinal, not server time. */
object CapsulePathModel {
    const val MODEL_ID = "ordinary-stationary-visual-ordinal-candidate-v1"
    const val SPEED = 0.8482482249942948
    const val VERTICAL_ACCELERATION = -0.020085553311180723
    const val POINT_COUNT = 64
    val endParameter = 15.0 / SPEED

    private fun ShotVector.finite() = x.isFinite() && y.isFinite() && z.isFinite()
    private fun normalized(aim: ShotVector): ShotVector? {
        if (!aim.finite()) return null
        val length = hypot(hypot(aim.x, aim.y), aim.z)
        if (!length.isFinite() || length == 0.0) return null
        return ShotVector(aim.x / length, aim.y / length, aim.z / length)
    }

    fun point(eye: ShotVector, aim: ShotVector, parameter: Double): ShotVector? {
        if (!eye.finite() || !parameter.isFinite() || parameter !in 2.0..endParameter) return null
        return normalized(aim)?.let { calculate(eye, it, parameter) }
    }

    private fun calculate(eye: ShotVector, aim: ShotVector, n: Double): ShotVector? =
        ShotVector(eye.x + SPEED * n * aim.x,
            eye.y + SPEED * n * aim.y + .5 * VERTICAL_ACCELERATION * n * (n - 1),
            eye.z + SPEED * n * aim.z).takeIf { it.finite() }

    fun points(eye: ShotVector, aim: ShotVector): List<ShotVector> {
        if (!eye.finite()) return emptyList()
        val direction = normalized(aim) ?: return emptyList()
        val result = ArrayList<ShotVector>(POINT_COUNT)
        repeat(POINT_COUNT) { index ->
            val n = if (index == POINT_COUNT - 1) endParameter else 2.0 + (endParameter - 2.0) * index / (POINT_COUNT - 1)
            result.add(calculate(eye, direction, n) ?: return emptyList())
        }
        return result.toList()
    }
}
