package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig

data class BiomeClearEvidence(val observed: Boolean = false, val nearbyCritters: Int = 0,
    val moundsCleared: Boolean = false, val wallsCleared: Boolean = false,
    val nestsChecked: Boolean = false, val floorDropsCleared: Boolean = false)

/** Missing entities outside loaded chunks never complete a previously observed site. */
class ClearSiteSurvey {
    private val sites = mutableMapOf<Triple<Int, Int, Int>, Boolean>()
    val allCleared get() = sites.isNotEmpty() && sites.values.all { it }
    fun scan(present: Set<Triple<Int, Int, Int>>, observable: (Triple<Int, Int, Int>) -> Boolean) {
        present.forEach { sites[it] = false }
        sites.keys.filter { it !in present && observable(it) }.forEach { sites[it] = true }
    }
}

/** Command-owned presets change presentation, never the per-run capture ledger. */
object SafariFullClear {
    private val regularEsp = setOf("Driftling", "Foxtrot", "Treefrog", "Woodchucker", "Fluffling", "Hideonfloor",
        "Tepid", "Shuddersquid", "Billygoat", "Mantis Shrimp", "Nozzlenose", "Wumpa", "Bloodbat", "Duplico",
        "Litterbug", "Solsnatcher", "Hideonwall", "Hideyho", "Doomspiral")
    private val minimums = mapOf(
        "Foxtrot" to 6, "Bluebird" to 0, "Honeybug" to 3, "Treefrog" to 3, "Woodchucker" to 3,
        "Fluffling" to 1, "Hideonfloor" to 1, "Parakeet" to 0, "Macaw" to 0,
        "Cavernfish" to 4, "Flitter" to 6, "Shyworm" to 4, "Driftling" to 3, "Chuckwalla" to 2,
        "Rockmite" to 0, "Scrappy" to 3, "Snoozle" to 0, "Gemzie" to 3,
        "Strongarm" to 6, "Tepid" to 6, "Polaris" to 2, "Shuddersquid" to 3, "Billygoat" to 2,
        "Mantis Shrimp" to 3, "Nozzlenose" to 2, "Troodon" to 3, "Wumpa" to 1,
        "Areita" to 3, "Bloodbat" to 3, "Duplico" to 2, "Gazer" to 4, "Litterbug" to 4,
        "Solsnatcher" to 4, "Gimmiegold" to 3, "Hideonwall" to 2, "Hideyho" to 1, "Doomspiral" to 1)
    const val MOUND_MINIMUM = 10
    fun minimum(species: String) = minimums.getValue(species)
    fun eligible(run: SafariRun, biome: SafariBiome, evidence: BiomeClearEvidence): Boolean {
        if (run.endedAt != null || !evidence.observed || evidence.nearbyCritters != 0) return false
        if (biome.critters.any { critter ->
            val quota = minimum(critter.name)
            run.personalCount(critter.name) < if (quota == 0) run.observedCount(critter.name) else quota
        }) return false
        return when (biome) {
            SafariBiome.CAVERN -> run.brokenMounds >= MOUND_MINIMUM && evidence.moundsCleared && evidence.wallsCleared
            SafariBiome.FOREST -> evidence.nestsChecked && (evidence.floorDropsCleared ||
                listOf("Bluebird", "Parakeet", "Macaw").sumOf(run::personalCount) >= 9)
            else -> true
        }
    }
    fun toggle(): Boolean {
        ConfigManager.fullClearMode = !ConfigManager.fullClearMode
        apply(ConfigManager.fullClearMode)
        ConfigManager.save()
        return ConfigManager.fullClearMode
    }
    private fun apply(enabled: Boolean) {
        ConfigManager.catchCountPanel = enabled
        ConfigManager.showMoundStats = enabled
        ConfigManager.countUniqueOnly = !enabled
        if (enabled) {
            ConfigManager.showBeeNests = true; ConfigManager.showMoundCount = true; ConfigManager.showSnooperWalls = true
        }
        SafariEspConfig.groups.values.forEach { it.enabled = true }
        SafariEspConfig.mobs.forEach { (species, setting) -> setting.enabled = enabled || species in regularEsp }
        SafariEspConfig.rockmiteMoundEnabled = true
    }
}
