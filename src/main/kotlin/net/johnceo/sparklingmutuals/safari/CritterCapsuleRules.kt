/*
 * Adapted from SkyHanni 9.1.0 CritterCapsuleHider.kt (LGPL-2.1).
 * Copyright SkyHanni contributors. See licenses/SkyHanni-LGPL-2.1.txt.
 * Modified 2026-10-05: standalone rules, two enabled toggles and close-only flying mode.
 */
package net.johnceo.sparklingmutuals.safari

object CritterCapsuleRules {
    fun hidden(inSafari: Boolean, flying: Boolean, itemId: String?, distanceSquared: Double,
        hideGround: Boolean, hideFlying: Boolean, closeDistance: Float): Boolean {
        if (!inSafari) return false
        return if (flying) hideFlying &&
            itemId in setOf("CRITTER_CAPSULE", "MASTERFUL_CRITTER_CAPSULE") &&
            distanceSquared <= closeDistance.toDouble() * closeDistance
        else hideGround && itemId == "CRITTER_CAPSULE"
    }
}
