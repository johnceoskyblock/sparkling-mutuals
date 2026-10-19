package net.johnceo.sparklingmutuals.safari.resources

/** Absolute starting quantities, not remaining inventory or spawning/capture evidence. */
data class StartingItemReport(val quantities: Map<FloorDropType, Int>, val scope: String? = null) {
    val text get() = PREFIX + order.mapNotNull { type -> quantities[type]?.takeIf { it > 0 }?.let { "${it}x ${type.label}" } }
        .joinToString("; ").ifEmpty { "none" } + (scope?.let { " [run:$it]" } ?: "")
    companion object {
        const val PREFIX = "[SM] Starting items: "
        val order = listOf(FloorDropType.COIN, FloorDropType.SEEDS, FloorDropType.WORM, FloorDropType.BERRY)
        private val scoped = Regex("^(.*) \\[run:([0-9a-f]{32})]$")
        fun parse(text: String): StartingItemReport? {
            if (!text.startsWith(PREFIX) || text.length > 256) return null
            val suffix = text.removePrefix(PREFIX)
            val match = scoped.matchEntire(suffix)
            val body = match?.groupValues?.get(1) ?: suffix
            val scope = match?.groupValues?.get(2)
            if (body == "none") return StartingItemReport(emptyMap(), scope)
            val amounts = linkedMapOf<FloorDropType, Int>()
            for (part in body.split("; ")) {
                val item = Regex("^([0-9]+)x (.+)$").matchEntire(part) ?: return null
                val amount = item.groupValues[1].toIntOrNull()?.takeIf { it in 1..64 } ?: return null
                val type = order.firstOrNull { it.label == item.groupValues[2] } ?: return null
                if (amounts.put(type, amount) != null) return null
            }
            return StartingItemReport(amounts.toMap(), scope)
        }
    }
}
