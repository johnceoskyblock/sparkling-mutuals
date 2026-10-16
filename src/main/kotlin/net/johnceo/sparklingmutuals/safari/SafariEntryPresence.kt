package net.johnceo.sparklingmutuals.safari

class SafariEntryRunState {
    private var instance: Any? = null
    private var entered = false
    fun active(instance: Any?, onIsland: Boolean, location: SafariLocation, mappedArea: Int?): Boolean {
        if (this.instance !== instance) { this.instance = instance; entered = false }
        if (!onIsland || instance == null || location == SafariLocation.ENTRANCE) entered = false
        else if (SafariEntryPresence.inRun(location, mappedArea)) entered = true
        return entered
    }
    fun reset() { instance = null; entered = false }
}

/** Current connection player-list membership, independent of entity render distance. Unknown is never ready. */
class SafariEntryPresence {
    var missing: Set<String> = emptySet()
        private set
    var ready = false
        private set
    fun update(now: Long, onIsland: Boolean, inRun: Boolean, instance: Any?,
               members: Set<String>?, online: Set<String>, self: String): Boolean {
        val previous = missing to ready
        val present = online.mapTo(HashSet(), ::normalize)
        val roster = members?.mapTo(HashSet(), ::normalize)
        missing = if (onIsland && !inRun && instance != null && roster != null) roster - present else emptySet()
        ready = onIsland && !inRun && instance != null && roster != null && normalize(self) in present && missing.isEmpty()
        return previous != (missing to ready)
    }
    fun reset() { missing = emptySet(); ready = false }
    companion object {
        fun normalize(uuid: String) = uuid.replace("-", "").lowercase()
        fun inRun(location: SafariLocation, mappedArea: Int?) =
            location == SafariLocation.INSIDE && mappedArea in 0..4
    }
}
