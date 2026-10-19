package net.johnceo.sparklingmutuals.safari

class SafariLedger {
    private var pendingArrival: Long? = null
    var current: SafariRun? = null
        private set
    var last: SafariRun? = null
        private set
    val displayed get() = current ?: last
    fun arrive(now: Long) { if (current == null) current = SafariRun(pendingArrival ?: now); pendingArrival = null }
    fun worldChanged(now: Long) { leave(now); pendingArrival = now }
    fun enter(now: Long) { leave(now); arrive(now) }
    // The boat is detected before the server's entry banner. Confirm that arrival without erasing it.
    fun confirmEntry(now: Long) = arrive(now)
    fun update(location: SafariLocation, biome: SafariBiome?, now: Long) {
        when (location) {
            SafariLocation.INSIDE -> { arrive(now); if (biome != null) current!!.visitBiome(biome) }
            SafariLocation.ENTRANCE, SafariLocation.OUTSIDE -> leave(now)
            SafariLocation.UNKNOWN -> Unit
        }
    }
    fun leave(now: Long) { current?.let { it.startingAnnouncement.cancel(); it.endedAt = now; last = it }; current = null; pendingArrival = null }
}
