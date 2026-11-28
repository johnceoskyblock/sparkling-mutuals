package net.johnceo.sparklingmutuals.safari

import kotlin.math.ceil
import kotlin.math.exp

/** Pure presentation state. The lottery owns evidence, cancellation and the server clock. */
internal object DoomspiralCabinet {
    const val REVEAL_TICKS = 25
    private const val REEL_STOP_TICK = DOOMSPIRAL_CAPTURE_TICKS - REVEAL_TICKS

    fun scale(width: Int, height: Int): Float =
        if (width <= 0 || height <= 0) 0f else minOf(1.6f, width / 352f, height / 300f)

    fun frame(elapsed: Float, settleElapsed: Float?, remaining: Int?, ticks: Float, result: Int?): Frame =
        Frame(elapsed, settleElapsed, ticks, result, result != null && remaining != null && remaining <= REVEAL_TICKS)

    class Frame(
        val elapsed: Float,
        private val settleElapsed: Float?,
        private val ticks: Float,
        private val result: Int?,
        val revealed: Boolean
    ) {
        val jackpot get() = revealed && result == DOOMSPIRAL_JACKPOT_MASK
        val revealProgress get() = ((ticks - REEL_STOP_TICK) / REVEAL_TICKS).coerceIn(0f, 1f)

        fun offset(index: Int): Float {
            val settle = settleElapsed ?: return spinOffset(elapsed, index)
            val mask = result ?: return spinOffset(elapsed, index)
            val from = spinOffset(settle, index)
            val to = ceil((from + 4) / 6) * 6 + if (mask and (1 shl index) != 0) 0 else 1
            val spin = (ticks / REEL_STOP_TICK).coerceIn(0f, 1f)
            return if (spin == 1f) to else from + (to - from) * (1f - (1f - spin) * (1f - spin))
        }

        private fun spinOffset(seconds: Float, index: Int) =
            seconds * 7f + 14f * (1f - exp(-seconds / 1.8f)) + index * 2
    }
}
