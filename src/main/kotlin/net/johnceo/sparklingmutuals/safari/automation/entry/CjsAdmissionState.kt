package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.history.TicketCounter
import net.johnceo.sparklingmutuals.safari.SafariRules

/** Island presence is not personal ticket admission. Server GAME messages only. */
class CjsAdmissionState {
    private var world: Any? = null
    var admitted = false
        private set
    fun world(current: Any?) {
        if (world !== current) { world = current; admitted = false }
    }
    fun chat(raw: String) {
        if (world == null) return
        val text = SafariRules.strip(raw)
        if (TicketCounter.isTicket(text) || Regex("^HOTSPOT! Your Hunting Hotspot is the (Forest|Cavern|Icy|Haunted) Biome!$").matches(text)) admitted = true
    }
    fun frame(context: EntryContext?, enabled: Boolean, mapped: Int?, generalInside: Boolean, arrivedAt: Long) =
        EntryDryRunFrame(context, enabled, context != null && mapped == 0 && !admitted,
            admitted || context != null && context.account == context.party.leader && generalInside, arrivedAt)
}
