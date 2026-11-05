package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID

enum class ScrappyFood(val label: String, val defaultRgb: Int) {
    WHEAT("Wheat",0xFFFFFF00.toInt()), FISH("Fish",0xFFFF0000.toInt()), LILY_PAD("Lily Pad",0xFF00FF00.toInt())
}

/** Visit-local initial trio inference adapted from SafariUtils ScrappyFeeding (MIT). */
class ScrappyFoods {
    private val bodies = mutableMapOf<UUID, Int>()
    private val otherIds = mutableSetOf<Int>()
    private var base: Int? = null
    private var invalid = false

    fun observe(scrappys: List<Pair<UUID, Int>>, loadedIds: Collection<Int> = emptyList()) {
        val start = base
        scrappys.forEach { (uuid, id) -> if (start == null || id - start in setOf(0, 3, 6)) bodies.putIfAbsent(uuid, id) }
        if (start != null || invalid) return
        val known = bodies.values.toSet()
        otherIds.addAll(loadedIds.asSequence().filterNot { it in known || it in otherIds }
            .take((8192 - otherIds.size).coerceAtLeast(0)).toList())
        otherIds.removeAll(known)
        val first = known.minOrNull() ?: return
        val candidates = listOf(first, first - 3, first - 6).filter { start ->
            val slots = listOf(start, start + 3, start + 6)
            slots.containsAll(known) && slots.none { it in otherIds }
        }
        base = candidates.singleOrNull()
        invalid = candidates.isEmpty() || otherIds.size >= 8192
        if (base != null || invalid) otherIds.clear()
    }

    fun food(id: UUID): ScrappyFood? {
        val offset = (bodies[id] ?: return null) - (base ?: return null)
        return if (offset in 0..6 && offset % 3 == 0) ScrappyFood.entries[offset / 3] else null
    }

    fun reset() { bodies.clear(); otherIds.clear(); base = null; invalid = false }
}
