package net.johnceo.sparklingmutuals.hud

import kotlin.math.abs
import kotlin.math.roundToInt

/** Screen-center/edge snapping adapted from SafariUtils HudEditorScreen (MIT). */
object HudSnapping {
    data class Position(val x: Int,val y: Int,val centeredX: Boolean,val centeredY: Boolean)

    fun position(x: Double,y: Double,width: Int,height: Int,boxWidth: Int,boxHeight: Int): Position {
        val px=axis(x,width,boxWidth)
        val py=axis(y,height,boxHeight)
        return Position(px,py,px==(width-boxWidth).coerceAtLeast(0)/2,py==(height-boxHeight).coerceAtLeast(0)/2)
    }

    private fun axis(value: Double,screen: Int,size: Int): Int {
        val far=(screen-size).coerceAtLeast(0)
        val pixel=value.roundToInt().coerceIn(0,far)
        val center=far/2
        return when {
            abs(pixel-center)<=5 -> center
            pixel<=3 -> 0
            far-pixel<=3 -> far
            else -> pixel
        }
    }
}
