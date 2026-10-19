package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome

/** The exact server HOTSPOT line is the maintainer-confirmed departure signal. Owned by one run. */
class StartingResourceAnnouncement {
    var departedAt: Long? = null
        private set
    var sent = false
        private set
    private var cancelled = false
    fun trigger(text: String, now: Long): Boolean {
        if (departedAt != null || cancelled || SafariBiome.entries.none {
                text == "HOTSPOT! Your Hunting Hotspot is the ${it.label} Biome!" }) return false
        departedAt = now
        return true
    }
    fun dispatch(now: Long, currentOwner: Boolean, report: StartingItemReport?, send: (String) -> Boolean): Boolean {
        val departure = departedAt ?: return false
        if (!currentOwner || cancelled || sent || now - departure < 2000 || report?.scope == null) return false
        if (!send(report.text)) return false
        sent = true
        return true
    }
    fun cancel() { cancelled = true }
}
