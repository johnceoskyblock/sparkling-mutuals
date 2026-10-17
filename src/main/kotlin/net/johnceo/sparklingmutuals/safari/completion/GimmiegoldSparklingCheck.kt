package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID

/** Coin-source slots, extra party coins and current population, owned by one Safari run. */
class GimmiegoldSparklingCheck {
    private val floors = linkedMapOf<Int, Boolean>()
    private var held: Int? = null
    private var extras = 0
    private var validated = false
    val floorMinimum get() = floors.size
    val remaining get() = floors.values.count { !it }
    val required get() = floorMinimum + extras
    val complete get() = floors.isNotEmpty() && remaining == 0 && held == 0 && validated
    private var population = 0

    /** A disappeared marker is collected only when its location can still be observed. */
    fun floor(id: Int, present: Boolean?, collected: Boolean = false) {
        val previous = floors[id]
        val resolved = when { collected -> true; present != null -> !present; else -> previous ?: false }
        if (previous == null || !resolved) validated = false
        floors[id] = resolved
    }
    fun reenter() { validated = false }
    fun inventory(amount: Int) {
        held = amount.coerceAtLeast(0)
        if (held != 0) validated = false
    }
    /** Notices overlap this player's known starting coins, and repeated notices overlap themselves. */
    fun contributions(starting: Int, localPlayer: String?, notices: Map<String, Int>) {
        val ownNotice = notices.entries.firstOrNull { it.key.equals(localPlayer, true) }?.value ?: 0
        val total = maxOf(starting, ownNotice) + notices.filterKeys { !it.equals(localPlayer, true) }.values.sum()
        if (extras != total) validated = false
        extras = total
    }
    fun scan(captured: Int, live: Collection<UUID>, qualified: Boolean) {
        if (!qualified) return
        population = captured + live.distinct().size
        // Retain a completed check through unloading; new sources/coins revoke it first.
        if (floors.isNotEmpty() && remaining == 0 && held == 0 && population >= required) validated = true
    }
    fun debug() = "floorMinimum=$floorMinimum remaining=$remaining extraCoins=$extras held=$held " +
        "required=$required capturedPlusRendered=$population checked=$complete"
}
