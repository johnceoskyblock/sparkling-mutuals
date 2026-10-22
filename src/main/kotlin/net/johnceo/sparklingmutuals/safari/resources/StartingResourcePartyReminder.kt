package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.commands.PartyReplyChunks
import net.johnceo.sparklingmutuals.safari.PartySparklingState

/** One reminder batch per run after local completion, including a peer-reported shared clear. */
class StartingResourcePartyReminder(private val resources: SafariResources) {
    private var sentOnce = false
    private var active = false
    private var nextAt = 0L
    private var message: String? = null
    private val queued = ArrayDeque<String>()

    fun afterBiomeReport(now: Long) {
        if (active || sentOnce) return
        active = true; nextAt = now + 1500; message = null; queued.clear()
    }

    fun dispatch(now: Long, party: PartySparklingState, nameOf: (String) -> String?, send: (String) -> Unit): Boolean {
        if (!active || now < nextAt || !party.ready) return false
        val notice = resources.startingNotice(party)
        if (notice == null) { active = false; queued.clear(); return false }
        val reasons = (notice.namedNeeds(party, nameOf) ?: return false).map { (members, critters) ->
            val players = members.joinToString(", ")
            "$players still ${if (members.size == 1) "needs" else "need"} Shiny ${critters.joinToString(", ")}"
        }
        val items = notice.items.joinToString(", ") { "${it.quantity}x ${it.type.label}" }
        val current = "Need to use: $items; ${reasons.joinToString("; ")}. Please wait."
        // Rebuild if quantities or discovery/name information changed while a longer message was pending.
        if (current != message && sentOnce) { active = false; queued.clear(); return false }
        if (current != message) { message = current; queued.clear(); queued.addAll(PartyReplyChunks.split(current)) }
        send(queued.first())
        sentOnce = true
        queued.removeFirst()
        active = queued.isNotEmpty()
        nextAt = now + 1500
        return true
    }
}
