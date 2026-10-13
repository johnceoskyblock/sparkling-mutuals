package net.johnceo.sparklingmutuals.safari

/** Nebulune's CPS accumulator, fixed at 12 CPS without jitter or catch-up bursts. */
class SafariClickClock {
    private var credit = 8 // The first press attacks immediately; a full 20-tick interval still has 12 attacks.
    fun tick(active: Boolean): Boolean {
        if (!active) { credit = 8; return false }
        credit += 12
        if (credit < 20) return false
        credit -= 20
        return true
    }
}
data class ClickConditions(val enabled: Boolean = true, val inSafari: Boolean = true,
    val mouseHeld: Boolean = true, val inGame: Boolean = true, val usingItem: Boolean = false,
    val breakingBlock: Boolean = false, val targetBlock: Boolean = false, val rockmiteMound: Boolean = false) {
    fun allowed() = enabled && inSafari && mouseHeld && inGame && !usingItem && !breakingBlock && !targetBlock && rockmiteMound
}
