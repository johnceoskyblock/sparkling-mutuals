package net.johnceo.sparklingmutuals.safari

import kotlin.math.ceil
import kotlin.math.exp

/** Pure presentation state. The lottery owns evidence, cancellation and the server clock. */
internal object DoomspiralCabinet {
    fun scale(width: Int, height: Int): Float =
        if (width <= 0 || height <= 0) 0f else minOf(1.6f, width / 352f, height / 300f)

    fun frame(elapsed: Float, settleElapsed: Float?, remaining: Int?, ticks: Float, result: Int): Frame =
        Frame(elapsed, settleElapsed, ticks, result, remaining != null && remaining <= 5)

    class Frame(
        val elapsed: Float,
        private val settleElapsed: Float?,
        val ticks: Float,
        private val result: Int,
        private val revealed: Boolean
    ) {
        val jackpot get() = revealed && result == 7
        val label get() = if (!revealed) null else if (jackpot) "Sparkling Doomspiral" else "Regular Doomspiral"
        val labelColor get() = if (jackpot) 0xFFFFD87A.toInt() else 0xFFFFFFFF.toInt()
        val revealAge get() = (ticks - 93f).coerceAtLeast(0f)

        fun offset(index: Int): Float {
            val settle = settleElapsed ?: return spinOffset(elapsed, index)
            val from = spinOffset(settle, index)
            val to = ceil((from + 4) / 6) * 6 + if (result and (1 shl index) != 0) 0 else 1
            val spin = (ticks / 93f).coerceIn(0f, 1f)
            return if (spin == 1f) to else from + (to - from) * (1f - (1f - spin) * (1f - spin))
        }

        private fun spinOffset(seconds: Float, index: Int) =
            seconds * 7f + 14f * (1f - exp(-seconds / 1.8f)) + index * 2
    }
}
