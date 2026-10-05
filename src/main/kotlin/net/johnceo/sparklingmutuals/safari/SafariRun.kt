package net.johnceo.sparklingmutuals.safari

/** Roster and completion quotas from CritterMod v0.9.0, cdfcb8effb4b3158df50f0c0960c11e536931840 (MIT). */
enum class SafariBiome(val label: String, val color: Int) {
    FOREST("Forest", 0xFF55FF55.toInt()), CAVERN("Cavern", 0xFFFFAA00.toInt()),
    ICY("Icy", 0xFF55FFFF.toInt()), HAUNTED("Haunted", 0xFFAA00AA.toInt());
    val critters get() = SafariRoster.inBiome(this)
    companion object { fun mapped(id: Int?) = entries.getOrNull((id ?: 0) - 1) }
}
data class SafariCritter(val name: String, val biome: SafariBiome, val quota: Int = 1) {
    fun required(unique: Boolean) = if (unique) 1 else quota
    val color get() = SafariRoster.color(name)
}
object SafariRoster {
    private val quotas = mapOf("Gemzie" to 3, "Troodon" to 3, "Gazer" to 4)
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
data class SafariCatch(val critter: SafariCritter) {
    companion object {
        private val catcher = Regex("\\bfrom\\s+(\\w{1,16})\\s+(?:catching|finding)\\b")
        fun parse(raw: String): SafariCatch? {
            val text = SafariRules.strip(raw)
            val own = text.startsWith("CAPTURE!")
            if (!own && !text.startsWith("LOOT SHARE!")) return null
            val species = SafariRoster.findIn(text) ?: return null
            return if (own || catcher.containsMatchIn(text)) SafariCatch(species) else null
        }
    }
}
class SafariRun(val startedAt: Long) {
    var endedAt: Long? = null
    var lastBiome: SafariBiome? = null
    private val counts = mutableMapOf<String, Int>()
    fun record(catch: SafariCatch) { counts.merge(catch.critter.name, 1, Int::plus) }
    fun count(name: String) = counts[name] ?: 0
    fun elapsed(now: Long) = ((endedAt ?: now) - startedAt).coerceAtLeast(0)
    fun complete(critter: SafariCritter, unique: Boolean) = count(critter.name) >= critter.required(unique)
    fun progress(critters: List<SafariCritter>, unique: Boolean) = critters.count { complete(it, unique) }
}
class SafariLedger {
    private var pendingArrival: Long? = null
    var current: SafariRun? = null
        private set
    var last: SafariRun? = null
        private set
    val displayed get() = current ?: last
    fun arrive(now: Long) { if (current == null) current = SafariRun(pendingArrival ?: now); pendingArrival = null }
    fun worldChanged(now: Long) { leave(now); pendingArrival = now }
    fun enter(now: Long) { leave(now); arrive(now) }
    // The boat is detected before the server's entry banner. Confirm that arrival without erasing it.
    fun confirmEntry(now: Long) = arrive(now)
    fun update(location: SafariLocation, biome: SafariBiome?, now: Long) {
        when (location) {
            SafariLocation.INSIDE -> { arrive(now); if (biome != null) current!!.lastBiome = biome }
            SafariLocation.ENTRANCE, SafariLocation.OUTSIDE -> leave(now)
            SafariLocation.UNKNOWN -> Unit
        }
    }
    fun leave(now: Long) { current?.let { it.endedAt = now; last = it }; current = null; pendingArrival = null }
}
/** Hypixel's entry notice may be inside one multiline component, as documented by v0.9.0. */
object SafariMessages {
    private val separators = Regex("\\r?\\n|\\\\n")
    private val playerChat = Regex("^(?:(?:Party|Guild|Officer|Co-op|Coop)\\s*>|(?:From|To)\\s+|<\\w{1,16}>|(?:\\[[^]]+]\\s*)*\\w{1,16}(?:\\s*\\[[^]]+])*:)", RegexOption.IGNORE_CASE)
    private val entry = Regex("^(?:\\[[^]]+]\\s*)*(\\w{1,16}) entered Critter Safari!$")
    fun lines(raw: String): List<String> {
        if (playerChat.containsMatchIn(SafariRules.strip(raw))) return emptyList()
        return raw.split(separators).map(SafariRules::strip).filter(String::isNotEmpty)
    }
    fun enteredBy(line: String, player: String) = entry.matchEntire(line)?.groupValues?.get(1).equals(player, true)
}
object SafariVisibility {
    fun visible(where: Int, inSafari: Boolean, entrance: Boolean) = where == 2 || inSafari || (where == 1 && entrance)
}
