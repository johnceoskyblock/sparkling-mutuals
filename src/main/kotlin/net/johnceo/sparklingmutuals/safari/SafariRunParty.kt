package net.johnceo.sparklingmutuals.safari

/** Save the roster seen together during the run, even if someone leaves before the reward summary. */
internal class SafariRunParty {
    var members: Map<String, String>? = null
        private set
    var presentTogether = false
        private set
    var hotspotUsed = false
        private set

    fun observeHotspot(enabled: Boolean) {
        if (enabled) hotspotUsed = true
    }

    fun observeRoster(roster: Set<String>?, admitted: Boolean, present: () -> Map<String, String>) {
        if (roster == null) return
        val previous = members.orEmpty()
        if (presentTogether && (roster == previous.keys || !roster.containsAll(previous.keys))) return

        val online = present()
        val together = admitted && online.keys.containsAll(roster)
        // A late arrival can expand a verified roster once everyone is present.
        if (presentTogether && !together) return
        members = roster.associateWith { online[it] ?: previous[it].orEmpty() }
        presentTogether = together
    }
}
