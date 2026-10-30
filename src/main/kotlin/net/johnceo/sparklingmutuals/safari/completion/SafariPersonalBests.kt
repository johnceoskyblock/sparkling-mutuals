package net.johnceo.sparklingmutuals.safari.completion

import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariRun

enum class BiomePbType(val label: String) { FULL_CLEAR("Full Clear"), UNIQUE("Unique Run") }

/** Only the player's successful CAPTURE messages can improve a personal best. */
class SafariPersonalBests {
    data class CaptureFeedback(val notice: String, val changed: Boolean)
    private data class PartyBest(val name: String, val times: MutableMap<String, Long> = linkedMapOf())
    private val partyBests = linkedMapOf<String, PartyBest>()
    private val ign = Regex("^[A-Za-z0-9_]{1,16}$")
    private val sharedBoss = Regex("^LOOT SHARE! .*\\bfrom\\s+([A-Za-z0-9_]{1,16})\\s+catching a (?:SPARKLING )?(Doomspiral|Wumpa)(?![A-Za-z]).*$")
    private fun playerKey(name: String) = name.lowercase(java.util.Locale.ROOT)
    @Synchronized fun rememberParty(names: Iterable<String>): Boolean {
        var changed = false
        names.filter(ign::matches).forEach { name ->
            if (partyBests.putIfAbsent(playerKey(name), PartyBest(name)) == null) changed = true
        }
        return changed
    }
    @Synchronized fun trackedResponse(player: String, species: String, duo: Boolean = false, localName: String? = null): String {
        // A player's own boss PB is authoritative; the observed table can predate their personal record.
        if (species in setOf("Wumpa", "Doomspiral") && player.equals(localName, true))
            return response(localName!!, species, duo)
        val record = partyBests[playerKey(player)]
        val biome = SafariBiome.entries.any { it.label == species }
        val key = if (biome) "${if (duo) "uniqueParty" else "unique"}.$species" else "${if (duo) "duo" else "solo"}.$species"
        val category = if (biome) " Unique Run${if (duo) " Party" else ""}" else if (duo) " Duo" else ""
        return "${record?.name ?: player}'s $species Observed$category PB: ${record?.times?.get(key)?.let(::formatTime) ?: "Not recorded yet"}"
    }
    /** Observed unique biomes stop at the party's final required unique capture. */
    @Synchronized fun recordObservedUnique(biome: SafariBiome, run: SafariRun, now: Long): Boolean {
        if (run.endedAt != null || now < run.startedAt || biome in run.observedUniqueClears ||
            biome.critters.any { run.count(it.name) == 0 && !run.uniqueUnavailable(it.name) }) return false
        run.observedUniqueClears.add(biome)
        var changed = false
        for (player in run.catchers(biome)) {
            if (rememberParty(listOf(player))) changed = true
            val solo = run.catchers(biome).size == 1 && biome.critters.all { run.playerCount(it.name, player) > 0 || run.uniqueUnavailable(it.name) }
            val key = "${if (solo) "unique" else "uniqueParty"}.${biome.label}"
            val times = partyBests.getValue(playerKey(player)).times
            val elapsed = now - run.startedAt
            if (times[key] == null || elapsed < times.getValue(key)) { times[key] = elapsed; changed = true }
        }
        return changed
    }
    private fun soloEligible(species: String, run: SafariRun) = if (species == "Doomspiral") run.hadFourIncense
        else SafariBiome.ICY.critters.filter { it.name != "Wumpa" }.all { run.personalCount(it.name) > 0 }
    /** Every boss capture reports elapsed time; comparisons use the record BEFORE this catch. */
    @Synchronized fun captureFeedback(raw: String, run: SafariRun, now: Long, localName: String): CaptureFeedback? {
        if (run.endedAt != null || now < run.startedAt) return null
        val text = SafariRules.strip(raw)
        val own = capture.matchEntire(text)
        val shared = if (own == null) sharedBoss.matchEntire(text) else null
        val species = own?.groupValues?.get(1) ?: shared?.groupValues?.get(2) ?: return null
        val name = if (own != null) localName else shared!!.groupValues[1]
        if (!ign.matches(name)) return null
        val elapsed = now - run.startedAt
        var changed = rememberParty(listOf(name))
        val record = partyBests.getValue(playerKey(name))
        val solo = if (own != null) soloEligible(species, run) else species == "Doomspiral" ||
            SafariBiome.ICY.critters.filter { it.name != "Wumpa" }.all { run.playerCount(it.name, name) > 0 }
        val key = "${if (solo) "solo" else "duo"}.$species"
        val previousObserved = record.times[key]
        val previous = if (own != null) time(species, duo = !solo) else previousObserved
        if (previousObserved == null || elapsed < previousObserved) {
            record.times[key] = elapsed
            changed = true
        }
        if (own != null && record(text, run, now)) changed = true
        val category = "${if (own == null) "Observed " else ""}${if (solo) "Solo" else "Duo"}"
        val gap = when {
            previous == null -> "first recorded PB"
            elapsed < previous -> "new PB, ${seconds(previous - elapsed)}s faster"
            elapsed == previous -> "matches PB"
            else -> "${seconds(elapsed - previous)}s slower than PB"
        }
        return CaptureFeedback("[SM] ${record.name} caught $species in ${formatTime(elapsed)} — $category: $gap.", changed)
    }
    private fun seconds(millis: Long) = java.lang.String.format(java.util.Locale.ROOT, "%.3f", millis / 1000.0)
    @Volatile private var doom: Long? = null
    @Volatile private var wumpa: Long? = null
    @Volatile private var doomDuo: Long? = null
    @Volatile private var wumpaDuo: Long? = null
    private val biomes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val uniqueBiomes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    /** Unique Run times for the whole party: every critter of the biome secured, teammates' catches included. */
    private val partyUniqueBiomes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    fun partyTime(biome: String) = partyUniqueBiomes[biome]
    @Volatile var biomeType = BiomePbType.FULL_CLEAR
        private set
    fun selectBiomeType(type: BiomePbType) { biomeType = type }
    fun toggleBiomeType(): BiomePbType {
        biomeType = if (biomeType == BiomePbType.FULL_CLEAR) BiomePbType.UNIQUE else BiomePbType.FULL_CLEAR
        return biomeType
    }
    private val capture = Regex("^CAPTURE! You caught a (?:SPARKLING )?(Doomspiral|Wumpa)(?![A-Za-z]).*$")
    fun time(species: String, type: BiomePbType = BiomePbType.FULL_CLEAR, duo: Boolean = false) = when (species) {
        "Doomspiral" -> if (duo) doomDuo else doom; "Wumpa" -> if (duo) wumpaDuo else wumpa; else -> (if (type == BiomePbType.UNIQUE) uniqueBiomes else biomes)[species]
    }
    fun record(raw: String, run: SafariRun, now: Long): Boolean {
        val species = capture.matchEntire(SafariRules.strip(raw))?.groupValues?.get(1) ?: return false
        if (run.endedAt != null || now < run.startedAt) return false
        val elapsed = now - run.startedAt
        var changed = false
        val duoBest = time(species, duo = true)
        if (!soloEligible(species, run) && (duoBest == null || elapsed < duoBest)) {
            if (species == "Doomspiral") doomDuo = elapsed else wumpaDuo = elapsed
            changed = true
        }
        val soloEligible = soloEligible(species, run)
        val best = time(species)
        if (soloEligible && (best == null || elapsed < best)) {
            if (species == "Doomspiral") doom = elapsed else wumpa = elapsed
            changed = true
        }
        return changed
    }
    fun newBest(raw: String, run: SafariRun, now: Long): String? {
        val species = capture.matchEntire(SafariRules.strip(raw))?.groupValues?.get(1) ?: return null
        val previousSolo = time(species)
        if (!record(raw, run, now)) return null
        val soloImproved = time(species) != previousSolo
        val label = if (soloImproved) "" else " Duo"
        return "[SM] New $species$label PB: ${formatTime(time(species, duo = !soloImproved)!!)}!"
    }
    fun recordUnique(biome: SafariBiome, run: SafariRun, now: Long): String? {
        if (run.endedAt != null || now < run.startedAt || biome in run.uniqueBiomeClears ||
            biome.critters.any { run.personalCount(it.name) == 0 && !run.uniqueUnavailable(it.name) }) return null
        run.uniqueBiomeClears.add(biome)
        return recordBiome(biome, run, now, BiomePbType.UNIQUE)
    }
    /**
     * The party's Unique Run PB: from run start until every critter of the biome is secured by anyone (loot shares
     * count), or proven absent. Checked after each catch and after each scan, since a scan proving Snoozle or Rockmite
     * absent can be what finishes the biome. Teammates' catches count in whatever order they come relative to your own
     * travel; catches made before this client entered never reach it, so a biome worked before you arrived can't
     * complete here.
     */
    fun recordPartyUnique(biome: SafariBiome, run: SafariRun, now: Long): String? {
        if (run.endedAt != null || now < run.startedAt || biome in run.partyUniqueClears ||
            biome.critters.any { run.count(it.name) == 0 && !run.uniqueUnavailable(it.name) }) return null
        run.partyUniqueClears.add(biome)
        val elapsed = now - run.startedAt
        val best = partyUniqueBiomes[biome.label]
        if (best != null && elapsed >= best) return null
        partyUniqueBiomes[biome.label] = elapsed
        return "[SM] New ${biome.label} ${BiomePbType.UNIQUE.label} Party PB: ${formatTime(elapsed)}!"
    }
    fun recordBiome(biome: SafariBiome, run: SafariRun, now: Long, type: BiomePbType = BiomePbType.FULL_CLEAR): String? {
        if (run.endedAt != null || now < run.startedAt) return null
        val elapsed = now - run.startedAt
        val best = time(biome.label, type)
        if (best != null && elapsed >= best) return null
        (if (type == BiomePbType.UNIQUE) uniqueBiomes else biomes)[biome.label] = elapsed
        return "[SM] New ${biome.label} ${type.label} PB: ${formatTime(elapsed)}!"
    }
    private fun formatTime(millis: Long) = java.lang.String.format(java.util.Locale.ROOT, "%d:%02d.%03d", millis / 60000, millis / 1000 % 60, millis % 1000)
    @Synchronized fun load(properties: Properties) {
        partyBests.clear()
        properties.stringPropertyNames().filter { it.startsWith("pb.party.") && it.endsWith(".name") }.forEach { key ->
            val name = properties.getProperty(key)
            if (ign.matches(name)) {
                val prefix = "pb.party.${playerKey(name)}."
                fun readParty(species: String) = properties.getProperty("${prefix}${species}Millis")?.toLongOrNull()?.takeIf { it >= 0 }
                val record = PartyBest(name)
                // Older observations have no capture attribution: Doom is assumed solo; Wumpa stays assisted.
                readParty("doomspiral")?.let { record.times["solo.Doomspiral"] = it }
                readParty("wumpa")?.let { record.times["duo.Wumpa"] = it }
                properties.stringPropertyNames().filter { it.startsWith("${prefix}times.") }.forEach { timeKey ->
                    properties.getProperty(timeKey)?.toLongOrNull()?.takeIf { it >= 0 }?.let {
                        record.times[timeKey.removePrefix("${prefix}times.").removeSuffix("Millis")] = it
                    }
                }
                partyBests[playerKey(name)] = record
            }
        }
        fun read(key: String) = properties.getProperty("pb.${key}Millis")?.toLongOrNull()?.takeIf { it >= 0 }
        doom = read("solo.doomspiral"); wumpa = read("solo.wumpa")
        // Historical boss records had no solo evidence; preserve them only in the unrestricted category.
        doomDuo = read("duo.doomspiral") ?: read("doomspiral")
        wumpaDuo = read("duo.wumpa") ?: read("wumpa")
        biomeType = BiomePbType.entries.firstOrNull { it.name == properties.getProperty("pb.biomeType") } ?: BiomePbType.FULL_CLEAR
        biomes.clear(); uniqueBiomes.clear(); partyUniqueBiomes.clear()
        SafariBiome.entries.forEach { biome ->
            read(biome.label.lowercase())?.let { biomes[biome.label] = it }
            read("unique.${biome.label.lowercase()}")?.let { uniqueBiomes[biome.label] = it }
            read("uniqueParty.${biome.label.lowercase()}")?.let { partyUniqueBiomes[biome.label] = it }
        }
    }
    @Synchronized fun save(properties: Properties) {
        partyBests.forEach { (key, value) ->
            val prefix = "pb.party.$key."
            properties.setProperty("${prefix}name", value.name)
            value.times.forEach { (category, time) -> properties.setProperty("${prefix}times.${category}Millis", time.toString()) }
        }
        biomes.forEach { (name, time) -> properties.setProperty("pb.${name.lowercase()}Millis", time.toString()) }
        uniqueBiomes.forEach { (name, time) -> properties.setProperty("pb.unique.${name.lowercase()}Millis", time.toString()) }
        partyUniqueBiomes.forEach { (name, time) -> properties.setProperty("pb.uniqueParty.${name.lowercase()}Millis", time.toString()) }
        properties.setProperty("pb.biomeType", biomeType.name)
        doom?.let { properties.setProperty("pb.solo.doomspiralMillis", it.toString()) }
        wumpa?.let { properties.setProperty("pb.solo.wumpaMillis", it.toString()) }
        doomDuo?.let { properties.setProperty("pb.duo.doomspiralMillis", it.toString()) }
        wumpaDuo?.let { properties.setProperty("pb.duo.wumpaMillis", it.toString()) }
    }
    fun response(player: String, species: String, duo: Boolean = false): String {
        val formatted = time(species, biomeType, duo)?.let(::formatTime) ?: "Not recorded yet"
        val biome = SafariBiome.entries.any { it.label == species }
        val type = if (biome) " ${biomeType.label}" else if (duo) " Duo" else ""
        // In Unique mode the party's time for the biome rides along when there is one.
        val party = if (biome && biomeType == BiomePbType.UNIQUE) partyUniqueBiomes[species]?.let { " | Party PB: ${formatTime(it)}" } ?: "" else ""
        return "$player's $species$type PB: $formatted$party"
    }
}
