package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID

/** Coin-source slots, extra party coins and current population, owned by one Safari run. */
class GimmiegoldSparklingCheck {
    private val floors = linkedMapOf<Int, Boolean>()
    private var held: Int? = null
    private var extras = 0
    private var validated = false
    private var catalogKnown = false
    private var observedRemaining: Int? = null
    var revision = 0L
        private set
    val floorMinimum get() = floors.size
    val remaining get() = floors.values.count { !it }
    val required get() = floorMinimum + extras
    val knownBlocked get() = (held ?: 0) > 0 || (observedRemaining ?: 0) > 0
    val complete get() = catalogKnown && !knownBlocked && held == 0 && validated
    private var population = 0

    /** Absence/unloading alone never marks a slot collected. */
    fun floor(id: Int, present: Boolean?, collected: Boolean = false) {
        val previous = floors[id]
        val resolved = when { collected -> true; present == true -> false; else -> previous ?: false }
        if (previous == null || present == true && !collected) validated = false
        if (previous == null) revision++
        if (present == true) {
            if ((observedRemaining ?: 0) == 0) revision++
            observedRemaining = maxOf(1, observedRemaining ?: 0)
        }
        floors[id] = resolved
        if (collected && floors.values.all { it }) observedRemaining = 0
    }
    fun sourceEvidence(known: Boolean, remaining: Int?) {
        if (catalogKnown != known) { revision++; validated = false }
        catalogKnown = known
        val next = remaining?.takeIf { it >= 0 }
        if ((next ?: 0) > (observedRemaining ?: 0)) revision++
        observedRemaining = next
        if (!known || (observedRemaining ?: 0) > 0) validated = false
    }
    fun reenter() { validated = false }
    fun inventory(amount: Int) {
        val next = amount.coerceAtLeast(0)
        if (next > (held ?: 0)) revision++
        held = next
        if (held != 0) validated = false
    }
    /** Notices overlap this player's known starting coins, and repeated notices overlap themselves. */
    fun contributions(starting: Int, localPlayer: String?, notices: Map<String, Int>) {
        val ownNotice = notices.entries.firstOrNull { it.key.equals(localPlayer, true) }?.value ?: 0
        val total = maxOf(starting, ownNotice) + notices.filterKeys { !it.equals(localPlayer, true) }.values.sum()
        if (extras != total) { validated = false; revision++ }
        extras = total
    }
    fun scan(captured: Int, live: Collection<UUID>, qualified: Boolean) {
        if (!qualified) return
        population = captured + live.distinct().size
        // Retain a completed check through unloading; new sources/coins revoke it first.
        if (catalogKnown && (observedRemaining == 0 || floors.isNotEmpty() && remaining == 0) &&
            held == 0 && population >= required) validated = true
    }
    fun debug() = "floorMinimum=$floorMinimum unresolvedSlots=$remaining catalogKnown=$catalogKnown observedRemaining=$observedRemaining extraCoins=$extras held=$held " +
        "required=$required capturedPlusRendered=$population checked=$complete"
}
