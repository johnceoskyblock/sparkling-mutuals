package net.johnceo.sparklingmutuals.safari

/** Species and exact Sparkling-prefix matching adapted from CritterMod v0.9.0 (MIT). */
object SafariRules {
    const val RADIUS = 80.0
    private val species = ("Foxtrot,Bluebird,Honeybug,Treefrog,Woodchucker,Fluffling,Hideonfloor,Parakeet,Macaw," +
        "Cavernfish,Flitter,Shyworm,Driftling,Chuckwalla,Rockmite,Scrappy,Snoozle,Gemzie," +
        "Strongarm,Tepid,Polaris,Shuddersquid,Billygoat,Mantis Shrimp,Nozzlenose,Troodon,Wumpa," +
        "Areita,Bloodbat,Duplico,Gazer,Litterbug,Solsnatcher,Gimmiegold,Hideonwall,Hideyho,Doomspiral")
        .split(',').associateBy(String::lowercase)

    fun strip(text: String) = text.replace(Regex("§."), "").replace(Regex("[\\p{Cf}\\p{Co}]"), "").trim()
    fun withinRange(distanceSquared: Double) = distanceSquared >= 0 && distanceSquared <= RADIUS * RADIUS
    fun sparklingSpecies(text: String): String? {
        val name = strip(text)
        if (!name.startsWith("Sparkling ", ignoreCase = true)) return null
        return species[name.substring(10).trim().lowercase()]
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
    fun isHideyhoDialogue(text: String) = Regex("^\\[NPC] Hideyho:.*$", RegexOption.IGNORE_CASE).matches(strip(text))
}
