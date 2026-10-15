package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

/** Roster and completion quotas from CritterMod v0.9.0, cdfcb8effb4b3158df50f0c0960c11e536931840 (MIT). */
enum class SafariBiome(val label: String, val color: Int) {
    FOREST("Forest", 0xFF55FF55.toInt()), CAVERN("Cavern", 0xFFFFAA00.toInt()),
    ICY("Icy", 0xFF55FFFF.toInt()), HAUNTED("Haunted", 0xFFAA00AA.toInt());
    val critters get() = SafariRoster.inBiome(this)
    companion object { fun mapped(id: Int?) = entries.getOrNull((id ?: 0) - 1) }
}
data class SafariCritter(val name: String, val biome: SafariBiome, val quota: Int = 1) {
    fun required(unique: Boolean, fullClear: Boolean = false) = if (fullClear) SafariFullClear.minimum(name) else if (unique) 1 else quota
    val color get() = SafariRoster.color(name)
}
object SafariRoster {
    private val quotas = mapOf("Gemzie" to 3, "Troodon" to 3, "Gazer" to 4, "Scrappy" to 3)
    private val colors = listOf(
        0xFF55FF55.toInt() to "Bluebird,Honeybug,Treefrog,Woodchucker,Driftling,Polaris,Shuddersquid,Areita,Bloodbat,Duplico,Gazer,Litterbug,Solsnatcher",
        0xFF5555FF.toInt() to "Fluffling,Hideonfloor,Parakeet,Chuckwalla,Rockmite,Scrappy,Snoozle,Billygoat,Mantis Shrimp,Nozzlenose,Troodon,Gimmiegold,Hideonwall,Hideyho",
        0xFFAA00AA.toInt() to "Gemzie", 0xFFFFAA00.toInt() to "Macaw,Wumpa,Doomspiral"
    ).flatMap { (color, names) -> names.split(',').map { it to color } }.toMap()
    fun color(name: String) = colors[name] ?: 0xFFFFFFFF.toInt()
    val all = listOf(
        "Foxtrot,Bluebird,Honeybug,Treefrog,Woodchucker,Fluffling,Hideonfloor,Parakeet,Macaw",
        "Cavernfish,Flitter,Shyworm,Driftling,Chuckwalla,Rockmite,Scrappy,Snoozle,Gemzie",
        "Strongarm,Tepid,Polaris,Shuddersquid,Billygoat,Mantis Shrimp,Nozzlenose,Troodon,Wumpa",
        "Areita,Bloodbat,Duplico,Gazer,Litterbug,Solsnatcher,Gimmiegold,Hideonwall,Hideyho,Doomspiral"
    ).flatMapIndexed { index, names -> names.split(',').map { SafariCritter(it, SafariBiome.entries[index], quotas[it] ?: 1) } }
    private val byName = all.associateBy { it.name.lowercase() }
    private val byBiome = all.groupBy { it.biome }
    private val patterns = all.sortedByDescending { it.name.length }.map {
        it to Regex("(?<![A-Za-z])${Regex.escape(it.name)}(?![A-Za-z])", RegexOption.IGNORE_CASE)
    }
    fun named(name: String) = byName[name.lowercase()]
    fun inBiome(biome: SafariBiome) = byBiome.getValue(biome)
    fun findIn(text: String) = patterns.firstOrNull { it.second.containsMatchIn(text) }?.first
}
