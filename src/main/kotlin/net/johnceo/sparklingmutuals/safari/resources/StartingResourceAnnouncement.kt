package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState

/** One run's boat departure, independent of completion reports and presentation settings. */
class StartingResourceAnnouncement(private val resources: SafariResources) {
    private var sawBoat = false
    private var leftAt: Long? = null
    private var sent = false

    fun observeArea(area: Int?, now: Long) {
        if (area == 0) sawBoat = true
        if (sawBoat && area in 1..4 && leftAt == null) leftAt = now
    }

    fun dispatch(now: Long, party: PartySparklingState, send: (String) -> Unit): Boolean {
        val departure = leftAt ?: return false
        if (sent || now - departure < 500 || !party.ready) return false
        val notice = resources.startingNotice(party)
        if (notice != null) {
            send(notice.startingTexts().single())
            sent = true
            return true
        }
        // No applicable items is intentionally silent; API loading is handled above.
        sent = true
        return false
    }
}
