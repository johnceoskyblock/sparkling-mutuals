package net.johnceo.sparklingmutuals.safari

object SafariCandleRules {
    fun target(redCandle: Boolean) = redCandle
    fun enabled(toggle: Boolean, haunted: Boolean, clientWorld: Boolean, heldName: String) =
        toggle && haunted && clientWorld && SafariRules.strip(heldName).trim().equals("Soothing Incense", true)
}
