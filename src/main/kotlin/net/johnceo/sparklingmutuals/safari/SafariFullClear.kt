package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig

enum class SafariMode { UNIQUE, FULL_CLEAR, SPARKLING }

data class BiomeClearEvidence(val observed: Boolean = false, val nearbyCritters: Int = 0,
    val moundsCleared: Boolean = false, val wallsCleared: Boolean = false,
    val nestsChecked: Boolean = false, val nearbyMacaws: Int = 0)

/** Command-owned presets change presentation, never the per-run capture ledger. */
object SafariFullClear {
    private val regularEsp = setOf("Driftling", "Chuckwalla", "Foxtrot", "Treefrog", "Woodchucker", "Fluffling", "Hideonfloor",
        "Tepid", "Shuddersquid", "Billygoat", "Mantis Shrimp", "Nozzlenose", "Wumpa", "Bloodbat", "Duplico",
        "Litterbug", "Solsnatcher", "Hideonwall", "Hideyho", "Doomspiral")
    private val minimums = mapOf(
        "Foxtrot" to 6, "Bluebird" to 0, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
        "Fluffling" to 1, "Hideonfloor" to 1, "Parakeet" to 0, "Macaw" to 0,
        "Cavernfish" to 4, "Flitter" to 6, "Shyworm" to 4, "Driftling" to 3, "Chuckwalla" to 2,
        "Rockmite" to 0, "Scrappy" to 3, "Snoozle" to 0, "Gemzie" to 3,
        "Strongarm" to 4, "Tepid" to 6, "Polaris" to 2, "Shuddersquid" to 3, "Billygoat" to 2,
        "Mantis Shrimp" to 3, "Nozzlenose" to 2, "Troodon" to 3, "Wumpa" to 1,
        "Areita" to 3, "Bloodbat" to 3, "Duplico" to 2, "Gazer" to 4, "Litterbug" to 4,
        "Solsnatcher" to 4, "Gimmiegold" to 3, "Hideonwall" to 2, "Hideyho" to 1, "Doomspiral" to 1)
    fun minimum(species: String) = minimums.getValue(species)
    fun eligible(run: SafariRun, biome: SafariBiome, evidence: BiomeClearEvidence): Boolean {
        val macawException = biome == SafariBiome.FOREST && evidence.nearbyMacaws > 0 &&
            evidence.nearbyCritters == evidence.nearbyMacaws
        if (run.endedAt != null || !evidence.observed || evidence.nearbyCritters != 0 && !macawException) return false
        val remaining = if (macawException) setOf("Macaw") else emptySet()
        return biome.critters.all { run.captureComplete(it.name, personalOnly = true, remaining, evidence) }
    }
    fun recordClear(run: SafariRun, biome: SafariBiome, evidence: BiomeClearEvidence,
        bests: SafariPersonalBests, now: Long): String? {
        if (biome in run.biomeClears || !eligible(run, biome, evidence)) return null
        run.biomeClears.add(biome)
        return bests.recordBiome(biome, run, now)
    }
    val mode: SafariMode get() = when {
        ConfigManager.sparklingMode -> SafariMode.SPARKLING
        ConfigManager.fullClearMode -> SafariMode.FULL_CLEAR
        else -> SafariMode.UNIQUE
    }
    fun modeMessage() = when (mode) {
        SafariMode.SPARKLING -> "[SM] Sparkling mode on."
        SafariMode.FULL_CLEAR -> "[SM] Full clear mode on."
        SafariMode.UNIQUE -> "[SM] Unique run mode on."
    }
    fun modeLines() = listOf(modeMessage()) + if (mode == SafariMode.SPARKLING)
        listOf("Make sure you have an API key set up for sparkling mode to work correctly.") else emptyList()
    fun toggle(): Boolean {
        setEnabled(!ConfigManager.fullClearMode)
        return ConfigManager.fullClearMode
    }
    fun setEnabled(enabled: Boolean) = select(if (enabled) SafariMode.FULL_CLEAR else SafariMode.UNIQUE)
    fun select(selected: SafariMode) {
        if (mode == selected) return
        ConfigManager.sparklingMode = selected == SafariMode.SPARKLING
        ConfigManager.fullClearMode = selected == SafariMode.FULL_CLEAR
        syncMode()
        apply(selected)
        SafariSparklingMode.requestRefresh()
        ConfigManager.save()
    }
    fun syncMode() {
        if (ConfigManager.sparklingMode) ConfigManager.fullClearMode = false
        ConfigManager.timesaveOnly = mode == SafariMode.UNIQUE
        ConfigManager.personalBests.selectBiomeType(if (ConfigManager.fullClearMode) BiomePbType.FULL_CLEAR else BiomePbType.UNIQUE)
    }
    private fun apply(selected: SafariMode) {
        val allEsp = selected != SafariMode.UNIQUE
        ConfigManager.catchCountPanel = selected == SafariMode.FULL_CLEAR
        ConfigManager.showMoundStats = selected == SafariMode.FULL_CLEAR
        ConfigManager.countUniqueOnly = selected != SafariMode.FULL_CLEAR
        if (allEsp) {
            ConfigManager.showBeeNests = true; ConfigManager.showMoundCount = true; ConfigManager.showSnooperWalls = true
        }
        SafariEspConfig.groups.values.forEach { it.enabled = true }
        SafariEspConfig.mobs.forEach { (species, setting) -> setting.enabled = allEsp || species in regularEsp }
        SafariEspConfig.rockmiteMoundEnabled = true
        ConfigManager.highlightSnooperWalls = true
    }
}
