package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID
import net.minecraft.world.phys.Vec3

internal data class EspPityBody(
    val id: UUID,
    val species: String,
    val position: Vec3,
    val named: Boolean,
    val capturing: Boolean = false,
)

/** Confirmed attempt and delayed body association adapted from SafariUtils CritterState (MIT). */
class EspCaptureMemory {
    private data class Attempt(val id: java.util.UUID, val species: String, val at: Long)
    private val pending = mutableListOf<Attempt>()
    internal data class RecatchPin(val species: String, val position: Vec3, val expiresAt: Long)
    private val pins = mutableMapOf<UUID, RecatchPin>()

    internal fun recatchPins(now: Long): Collection<RecatchPin> {
        pins.values.removeIf { now >= it.expiresAt }
        return pins.values
    }

    private fun resolvePin(id: UUID, escaped: Boolean, now: Long) {
        if (!escaped) pins.remove(id)
        else pins[id]?.let { pins[id] = it.copy(expiresAt = now + 8_000) }
    }
    private val labelOwners = mutableMapOf<java.util.UUID, Pair<String, java.util.UUID>>()
    /** An unchanged name tag is the strongest evidence for a body replacement. */
    fun bindLabel(id: java.util.UUID, species: String, label: java.util.UUID, liveBodies: Set<java.util.UUID>) {
        val previous=labelOwners[label]
        if(previous!=null && previous.second!=id && previous.second in liveBodies)return
        if(previous!=null && previous.first==species && previous.second!=id && !hidden(previous.second)) {
            val old=previous.second
            val launch = breakouts[old]?.launchedAt ?: pending.firstOrNull { it.id == old }?.at
            if (launch != null && seenBodies[id]?.firstSeenAt?.let { it < launch } == true) return
            if (!moveAttemptState(old, id)) return
            breakouts.remove(old)?.let { breakouts[id] = it }
        }
        labelOwners[label]=species to id
    }
    private fun unclaimed(id: UUID) = id !in pity && id !in handoffBodies && !hidden(id)

    private fun moveAttemptState(old: UUID, id: UUID): Boolean {
        // An independently tracked critter is never a blank replacement body.
        if (!unclaimed(id)) return false
        handoffBodies.add(old)
        handoffBodies.add(id)
        pity.remove(old)?.let { pity[id] = it }
        pins.remove(old)?.let { if (old !in breakouts) pins[id] = it }
        for (index in pending.indices) if (pending[index].id == old) pending[index] = pending[index].copy(id = id)
        recent.entries.forEach { if (it.value.id == old) it.setValue(it.value.copy(id = id)) }
        for (index in input.indices) input[index] = input[index].copy(aim = input[index].aim.mapValues { (_, aim) ->
            if (aim.id == old) aim.copy(id = id) else aim
        })
        return true
    }
    private val pity = mutableMapOf<java.util.UUID, Int>()
    fun pityFor(id: java.util.UUID) = pity[id] ?: 0
    private val captured = mutableSetOf<java.util.UUID>()
    private val recent = mutableMapOf<String, Attempt>()
    private data class Throw(val at: Long, val aim: Map<String, Attempt>)
    private val input = mutableListOf<Throw>()
    private data class SeenBody(val firstSeenAt: Long, val position: Vec3)
    private data class Breakout(val species: String, val launchedAt: Long, val escapedAt: Long)
    private enum class Outcome { ESCAPED, CAUGHT, DODGED }
    private data class DeferredAttempt(val species: String, val at: Long, var outcome: Outcome? = null)
    private data class Animation(val species: String, val at: Long, var used: Boolean = false)
    private val seenBodies = mutableMapOf<UUID, SeenBody>()
    private val handoffBodies = mutableSetOf<UUID>()
    private val breakouts = mutableMapOf<UUID, Breakout>()
    private val deferred = mutableListOf<DeferredAttempt>()
    private val animations = mutableMapOf<UUID, Animation>()
    private val diagnostics = ArrayDeque<String>()

    internal fun recentAttempts(): List<String> = diagnostics.toList()
    private fun trace(message: String) {
        if (diagnostics.size >= 12) diagnostics.removeFirst()
        diagnostics.addLast(message)
    }

    /** A confirmed escape and a unique nearby return can establish identity when both IDs change. */
    internal fun observeBodies(bodies: List<EspPityBody>, now: Long): List<Pair<String, UUID>> {
        for (body in bodies) {
            val seen = seenBodies[body.id]
            val position = if (seen == null || body.named) body.position else seen.position
            seenBodies[body.id] = SeenBody(seen?.firstSeenAt ?: now, position)
        }
        val active = bodies.filter { it.capturing && !hidden(it.id) }
        for (body in active) {
            animations.putIfAbsent(body.id, Animation(body.species, now))
            if (pending.any { it.id == body.id }) animations[body.id]?.used = true
        }
        val resolved = mutableListOf<Pair<String, UUID>>()
        expire(now)
        for (attempt in deferred.toList()) {
            val body = animations.entries.singleOrNull { (_, animation) ->
                animation.species == attempt.species && !animation.used &&
                    animation.at - attempt.at in -500..1_500
            } ?: continue
            body.value.used = true
            deferred.remove(attempt)
            aimed(body.key, attempt.species, now, confirmedThrow = true, launchedAt = attempt.at)
            when (attempt.outcome) {
                Outcome.CAUGHT -> {
                    retire(body.key)
                    resolved.add(attempt.species to body.key)
                }
                Outcome.ESCAPED, Outcome.DODGED -> {
                    pending.removeAll { it.id == body.key }
                    resolvePin(body.key, attempt.outcome == Outcome.ESCAPED, now)
                    if (attempt.outcome == Outcome.ESCAPED) breakouts[body.key] = Breakout(attempt.species, attempt.at, now)
                }
                null -> Unit
            }
            trace("${attempt.species} throw matched capture animation pity=${pityFor(body.key)}")
        }
        val activeIds = active.mapTo(hashSetOf()) { it.id }
        animations.entries.removeIf { (id, animation) -> id !in activeIds && (animation.used || now - animation.at > 500) }
        reconcileReturns(bodies, now)
        return resolved
    }

    private fun reconcileReturns(bodies: List<EspPityBody>, now: Long) {
        // Hideons can settle in place and remain outside render range until you come back.
        breakouts.entries.removeIf { !SafariEspRules.hideonForm(it.value.species) && now - it.value.escapedAt > 5_000 }
        if (breakouts.isEmpty()) return
        val named = bodies.filter { it.named && !it.capturing && !hidden(it.id) }
        val namedIds = named.mapTo(hashSetOf()) { it.id }
        val candidates = breakouts.filterKeys { it !in namedIds && !hidden(it) }.mapValues { (id, breakout) ->
            val position = seenBodies[id]?.position
            val stationary = SafariEspRules.hideonForm(breakout.species)
            val nearbyPeer = position != null && named.any { body ->
                body.species == breakout.species && (seenBodies.getValue(body.id).firstSeenAt < breakout.launchedAt ||
                    !unclaimed(body.id)) && position.distanceToSqr(body.position) <= 16.0
            }
            val radius = if (stationary || nearbyPeer) 1.5 else 4.0
            named.filter { body ->
                val firstSeenAt = seenBodies.getValue(body.id).firstSeenAt
                position != null && body.species == breakout.species &&
                    unclaimed(body.id) && firstSeenAt >= breakout.launchedAt &&
                    (stationary || !nearbyPeer || firstSeenAt - breakout.escapedAt in -1_000..1_500) &&
                    position.distanceToSqr(body.position) <= radius * radius
            }
        }
        for ((old, options) in candidates) {
            val successor = options.singleOrNull() ?: continue
            if (candidates.values.count { successor in it } != 1) continue
            if (!moveAttemptState(old, successor.id)) continue
            breakouts.remove(old)
            labelOwners.entries.removeAll { it.value.second == old }
            trace("${successor.species} returned ${old.toString().take(8)} → ${successor.id.toString().take(8)} pity=${pityFor(successor.id)}")
        }
    }
    fun sighted(id: java.util.UUID, species: String, now: Long) {
        if (!hidden(id)) recent[species] = Attempt(id, species, now)
    }
    /** Freeze aim at item use, before a capsule impact or camera turn changes the scan. */
    fun throwing(now: Long) {
        expireInput(now)
        val aim = recent.filterValues { now - it.at in 0..250 && !hidden(it.id) }
        if (aim.isNotEmpty()) input.add(Throw(now, aim))
    }
    fun throwingAt(now: Long, id: java.util.UUID, species: String) {
        throwingAt(now, mapOf(species to id))
    }
    internal fun throwingAt(now: Long, candidates: Map<String, java.util.UUID>) {
        expireInput(now)
        val aim = candidates.filterValues { !hidden(it) }.mapValues { (species, id) -> Attempt(id, species, now) }
        if (aim.isNotEmpty()) input.add(Throw(now, aim))
    }
    private fun expireInput(now: Long) { input.removeAll { now - it.at !in 0..6000 } }
    fun threw(species: String, now: Long, recoveredTarget: () -> UUID? = { null }) {
        expireInput(now)
        val index = input.indexOfFirst { now - it.at <= 3000 && it.aim[species]?.let { aim -> !hidden(aim.id) } == true }
        // Wait for body evidence when the local launch and current aim cannot identify it.
        if (index < 0) {
            val recovered = recoveredTarget()?.takeUnless(::hidden)
            if (recovered == null) {
                expire(now)
                deferred.add(DeferredAttempt(species, now))
                trace("$species throw awaiting capture animation")
                return
            }
            aimed(recovered, species, now, confirmedThrow = true)
            trace("$species throw matched confirmed-species aim ${recovered.toString().take(8)} pity=${pityFor(recovered)}")
            return
        }
        val launch = input.removeAt(index)
        val attempt = launch.aim.getValue(species)
        // A new server-confirmed throw at this body also proves the previous attempt ended,
        // even if we missed its escape message.
        aimed(attempt.id, species, now, confirmedThrow = true, launchedAt = launch.at)
        trace("$species throw matched ${attempt.id.toString().take(8)} pity=${pityFor(attempt.id)}")
    }
    private fun expire(now: Long) {
        pending.removeAll { now - it.at !in 0..40_000 }
        deferred.removeAll { now - it.at !in 0..1_500 }
    }
    fun aimed(id: java.util.UUID, species: String, now: Long, confirmedThrow: Boolean = false, launchedAt: Long = now) {
        expire(now)
        if (hidden(id)) return
        breakouts.remove(id)
        val index = pending.indexOfFirst { it.id == id }
        if (index < 0 || confirmedThrow) pity[id] = pityFor(id) + 1
        if (index < 0) pending.add(Attempt(id, species, launchedAt))
        else pending[index] = Attempt(id, species, launchedAt)
        if (confirmedThrow) recordPin(id, species, now)
    }
    private fun recordPin(id: UUID, species: String, now: Long) {
        if (!net.johnceo.sparklingmutuals.config.ConfigManager.recatchWaypoint) return
        seenBodies[id]?.let { pins[id] = RecatchPin(species, it.position, now + 40_000) }
    }
    fun escaped(species: String, now: Long, brokeOut: Boolean = true): java.util.UUID? {
        expire(now)
        val attempt = pending.firstOrNull { it.species == species }
            ?: if (brokeOut) recoverBreakout(species, now) else null
        if (attempt == null) {
            deferred.firstOrNull { it.species == species && it.outcome == null }?.outcome =
                if (brokeOut) Outcome.ESCAPED else Outcome.DODGED
            trace("$species escape had no matching throw")
            return null
        }
        pending.remove(attempt)
        if (brokeOut) breakouts[attempt.id] = Breakout(species, attempt.at, now)
        resolvePin(attempt.id, brokeOut, now)
        trace("$species escape matched ${attempt.id.toString().take(8)} pity=${pityFor(attempt.id)}")
        return attempt.id
    }
    /** A breakout is itself confirmation if every still-unmatched local throw points to the same body. */
    private fun recoverBreakout(species: String, now: Long): Attempt? {
        expireInput(now)
        val matching = input.filter { it.aim[species]?.let { aim -> !hidden(aim.id) } == true }
        val attempts = matching.map { it.aim.getValue(species) }
        if (attempts.map { it.id }.distinct().size != 1) return null
        val attempt = attempts.last().copy(at = matching.last().at)
        input.removeAll(matching.toSet())
        pity[attempt.id] = pityFor(attempt.id) + 1
        recordPin(attempt.id, species, now)
        return attempt
    }
    fun caught(species: String, now: Long): java.util.UUID? {
        expire(now)
        val attempt = pending.firstOrNull { it.species == species }
        if (attempt == null) {
            deferred.firstOrNull { it.species == species && it.outcome == null }?.outcome = Outcome.CAUGHT
            return null
        }
        pending.remove(attempt); captured.add(attempt.id)
        pins.remove(attempt.id)
        recent.entries.removeAll { it.value.id == attempt.id }
        return attempt.id
    }
    fun retire(id: java.util.UUID) {
        captured.add(id)
        breakouts.remove(id)
        pins.remove(id)
        pending.removeAll { it.id == id }
        recent.entries.removeAll { it.value.id == id }
    }
    fun hidden(id: java.util.UUID) = id in captured
    fun reset() {
        pending.clear(); labelOwners.clear(); pity.clear(); captured.clear(); recent.clear(); input.clear()
        seenBodies.clear(); handoffBodies.clear(); breakouts.clear(); deferred.clear(); animations.clear(); diagnostics.clear(); pins.clear()
    }
}
