package net.johnceo.sparklingmutuals.safari.automation.entry

/** Strict CJ readiness adds the current upstream hold without any input or Shift bypass. */
internal object CjsApproachReadiness {
    fun allows(safari: Boolean, rosterKnown: Boolean, presenceReady: Boolean,
        identityPending: Boolean, kickHold: Boolean, entryHold: Boolean) =
        safari && rosterKnown && presenceReady && !identityPending && !kickHold && !entryHold
}
