package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState

/** One run's boat departure, independent of completion reports and presentation settings. */
class StartingResourceAnnouncement(private val resources: SafariResources) {
    private var leftAt: Long? = null
    private val announced = mutableMapOf<FloorDropType, Int>()
    private var nextAt = 0L
    private var hotspot: String? = null
    private var hotspotHandled = false

    fun observeHotspot(raw: String, now: Long) {
        val text = net.johnceo.sparklingmutuals.safari.SafariRules.strip(raw)
        val match = Regex("^HOTSPOT! Your Hunting Hotspot is the (Forest|Cavern|Icy|Haunted) Biome!$").matchEntire(text) ?: return
        if (leftAt == null) { leftAt = now; hotspot = match.groupValues[1] }
    }

    fun dispatch(now: Long, party: PartySparklingState, send: (String) -> Unit): Boolean = dispatch(now, party, false, send)
    fun dispatch(now: Long, party: PartySparklingState, hotspotEnabled: Boolean, send: (String) -> Unit): Boolean =
        dispatch(now, party, hotspotEnabled, true, send)
    fun dispatch(now: Long, party: PartySparklingState, hotspotEnabled: Boolean, startingItemsEnabled: Boolean, send: (String) -> Unit): Boolean {
        val departure = leftAt ?: return false
        if (now < nextAt || now - departure < 500) return false
        if (!hotspotHandled) {
            if (hotspotEnabled) {
                send("[SM] Hotspot: $hotspot")
                hotspotHandled = true
                nextAt = now + 1500
                return true
            }
            hotspotHandled = true
        }
        if (!startingItemsEnabled || !party.ready) return false
        val notice = resources.startingNotice(party) ?: return false
        // Delayed starting rewards can amend the early report, but consumption cannot repeat it.
        if (notice.items.none { it.quantity > (announced[it.type] ?: 0) }) return false
        send(notice.startingTexts().single())
        notice.items.forEach { announced[it.type] = maxOf(announced[it.type] ?: 0, it.quantity) }
        nextAt = now + 1500
        return true
    }
}
