package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState

/** One run's boat departure, independent of completion reports and presentation settings. */
class StartingResourceAnnouncement(private val resources: SafariResources) {
    private var sawBoat = false
    private var leftAt: Long? = null
    private val announced = mutableMapOf<FloorDropType, Int>()
    private var nextAt = 0L

    fun observeArea(area: Int?, now: Long) {
        if (area == 0) sawBoat = true
        if (sawBoat && area in 1..4 && leftAt == null) leftAt = now
    }

    fun dispatch(now: Long, party: PartySparklingState, send: (String) -> Unit): Boolean {
        val departure = leftAt ?: return false
        if (now < nextAt || now - departure < 500 || !party.ready) return false
        val notice = resources.startingNotice(party) ?: return false
        // Delayed starting rewards can amend the early report, but consumption cannot repeat it.
        if (notice.items.none { it.quantity > (announced[it.type] ?: 0) }) return false
        send(notice.startingTexts().single())
        notice.items.forEach { announced[it.type] = maxOf(announced[it.type] ?: 0, it.quantity) }
        nextAt = now + 1500
        return true
    }
}
