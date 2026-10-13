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

/** Presence in the server player list, independent of entity render distance. */
class SafariEntryPresence {
    var missing: Set<String> = emptySet()
        private set
    private var waiting = false
    private var instance: Any? = null
    private var readyAt = 0L

    fun update(now: Long, onIsland: Boolean, inRun: Boolean, instance: Any?,
               members: Set<String>?, online: Set<String>, self: String): Boolean {
        val previous = missing
        if (!onIsland || inRun || instance == null) {
            reset()
        } else {
            if (!waiting || this.instance !== instance) {
                waiting = true
                this.instance = instance
                readyAt = now + 3_000L
            }
            val present = online.mapTo(HashSet(), ::normalize)
            missing = if (now < readyAt || members == null || normalize(self) !in present) emptySet()
                else members.mapTo(HashSet(), ::normalize) - present
        }
        return missing != previous
    }

    fun reset() {
        missing = emptySet()
        waiting = false
        instance = null
        readyAt = 0L
    }

    companion object {
        private fun normalize(uuid: String) = uuid.replace("-", "").lowercase()
        fun blocks(onIsland: Boolean, inRun: Boolean, kick: Boolean, downtime: Boolean,
                   missing: Boolean, shift: Boolean) =
            onIsland && !inRun && (kick || downtime || missing) && !shift

        // The Canyon-to-island chat message is not evidence of entering the actual run.
        fun inRun(location: SafariLocation, mappedArea: Int?) =
            location == SafariLocation.INSIDE && mappedArea in 0..4
    }
}
