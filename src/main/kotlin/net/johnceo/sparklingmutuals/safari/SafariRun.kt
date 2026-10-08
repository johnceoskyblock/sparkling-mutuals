package net.johnceo.sparklingmutuals.safari

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
data class SafariCatch(val critter: SafariCritter, val personal: Boolean = true) {
    companion object {
        private val catcher = Regex("\\bfrom\\s+(\\w{1,16})\\s+(?:catching|finding)\\b")
        fun parse(raw: String): SafariCatch? {
            val text = SafariRules.strip(raw)
            val own = text.startsWith("CAPTURE!")
            if (!own && !text.startsWith("LOOT SHARE!")) return null
            val species = SafariRoster.findIn(text) ?: return null
            return if (own || catcher.containsMatchIn(text)) SafariCatch(species,
                own && Regex("^CAPTURE! You (?:caught|found)\\b").containsMatchIn(text)) else null
        }
    }
}
class SafariRun(val startedAt: Long) {
    var brokenMounds = 0
        private set
    var rockmiteMounds = 0
        private set
    var endedAt: Long? = null
    var lastBiome: SafariBiome? = null
    private val counts = mutableMapOf<String, Int>()
    private val personal = mutableMapOf<String, Int>()
    private val visited = mutableSetOf<SafariBiome>()
    private val inheritedBiomes = mutableSetOf<SafariBiome>()
    fun visitBiome(biome: SafariBiome) {
        lastBiome = biome
        if (visited.add(biome) && biome.critters.none { personalCount(it.name) > 0 } &&
            biome.critters.any { count(it.name) > 0 }) inheritedBiomes.add(biome)
    }
    val partyNestsHandled get() = SafariBiome.FOREST in inheritedBiomes && count("Honeybug") > 0
    private val sightings = mutableSetOf<String>()
    private val observedCritters = mutableMapOf<String, MutableSet<Int>>()
    val birds = SafariBirdLedger()
    private val nearby = mutableMapOf<SafariBiome, Set<String>>()
    private var wallsChecked = false
    private var nestsChecked = false
    private val birdSpecies = setOf("Bluebird", "Parakeet", "Macaw")
    private var nearbyMacaws = 0
    val birdFoodsComplete get() = birds.foodsComplete
    fun recordBirdFood(raw: String, biome: SafariBiome? = SafariBiome.FOREST) = birds.pickup(raw, biome)
    fun updateCaptureEvidence(biome: SafariBiome, speciesInRange: Set<String>, allWallsChecked: Boolean, allNestsChecked: Boolean = false,
        macawsInRange: Int = if ("Macaw" in speciesInRange) 1 else 0) {
        nearby[biome] = speciesInRange
        wallsChecked = allWallsChecked
        nestsChecked = allNestsChecked
        if (biome == SafariBiome.FOREST) nearbyMacaws = macawsInRange
    }
    fun hasCaptureEvidence(biome: SafariBiome) = biome in nearby
    fun birdCount(personalOnly: Boolean = false) = birdSpecies.sumOf { if (personalOnly) personalCount(it) else count(it) }
    private fun birdComplete(species: String, speciesInRange: Set<String>?, personalOnly: Boolean = false, macawsInRange: Int = nearbyMacaws) =
        speciesInRange != null && birds.sufficient(birdCount(personalOnly),
            if (personalOnly) personalCount("Macaw") else count("Macaw"), macawsInRange) &&
            (species != "Macaw" || !birds.hasMacaws(count("Macaw"), macawsInRange) ||
                (if (personalOnly) personalCount(species) else count(species)) > 0) &&
            (species !in speciesInRange || species == "Macaw" && (if (personalOnly) personalCount(species) else count(species)) >= 1)
    fun birdsComplete(speciesInRange: Set<String>?, personalOnly: Boolean = false) =
        birdSpecies.all { birdComplete(it, speciesInRange, personalOnly) }
    fun captureComplete(species: String, personalOnly: Boolean = false,
        speciesInRange: Set<String>? = nearby[SafariRoster.named(species)!!.biome], structures: BiomeClearEvidence? = null): Boolean {
        val remaining = speciesInRange ?: return false
        val captured = if (personalOnly) personalCount(species) else count(species)
        if (captured < SafariFullClear.minimum(species)) return false
        if (!personalOnly && SafariRoster.named(species)!!.biome in inheritedBiomes) return species !in remaining
        if (species in birdSpecies) return birdComplete(species, remaining, personalOnly, structures?.nearbyMacaws ?: nearbyMacaws)
        if (species in remaining) return false
        return when (species) {
            "Honeybug" -> structures?.nestsChecked ?: nestsChecked
            "Snoozle" -> structures?.wallsCleared ?: wallsChecked
            "Rockmite" -> (structures?.moundsCleared ?: moundsComplete(remaining)) &&
                captured >= rockmiteMounds && "Rockmite Mound" !in remaining
            else -> true
        }
    }
    val biomeClears = mutableSetOf<SafariBiome>()
    val uniqueBiomeClears = mutableSetOf<SafariBiome>()
    val moundSurvey = MoundSurvey()
    val allMoundsBroken get() = moundSurvey.allBroken
    fun moundsComplete(remaining: Set<String>? = nearby[SafariBiome.CAVERN]) = remaining != null &&
        allMoundsBroken && moundSurvey.remaining == 0 && "Rockmite Mound" !in remaining
    fun encountered(name: String) = count(name) > 0 || name in sightings || name == "Rockmite" && rockmiteMounds > 0
    fun observe(entity: EspEntity) {
        if (entity.type == "silverfish" || entity.type == "sniffer") SafariEspRules.identify(entity)?.let { sightings.add(it.name) }
    }
    fun observeCritter(id: Int, species: String) { observedCritters.getOrPut(species) { mutableSetOf() }.add(id); sightings.add(species) }
    fun observedCount(species: String) = observedCritters[species]?.size ?: 0
    fun record(catch: SafariCatch) {
        counts.merge(catch.critter.name, 1, Int::plus)
        if (catch.personal) personal.merge(catch.critter.name, 1, Int::plus)
    }
    fun recordMound(raw: String): Boolean {
        val found = SafariMounds.outcome(raw) ?: return false
        brokenMounds++
        if (found) rockmiteMounds++
        return true
    }
    fun count(name: String) = counts[name] ?: 0
    fun personalCount(name: String) = personal[name] ?: 0
    fun elapsed(now: Long) = ((endedAt ?: now) - startedAt).coerceAtLeast(0)
    fun complete(critter: SafariCritter, unique: Boolean, fullClear: Boolean = false) = count(critter.name) >= critter.required(unique, fullClear)
    fun progress(critters: List<SafariCritter>, unique: Boolean, fullClear: Boolean = false) = critters.count { complete(it, unique, fullClear) }
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
            SafariLocation.INSIDE -> { arrive(now); if (biome != null) current!!.visitBiome(biome) }
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
    fun biomePanels(where: Int, inSafari: Boolean, activeRun: Boolean, biome: SafariBiome?) =
        visible(where, inSafari, false) && inSafari && activeRun && biome != null
}
