package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID

enum class ScrappyFood(val label: String, val defaultRgb: Int) {
    WHEAT("Wheat",0xFFFFFF00.toInt()), FISH("Fish",0xFFFF0000.toInt()), LILY_PAD("Lily Pad",0xFF00FF00.toInt())
}

/** Reported three-spawn ordering. Never rank a partially loaded set or discard captured IDs mid-run. */
class ScrappyFoods {
    private val seen = linkedMapOf<UUID,Int>()
    private var assigned = emptyMap<UUID,ScrappyFood>()
    fun observe(scrappys: List<Pair<UUID,Int>>) {
        if (assigned.isNotEmpty()) return
        scrappys.forEach { (uuid,id) -> seen.putIfAbsent(uuid,id) }
        if (seen.size != 3 || seen.values.toSet().size != 3) return
        assigned = seen.entries.sortedBy { it.value }.mapIndexed { i,entry -> entry.key to ScrappyFood.entries[i] }.toMap()
    }
    fun food(id: UUID) = assigned[id]
    fun reset() { seen.clear(); assigned=emptyMap() }
}
