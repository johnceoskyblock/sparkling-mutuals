package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

enum class SafariTicketType(val label: String) {
    BASIC("Basic"), ECONOMY("Economy"), PREMIUM("Premium"), FIRST_CLASS("First Class");
    companion object {
        fun fromItem(raw: String): SafariTicketType? {
            val text = SafariRules.strip(raw).replace('-', ' ')
            if (!text.contains("Critter Safari", true)) return null
            return entries.firstOrNull { text.contains(it.label, true) }
        }
    }
}

/** Entry context never invents starting resources from an assumed ticket bonus. */
class SafariEntryEvidence {
    var ticket: SafariTicketType? = null
    var admission: String? = null
    var headStart: FloorDropType? = null
        private set
    var hotspot: SafariBiome? = null
        private set
    fun chat(text: String) {
        val item = Regex("^HEAD START! You started with a (.+)!$").matchEntire(text)?.groupValues?.get(1)
        if (item != null) headStart = FloorDropType.entries.firstOrNull { it.label == item }
        val biome = Regex("^HOTSPOT! Your Hunting Hotspot is the (.+) Biome!$").matchEntire(text)?.groupValues?.get(1)
        if (biome != null) hotspot = SafariBiome.entries.firstOrNull { it.label.equals(biome, true) }
    }
}
