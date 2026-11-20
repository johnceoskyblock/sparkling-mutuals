package net.johnceo.sparklingmutuals.safari

/** Species and exact Sparkling-prefix matching adapted from CritterMod v0.9.0 (MIT). */
object SafariRules {
    const val RADIUS = 80.0
    private val formatting = Regex("§.")
    private val glyphs = Regex("[\\p{Cf}\\p{Co}]")
    private val dialogue = Regex("^\\[(?:NPC|MOB)] ", RegexOption.IGNORE_CASE)
    private val hideyho = Regex("^\\[(?:NPC|MOB)] Hideyho:.*$", RegexOption.IGNORE_CASE)
    private val sleepingWumpa = Regex("^Wumpa\\s+zZz$", RegexOption.IGNORE_CASE)
    private val safariBiomes = setOf("forest biome", "cavern biome", "icy biome", "haunted biome", "haunted mansion")
    private val hauntedZones = setOf("haunted biome", "haunted mansion")
    private val entrances = setOf("safari entrance", "critter safari entrance", "safari entry", "critter safari entry")

    fun strip(text: String): String {
        // Plain item names and scoreboard lines need no regex matchers.
        for (i in text.indices) {
            if (text[i] > '\u007f') return text.replace(formatting, "").replace(glyphs, "").trim()
        }
        return text.trim()
    }
    fun withinRange(distanceSquared: Double) = distanceSquared >= 0 && distanceSquared <= RADIUS * RADIUS
    fun sparklingSpecies(text: String): String? {
        val name = strip(text)
        if (!name.startsWith("Sparkling ", ignoreCase = true)) return null
        val species = name.substring(10).trim()
        if (sleepingWumpa.matches(species)) return "Wumpa"
        return SafariRoster.named(species)?.name
    }
    fun zone(lines: List<String>): String? {
        var markerLine: String? = null
        for (line in lines) {
            val normalized = strip(line)
            if (normalized.startsWith("Area: ")) return normalized.substring(6).trim()
            if (markerLine == null && '⏣' in normalized) markerLine = normalized
        }
        return markerLine?.substringAfter('⏣')?.trim()
    }
    fun isSafari(lines: List<String>) = isSafariZone(zone(lines), lines)
    fun isHaunted(lines: List<String>) = zone(lines)?.lowercase() in hauntedZones
    fun isEntrance(lines: List<String>) = isEntranceZone(zone(lines))
    fun isIslandName(lines: List<String>) = isIslandZone(zone(lines))
    internal fun isEntranceZone(zone: String?) = zone?.lowercase() in entrances
    internal fun isIslandZone(zone: String?) = when (zone?.lowercase()) {
        "critter safari", "safari" -> true
        else -> false
    }
    internal fun isSafariZone(zone: String?, lines: List<String>) = if (zone != null) {
        zone.equals("Critter Safari", true) || zone.equals("Safari", true) || zone.lowercase() in safariBiomes
    } else lines.any { strip(it).equals("Critter Safari", true) }
    fun isDialogue(text: String) = dialogue.containsMatchIn(strip(text))
    fun isHideyhoDialogue(text: String) = hideyho.matches(strip(text))
}

/** Area 0 is the boat/center. An active run can temporarily leave the graph during death/respawn. */
enum class SafariLocation {
    INSIDE, ENTRANCE, OUTSIDE, UNKNOWN;
    companion object {
        fun resolve(lines: List<String>, mappedArea: Int?, entered: Boolean): SafariLocation {
            val zone = SafariRules.zone(lines)
            return when {
                SafariRules.isEntranceZone(zone) -> ENTRANCE
                mappedArea == null && !entered && SafariRules.isIslandZone(zone) -> OUTSIDE
                SafariRules.isSafariZone(zone, lines) -> INSIDE
                zone != null -> OUTSIDE
                entered && mappedArea in 0..4 -> INSIDE
                else -> UNKNOWN
            }
        }
    }
}
