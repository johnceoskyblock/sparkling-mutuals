package net.johnceo.sparklingmutuals.safari.hideyho

import net.minecraft.core.BlockPos

/** Marker phases adapted from SafariUtils HideyhoSolver (MIT), independent of the encounter banner. */
internal class HideyhoMarker {
    private enum class Phase { START, PENDING, END, DONE }
    private var phase = Phase.START
    private var startPosition: BlockPos? = null

    fun chat(line: String, position: BlockPos?) {
        when (line) {
            "[MOB] Hideyho: Hehe, ok!" -> if (phase == Phase.START) {
                startPosition = position
                phase = Phase.PENDING
            }
            "[MOB] Hideyho: No peeking!" -> if (phase == Phase.START || phase == Phase.PENDING) {
                startPosition = startPosition ?: position
                phase = Phase.END
            }
            "[MOB] Hideyho: Aah! You found me!" -> phase = Phase.DONE
        }
    }

    fun title(position: BlockPos): String? = when (phase) {
        Phase.START -> "Hideyho (START)"
        Phase.END -> if (position != startPosition) "Hideyho (END)" else null
        Phase.PENDING, Phase.DONE -> null
    }
}
