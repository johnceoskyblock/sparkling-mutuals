package net.johnceo.sparklingmutuals.safari.structures

import kotlin.math.abs
import kotlin.math.floor
import net.johnceo.sparklingmutuals.safari.SafariRules

/** CritterMod 0.9.0 MoundSpotter, MoundTracker and WallTracker rules (MIT). */
data class MoundShape(val x: Double, val y: Double, val z: Double, val width: Double, val height: Double,
    val distanceSquared: Double, val occupied: Boolean = false)
object SafariMounds {
    const val HITS_TO_BREAK = 5

    /** SafariUtils (MIT): take the server's stage rather than counting click packets. */
    fun hitStage(text: String): Int? = when (text) {
        "Small cracks begin to form in the mound..." -> 1
        "The cracks seem to be getting larger, keep hitting it!" -> 2
        "Chunks of the mound begin falling away..." -> 3
        "The mound is about to fall to pieces! Keep going!" -> 4
        else -> outcome(text)?.let { HITS_TO_BREAK }
    }

    fun detected(shape: MoundShape, limitRange: Boolean = true) = (!limitRange || shape.distanceSquared <= 64.0 * 64) && shape.y <= 65 &&
        shape.width in .35..1.10 && shape.height in .25.. .95 && shape.height <= shape.width + .1 &&
        abs(shape.x - (floor(shape.x) + .5)) < .05 && abs(shape.z - (floor(shape.z) + .5)) < .05 && !shape.occupied
    fun outcome(raw: String): Boolean? {
        val text = SafariRules.strip(raw)
        return when {
            text.startsWith("The mound falls apart, but nothing is inside") -> false
            text.startsWith("The mound fell apart, revealing a Rockmite hidden inside") -> true
            else -> null
        }
    }
}
enum class WallState { INTACT, BROKEN, UNKNOWN }
data class WallSummary(val states: List<WallState>) {
    val intact get() = states.count { it == WallState.INTACT }
    val unknown get() = states.count { it == WallState.UNKNOWN }
    val allBroken get() = states.isNotEmpty() && states.all { it == WallState.BROKEN }
    val remaining get() = "$intact" + if (unknown > 0) " (+$unknown?)" else ""
}

/** Retain unopened sites until they can be checked again, even across biome changes. */
class MoundSurvey {
    private val sites = mutableMapOf<Triple<Int, Int, Int>, Boolean>()
    private val shared = mutableSetOf<Triple<Int,Int,Int>>()
    private val confirmedLocal = mutableSetOf<Triple<Int, Int, Int>>()
    private val inspectedEmpty = mutableSetOf<Triple<Int, Int, Int>>()
    private val confirmedHits = mutableMapOf<Triple<Int, Int, Int>, Int>()
    private val pendingHits = mutableMapOf<Triple<Int, Int, Int>, Long>()
    var recentHitCandidates = 0
        private set

    fun attack(position: Triple<Int, Int, Int>, now: Long): Boolean {
        if (confirmedBroken(position)) return false
        expireHits(now)
        return pendingHits.put(position, now) == null
    }

    fun confirmHit(stage: Int, now: Long): Triple<Int, Int, Int>? {
        expireHits(now)
        recentHitCandidates = pendingHits.size
        val position = pendingHits.keys.singleOrNull()
        if (stage == SafariMounds.HITS_TO_BREAK) {
            pendingHits.clear()
            position?.let(::confirmBreak)
        } else if (position != null && !confirmedBroken(position)) {
            confirmedHits[position] = maxOf(confirmedHits[position] ?: 0, stage)
        }
        return position
    }
    private fun expireHits(now: Long) { pendingHits.entries.removeIf { now - it.value !in 0..2_000 } }
    fun hitLabel(position: Triple<Int, Int, Int>): String? = if (confirmedBroken(position)) null
        else "(${confirmedHits[position] ?: 0}/${SafariMounds.HITS_TO_BREAK})"
    val locallyResolved get() = confirmedLocal + inspectedEmpty
    fun confirmedBroken(position: Triple<Int, Int, Int>) = position in shared || position in confirmedLocal
    fun confirmBreak(position: Triple<Int, Int, Int>) {
        if (position !in shared) confirmedLocal.add(position)
        sites[position] = true
    }
    fun sharedResolved(positions: Collection<Triple<Int,Int,Int>>) {
        shared.addAll(positions)
        inspectedEmpty.removeAll(positions.toSet())
        positions.forEach { sites[it] = true }
    }
    fun sharedResolved(position: Triple<Int,Int,Int>) = position in shared
    fun seenUnopened(position: Triple<Int, Int, Int>) = sites[position] == false
    private var scanned = false
    val allBroken get() = scanned && sites.values.all { it }
    val unopenedSites get() = sites.filterValues { !it }.keys
    val remaining get() = sites.values.count { !it }
    val hasSites get() = sites.isNotEmpty()
    fun scan(present: Set<Triple<Int, Int, Int>>, observable: (Triple<Int, Int, Int>) -> Boolean) {
        scanned = true
        present.forEach {
            inspectedEmpty.remove(it)
            sites[it] = confirmedBroken(it)
        }
        sites.keys.filter { seenUnopened(it) && it !in present && observable(it) }.forEach {
            inspectedEmpty.add(it)
            sites[it] = true
        }
    }
}
