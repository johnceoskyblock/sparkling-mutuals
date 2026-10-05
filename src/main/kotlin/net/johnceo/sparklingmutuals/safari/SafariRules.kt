package net.johnceo.sparklingmutuals.safari

/** Species and exact Sparkling-prefix matching adapted from CritterMod v0.9.0 (MIT). */
object SafariRules {
    const val RADIUS = 80.0
    private val formatting = Regex("§.")
    private val glyphs = Regex("[\\p{Cf}\\p{Co}]")
    private val dialogue = Regex("^\\[(?:NPC|MOB)] ", RegexOption.IGNORE_CASE)
    private val hideyho = Regex("^\\[(?:NPC|MOB)] Hideyho:.*$", RegexOption.IGNORE_CASE)

    fun strip(text: String) = text.replace(formatting, "").replace(glyphs, "").trim()
    fun withinRange(distanceSquared: Double) = distanceSquared >= 0 && distanceSquared <= RADIUS * RADIUS
    fun sparklingSpecies(text: String): String? {
        val name = strip(text)
        if (!name.startsWith("Sparkling ", ignoreCase = true)) return null
        return SafariRoster.named(name.substring(10).trim())?.name
    }
    private fun zone(lines: List<String>): String? {
        val normalized = lines.map(::strip)
        return normalized.firstOrNull { it.startsWith("Area: ") }?.substringAfter("Area: ")?.trim()
            ?: normalized.firstOrNull { it.contains('⏣') }?.substringAfter('⏣')?.trim()
    }
    fun isSafari(lines: List<String>) = zone(lines)?.let {
        it.equals("Critter Safari", true) || it.equals("Safari", true) ||
            it.lowercase() in setOf("forest biome", "cavern biome", "icy biome", "haunted biome", "haunted mansion")
    } ?: lines.any { strip(it).equals("Critter Safari", true) }
    fun isHaunted(lines: List<String>) = zone(lines)?.lowercase() in setOf("haunted biome", "haunted mansion")
    fun isEntrance(lines: List<String>) = zone(lines)?.lowercase() in setOf("safari entrance", "critter safari entrance", "safari entry", "critter safari entry")
    fun isIslandName(lines: List<String>) = zone(lines)?.lowercase() in setOf("critter safari", "safari")
    fun isDialogue(text: String) = dialogue.containsMatchIn(strip(text))
    fun isHideyhoDialogue(text: String) = hideyho.matches(strip(text))
}
