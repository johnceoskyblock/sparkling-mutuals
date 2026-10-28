package net.johnceo.sparklingmutuals.history.ui

import kotlin.math.roundToInt

/** Shared geometry for drawing and grabbing the history scrollbar in GUI coordinates. */
data class HistoryScrollTrack(val top: Int, val bottom: Int, val rows: Int, val total: Int) {
    val maxOffset get() = maxOf(0, total - rows)
    private val height get() = maxOf(0, bottom - top)
    val thumbHeight get() = minOf(height, maxOf(10, height * rows / total.coerceAtLeast(1)))
    private val travel get() = height - thumbHeight
    val scrollable get() = maxOffset > 0 && travel > 0
    fun thumbTop(offset: Int) = top + if (scrollable) travel * offset.coerceIn(0, maxOffset) / maxOffset else 0
    fun offsetAt(mouseY: Double, grabOffset: Double): Int = if (!scrollable) 0 else
        (((mouseY - top - grabOffset) / travel).coerceIn(0.0, 1.0) * maxOffset).roundToInt()
}
