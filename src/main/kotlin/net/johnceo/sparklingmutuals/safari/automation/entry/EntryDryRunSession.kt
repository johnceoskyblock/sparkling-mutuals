package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID

data class EntryDryRunObservation(val id: UUID, val species: String)
data class EntryDryRunFrame(val context: EntryContext?, val enabled: Boolean, val atEntry: Boolean,
    val activeRun: Boolean, val arrivedAt: Long)

/** Deliberately cannot represent movement, entity interaction, a menu action or a ticket permit. */
sealed interface EntryDryRunOutput {
    data class Party(val context: EntryContext, val message: EntryMessage, val proposedAt: Long,
        val recipient: EntryPeer? = null) : EntryDryRunOutput
    data class Notice(val text: String, val elapsedMs: Long) : EntryDryRunOutput
}

/** Client-thread confined transport experiment; the admission controller remains disconnected. */
class EntryDryRunSession {
    private var context: EntryContext? = null
    private var arrivedAt = 0L
    private var clock = EntryClock()
    private var sightings = EntrySightings()
    private var requester = EntryReportRequester()
    private var responder = EntryReportResponder(sightings)
    private var remote: EntrySighting? = null
    private var query: EntryMessage.Query? = null
    private var queriedAt = 0L
    private var answered = false
    private var cancelled = false
    private var wasEnabled = false
    private var status = "Off"
    private var emitted = ""
    private var evidenceSeen = false
    private var missingContext = false

    private fun prepare(frame: EntryDryRunFrame, now: Long): EntryContext? {
        val current = frame.context
        val old = context
        val newArrival = current != null && (old == null || old.connection != current.connection || old.arrival != current.arrival)
        if (newArrival) {
            context = current; arrivedAt = frame.arrivedAt; clock = EntryClock(); clock.reset(now)
            sightings = EntrySightings(); requester = EntryReportRequester(); responder = EntryReportResponder(sightings)
            requester.beginArrival(current, arrivedAt)
            remote = null; query = null; answered = false; cancelled = false; wasEnabled = false
            evidenceSeen = false; emitted = ""; missingContext = false
        }
        if (!clock.accept(now) || arrivedAt < 0 || arrivedAt > now) {
            cancelled = true; requester.cancel(); remote = null; status = "Cancelled: invalid monotonic timing"; return null
        }
        if (current == null) {
            if (old != null) {
                cancelled = true; requester.cancel(); remote = null
                if (!missingContext) { sightings = EntrySightings(); responder = EntryReportResponder(sightings) }
            }
            missingContext = true
            status = if (frame.enabled) "Waiting for current Safari location and party" else "Off"
            return null
        }
        missingContext = false
        if (context != current) {
            // Current leaders may serve newly verified members, but no old request or observation survives.
            requester.cancel(); cancelled = true; remote = null
            sightings = EntrySightings(); responder = EntryReportResponder(sightings); context = current
        }
        if (!frame.enabled) {
            if (wasEnabled) { cancelled = true; requester.cancel(); remote = null }
            responder.poll(current, now, false, frame.activeRun, true)
            sightings.retainVisible(current, emptySet(), now)
            status = "Off"; return null
        }
        wasEnabled = true
        return current
    }

    fun scan(frame: EntryDryRunFrame, observed: List<EntryDryRunObservation>, now: Long) {
        val current = prepare(frame, now) ?: return
        sightings.retainVisible(current, observed.mapTo(hashSetOf()) { it.id }, now)
        if (!frame.activeRun && !frame.atEntry) return
        observed.forEach { sightings.observe(current, it.id, it.species, now) }
    }

    /** Revalidate loaded identities at send-time without refreshing their observation timestamps. */
    fun retainLoaded(frame: EntryDryRunFrame, ids: Set<UUID>, now: Long) {
        val current = prepare(frame, now) ?: return
        sightings.retainVisible(current, ids, now)
    }

    fun captured(frame: EntryDryRunFrame, species: String, now: Long) {
        val current = prepare(frame, now) ?: return
        sightings.captured(current, species)
        if (current.account != current.party.leader && (!answered && query != null || remote?.species == species)) {
            requester.cancel(); remote = null; cancelled = true; status = "Cancelled: Sparkling captured"
        }
    }

    fun receive(frame: EntryDryRunFrame, peer: EntryPeer, message: EntryMessage, now: Long) {
        val current = prepare(frame, now) ?: return
        when (message) {
            is EntryMessage.Query -> responder.offer(current, peer, message, now, true, frame.activeRun, true)
            is EntryMessage.Reply -> {
                val sent = query ?: return
                if (cancelled || answered || !frame.atEntry || now >= entryDeadline(arrivedAt, 12000) ||
                    now >= entryDeadline(queriedAt, 3000) || peer.id != current.party.leader ||
                    peer.id !in current.party.members || message.nonce != sent.nonce ||
                    !message.requester.equals(current.selfName, true) || message.realm != current.realm ||
                    message.server != current.server) return
                remote = requester.receive(current, peer, message, now)
                answered = true
            }
        }
    }

    fun tick(frame: EntryDryRunFrame, now: Long): List<EntryDryRunOutput> {
        val out = mutableListOf<EntryDryRunOutput>()
        val current = prepare(frame, now)
        if (current != null) {
            if (current.account == current.party.leader) {
                responder.poll(current, now, true, frame.activeRun, true)?.let {
                    out.add(EntryDryRunOutput.Party(current, it.message, now, it.peer))
                }
                status = if (frame.activeRun) "Leader listening for fresh-status queries" else "Leader waiting outside the run"
            } else when {
                cancelled -> if (!status.contains("captured")) status = "Cancelled: entry context or setting changed"
                now >= entryDeadline(arrivedAt, 12000) -> { requester.cancel(); cancelled = true; status = "Expired: arrival preparation deadline" }
                frame.activeRun -> { requester.cancel(); cancelled = true; status = "Inside run; no entry query" }
                !frame.atEntry -> status = "Waiting for mapped Safari entrance"
                else -> {
                    val fresh = remote?.takeIf { it.valid(current, now) } ?: sightings.freshest(current, now)
                    when {
                        fresh != null -> { evidenceSeen = true; status = "Fresh ${fresh.species}: would check entry NPC/menu; actions disabled" }
                        evidenceSeen -> status = "Evidence expired or no longer loaded; no entry action"
                        answered -> status = "No fresh leader observation; this does not prove the Safari is empty"
                        query != null && now >= entryDeadline(queriedAt, 3000) -> {
                            requester.cancel(); cancelled = true; status = "Leader reply timed out; no retry"
                        }
                        else -> {
                            requester.request(current, now)?.let {
                                query = it; queriedAt = now; out.add(EntryDryRunOutput.Party(current, it, now))
                            }
                            status = "Waiting for fresh leader reply"
                        }
                    }
                }
            }
        }
        if (status != emitted) {
            emitted = status
            out.add(EntryDryRunOutput.Notice(status, (now - arrivedAt).coerceAtLeast(0)))
        }
        return out
    }

    /** Current observation for the separately opted-in approach; never returns cancelled or admitted evidence. */
    fun fresh(frame: EntryDryRunFrame, now: Long): EntrySighting? {
        val current = prepare(frame, now) ?: return null
        if (cancelled || !frame.atEntry || frame.activeRun || now >= entryDeadline(arrivedAt, 12000)) return null
        return remote?.takeIf { it.valid(current, now) } ?: sightings.freshest(current, now)
    }
    fun debug() = listOf("CJ entry dry run: $status", "arrival=$arrivedAt querySent=${query != null} replyReceived=$answered actions=disabled")
}
