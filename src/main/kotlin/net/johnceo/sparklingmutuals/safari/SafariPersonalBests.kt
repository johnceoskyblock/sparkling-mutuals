package net.johnceo.sparklingmutuals.safari

import java.util.Properties

enum class BiomePbType(val label: String) { FULL_CLEAR("Full Clear"), UNIQUE("Unique Run") }

/** Only the player's successful CAPTURE messages can improve a personal best. */
class SafariPersonalBests {
    @Volatile private var doom: Long? = null
    @Volatile private var wumpa: Long? = null
    @Volatile private var doomDuo: Long? = null
    @Volatile private var wumpaDuo: Long? = null
    private val biomes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val uniqueBiomes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    @Volatile var biomeType = BiomePbType.FULL_CLEAR
        private set
    fun selectBiomeType(type: BiomePbType) { biomeType = type }
    fun toggleBiomeType(): BiomePbType {
        biomeType = if (biomeType == BiomePbType.FULL_CLEAR) BiomePbType.UNIQUE else BiomePbType.FULL_CLEAR
        return biomeType
    }
    private val capture = Regex("^CAPTURE! You caught a (Doomspiral|Wumpa)(?![A-Za-z]).*$")
    fun time(species: String, type: BiomePbType = BiomePbType.FULL_CLEAR, duo: Boolean = false) = when (species) {
        "Doomspiral" -> if (duo) doomDuo else doom; "Wumpa" -> if (duo) wumpaDuo else wumpa; else -> (if (type == BiomePbType.UNIQUE) uniqueBiomes else biomes)[species]
    }
    fun record(raw: String, run: SafariRun, now: Long): Boolean {
        val species = capture.matchEntire(SafariRules.strip(raw))?.groupValues?.get(1) ?: return false
        if (run.endedAt != null || now < run.startedAt) return false
        val elapsed = now - run.startedAt
        var changed = false
        val duoBest = time(species, duo = true)
        if (duoBest == null || elapsed < duoBest) {
            if (species == "Doomspiral") doomDuo = elapsed else wumpaDuo = elapsed
            changed = true
        }
        val soloEligible = if (species == "Doomspiral") run.hadFourIncense
            else SafariBiome.ICY.critters.filter { it.name != "Wumpa" }.all { run.personalCount(it.name) > 0 }
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
            biome.critters.any { run.personalCount(it.name) == 0 }) return null
        run.uniqueBiomeClears.add(biome)
        return recordBiome(biome, run, now, BiomePbType.UNIQUE)
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
    fun load(properties: Properties) {
        fun read(key: String) = properties.getProperty("pb.${key}Millis")?.toLongOrNull()?.takeIf { it >= 0 }
        doom = read("solo.doomspiral"); wumpa = read("solo.wumpa")
        // Historical boss records had no solo evidence; preserve them only in the unrestricted category.
        doomDuo = read("duo.doomspiral") ?: read("doomspiral")
        wumpaDuo = read("duo.wumpa") ?: read("wumpa")
        biomeType = BiomePbType.entries.firstOrNull { it.name == properties.getProperty("pb.biomeType") } ?: BiomePbType.FULL_CLEAR
        biomes.clear(); uniqueBiomes.clear()
        SafariBiome.entries.forEach { biome ->
            read(biome.label.lowercase())?.let { biomes[biome.label] = it }
            read("unique.${biome.label.lowercase()}")?.let { uniqueBiomes[biome.label] = it }
        }
    }
    fun save(properties: Properties) {
        biomes.forEach { (name, time) -> properties.setProperty("pb.${name.lowercase()}Millis", time.toString()) }
        uniqueBiomes.forEach { (name, time) -> properties.setProperty("pb.unique.${name.lowercase()}Millis", time.toString()) }
        properties.setProperty("pb.biomeType", biomeType.name)
        doom?.let { properties.setProperty("pb.solo.doomspiralMillis", it.toString()) }
        wumpa?.let { properties.setProperty("pb.solo.wumpaMillis", it.toString()) }
        doomDuo?.let { properties.setProperty("pb.duo.doomspiralMillis", it.toString()) }
        wumpaDuo?.let { properties.setProperty("pb.duo.wumpaMillis", it.toString()) }
    }
    fun response(player: String, species: String, duo: Boolean = false): String {
        val formatted = time(species, biomeType, duo)?.let(::formatTime) ?: "Not recorded yet"
        val type = if (SafariBiome.entries.any { it.label == species }) " ${biomeType.label}" else if (duo) " Duo" else ""
        return "$player's $species$type PB: $formatted"
    }
}
