package net.johnceo.sparklingmutuals.safari.automation.entry

import net.johnceo.sparklingmutuals.safari.SafariRoster
import java.util.UUID

data class EntrySighting(val context: EntryContext, val species: String, val event: String, val receivedAt: Long, val expiresAt: Long) {
    init { require(SafariRoster.all.any { it.name == species } && EntryWire.token.matches(event) && receivedAt >= 0 && expiresAt >= receivedAt) }
    fun valid(current: EntryContext, now: Long) = context == current && now >= receivedAt && now < expiresAt
}

internal fun EntryContext.sameArrival(other: EntryContext) = account == other.account && connection == other.connection && arrival == other.arrival
internal fun entryToken() = UUID.randomUUID().toString().replace("-", "")

/** No chat is sent. beginArrival must be called only with independently established arrival evidence. */
class EntryReportRequester(private val token: () -> String = ::entryToken) {
    private var context: EntryContext? = null
    private val clock = EntryClock()
    private var arrivedAt = 0L
    private var sentAt: Long? = null
    private var nonce: String? = null
    private var consumed = false
    fun beginArrival(current: EntryContext, now: Long) {
        if (context?.sameArrival(current) == true) return
        context = current; arrivedAt = now; sentAt = null; nonce = null; consumed = false; clock.reset(now)
    }
    private fun accepts(current: EntryContext, now: Long): Boolean {
        if (context != current || !clock.accept(now)) { consumed = true; return false }
        if (now >= entryDeadline(arrivedAt, 12000)) consumed = true
        return !consumed
    }
    fun request(current: EntryContext, now: Long): EntryMessage.Query? {
        if (!accepts(current, now) || sentAt != null || current.party.leader == current.account) return null
        val fresh = token()
        require(EntryWire.token.matches(fresh))
        nonce = fresh; sentAt = now
        return EntryMessage.Query(fresh, current.realm, current.server)
    }
    fun receive(current: EntryContext, sender: EntryPeer, reply: EntryMessage.Reply, now: Long): EntrySighting? {
        if (!accepts(current, now)) return null
        val sent = sentAt ?: return null
        if (now >= minOf(entryDeadline(sent, 3000), entryDeadline(arrivedAt, 12000))) { consumed = true; return null }
        if (sender.id != current.party.leader || sender.id !in current.party.members ||
            reply.nonce != nonce || !reply.requester.equals(current.selfName, true) ||
            reply.realm != current.realm || reply.server != current.server) return null
        consumed = true
        if (reply !is EntryMessage.Live) return null
        val effectiveAge = reply.ageMs + (now - sent) // RTT is below 3000 and wire age below 5001.
        if (effectiveAge >= 5000) return null
        return EntrySighting(current, reply.species, reply.event, now,
            minOf(entryDeadline(now, 5000 - effectiveAge), entryDeadline(arrivedAt, 12000)))
    }
    fun cancel() { consumed = true }
}

/** One current observation per species; bounded capture history fails closed on overflow. */
class EntrySightings(private val token: () -> String = ::entryToken) {
    private data class Seen(val entity: UUID, val species: String, val event: String, val at: Long)
    private var context: EntryContext? = null
    private val clock = EntryClock()
    private val seen = linkedMapOf<String, Seen>()
    private val retired = mutableMapOf<UUID, String>()
    private var retirementOverflow = false
    private fun scope(current: EntryContext, now: Long): Boolean {
        if (context != current) { context = current; seen.clear(); retired.clear(); retirementOverflow = false; clock.reset(now) }
        if (!clock.accept(now)) { seen.clear(); return false }
        return true
    }
    fun observe(current: EntryContext, entity: UUID, species: String, now: Long): Boolean {
        if (!scope(current, now) || retirementOverflow || SafariRoster.all.none { it.name == species } || entity in retired) return false
        val event = seen[species]?.takeIf { it.entity == entity }?.event ?: token().also { require(EntryWire.token.matches(it)) }
        seen[species] = Seen(entity, species, event, now)
        return true
    }
    fun captured(current: EntryContext, species: String) {
        if (context != current) return
        seen.remove(species)?.let {
            if (retired.size >= 128 && it.entity !in retired) { retirementOverflow = true; seen.clear() }
            else retired[it.entity] = species
        }
    }
    /** Identity knowledge only; this must never be interpreted as fresh authorization. */
    internal fun knowsIdentity(current: EntryContext, species: String) =
        context == current && (species in seen || species in retired.values)
    /** A completed current scan can withdraw visibility without declaring a capture. */
    fun retainVisible(current: EntryContext, ids: Set<UUID>, now: Long) {
        if (scope(current, now)) seen.entries.removeAll { it.value.entity !in ids }
    }
    fun freshest(current: EntryContext, now: Long): EntrySighting? {
        if (!scope(current, now) || retirementOverflow) return null
        val found = seen.values.filter { now >= it.at && now - it.at < 5000 }.maxByOrNull { it.at } ?: return null
        return EntrySighting(current, found.species, found.event, found.at, entryDeadline(found.at, 5000))
    }
}

data class EntryReply(val peer: EntryPeer, val message: EntryMessage.Reply)

/** Bounded proposal queue; enabled/run/destination evidence is required again at dequeue time. */
class EntryReportResponder(private val sightings: EntrySightings) {
    private data class Pending(val peer: EntryPeer, val query: EntryMessage.Query, val at: Long)
    private var context: EntryContext? = null
    private val clock = EntryClock()
    private val used = mutableSetOf<UUID>()
    private val queue = ArrayDeque<Pending>()
    private var lastReply: Long? = null
    private fun scope(current: EntryContext, now: Long): Boolean {
        if (context != current) { context = current; queue.clear(); used.clear(); lastReply = null; clock.reset(now) }
        if (!clock.accept(now)) { queue.clear(); return false }
        return true
    }
    private fun allowed(c: EntryContext, enabled: Boolean, activeRun: Boolean, destinationAssociated: Boolean) =
        enabled && activeRun && destinationAssociated && c.account == c.party.leader
    fun offer(current: EntryContext, peer: EntryPeer, query: EntryMessage.Query, now: Long,
              enabled: Boolean, activeRun: Boolean, destinationAssociated: Boolean): Boolean {
        if (!scope(current, now) || !allowed(current, enabled, activeRun, destinationAssociated) ||
            peer.id == current.account || peer.id !in current.party.members || peer.id in used || used.size >= 3 ||
            query.realm != current.realm || query.server != current.server) return false
        used.add(peer.id); queue.addLast(Pending(peer, query, now))
        return true
    }
    fun poll(current: EntryContext, now: Long, enabled: Boolean, activeRun: Boolean, destinationAssociated: Boolean): EntryReply? {
        if (!scope(current, now)) return null
        if (!allowed(current, enabled, activeRun, destinationAssociated)) { queue.clear(); return null }
        while (queue.firstOrNull()?.let { now >= entryDeadline(it.at, 3000) } == true) queue.removeFirst()
        if (lastReply?.let { now - it < 1500 } == true) return null
        val pending = queue.removeFirstOrNull() ?: return null
        if (pending.peer.id !in current.party.members) return null
        val live = sightings.freshest(current, now)
        val q = pending.query
        val reply = if (live == null) EntryMessage.None(pending.peer.name, q.nonce, current.realm, current.server)
        else EntryMessage.Live(pending.peer.name, q.nonce, current.realm, current.server, live.species,
            5000 - (live.expiresAt - now), live.event)
        lastReply = now
        return EntryReply(pending.peer, reply)
    }
}
