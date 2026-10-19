package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome

/** Callers must verify current party membership and run provenance before report(). */
class StartingResourceLedger {
    private val amounts = linkedMapOf<Pair<String, FloorDropType>, Int>()
    private val received = mutableSetOf<String>()
    private var members: Set<String>? = null
    private val revisions = mutableMapOf<SafariBiome, Long>()
    val complete get() = members?.all { it in received } == true
    val missing get() = members?.minus(received)
    val foodTotal get() = StartingItemReport.order.filter { it != FloorDropType.COIN }.sumOf(::total)
    fun revision(biome: SafariBiome) = revisions[biome] ?: 0L
    fun roster(current: Set<String>?) {
        val next = current?.mapTo(linkedSetOf(), String::lowercase)
        if (next == members) return
        members = next
        // Roster changes make older remote checks inapplicable even if the added member reports none.
        SafariBiome.entries.filter { it == SafariBiome.FOREST || it == SafariBiome.HAUNTED }.forEach(::advance)
    }
    private fun advance(biome: SafariBiome) { revisions[biome] = revision(biome) + 1 }
    fun report(sender: String, quantities: Map<FloorDropType, Int>, complete: Boolean): Boolean {
        require(quantities.keys.all { it in StartingItemReport.order } && quantities.values.all { it in 1..64 })
        val who = sender.lowercase()
        var changed = false
        quantities.forEach { (type, amount) ->
            val key = who to type
            if (amount > (amounts[key] ?: 0)) {
                amounts[key] = amount
                advance(if (type == FloorDropType.COIN) SafariBiome.HAUNTED else SafariBiome.FOREST)
                changed = true
            }
        }
        if (complete && received.add(who)) {
            advance(SafariBiome.FOREST); advance(SafariBiome.HAUNTED)
            changed = true
        }
        return changed
    }
    fun total(type: FloorDropType) = amounts.filterKeys { it.second == type }.values.sum()
    fun coins() = amounts.filterKeys { it.second == FloorDropType.COIN }.mapKeys { it.key.first }
    fun food() = amounts.filterKeys { it.second != FloorDropType.COIN }.toMap()
}
