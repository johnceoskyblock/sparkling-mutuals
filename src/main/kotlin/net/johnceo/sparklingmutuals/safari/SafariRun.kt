package net.johnceo.sparklingmutuals.safari

/** Roster and completion quotas from CritterMod v0.9.0, cdfcb8effb4b3158df50f0c0960c11e536931840 (MIT). */
enum class SafariBiome(val label: String) {
    FOREST("Forest"), CAVERN("Cavern"), ICY("Icy"), HAUNTED("Haunted");
    val critters get() = SafariRoster.inBiome(this)
    companion object { fun mapped(id: Int?) = entries.getOrNull((id ?: 0) - 1) }
}
data class SafariCritter(val name: String, val biome: SafariBiome, val quota: Int = 1) {
    fun required(unique: Boolean) = if (unique) 1 else quota
}
object SafariRoster {
    private val quotas = mapOf("Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)
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
data class SafariCatch(val critter: SafariCritter, val catcher: String? = null) {
    companion object {
        private val catcher = Regex("\\bfrom\\s+(\\w{1,16})\\s+(?:catching|finding)\\b")
        fun parse(raw: String): SafariCatch? {
            val text = SafariRules.strip(raw)
            val own = text.startsWith("CAPTURE!")
            if (!own && !text.startsWith("LOOT SHARE!")) return null
            val species = SafariRoster.findIn(text) ?: return null
            return if (own) SafariCatch(species) else catcher.find(text)?.let { SafariCatch(species, it.groupValues[1]) }
        }
    }
}
class SafariRun(val startedAt: Long) {
    var endedAt: Long? = null
    private val own = mutableMapOf<String, Int>()
    private val shared = mutableMapOf<String, Int>()
    fun record(catch: SafariCatch) {
        val counts = if (catch.catcher == null) own else shared
        counts.merge(catch.critter.name, 1, Int::plus)
    }
    fun own(name: String) = own[name] ?: 0
    fun party(name: String) = own(name) + (shared[name] ?: 0)
    fun complete(critter: SafariCritter, unique: Boolean) = party(critter.name) >= critter.required(unique)
    fun progress(critters: List<SafariCritter>, unique: Boolean) = critters.count { complete(it, unique) }
    fun ownProgress(critters: List<SafariCritter>) = critters.count { own(it.name) > 0 }
}
class SafariLedger {
    var current: SafariRun? = null
        private set
    var last: SafariRun? = null
        private set
    fun arrive(now: Long) { if (current == null) current = SafariRun(now) }
    fun enter(now: Long) { leave(now); arrive(now) }
    fun leave(now: Long) { current?.let { it.endedAt = now; last = it }; current = null }
}
object SafariVisibility {
    fun visible(where: Int, inSafari: Boolean, entrance: Boolean) = where == 2 || inSafari || (where == 1 && entrance)
}
