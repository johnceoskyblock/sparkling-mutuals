package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.SafariBiome
import java.util.Properties

internal object HotspotPreferences {
    private const val KEY = "hotspotBiomePreferences"
    val defaults = listOf(SafariBiome.ICY, SafariBiome.FOREST, SafariBiome.HAUNTED, SafariBiome.CAVERN)
    var order: List<SafariBiome> = defaults
        private set

    fun parse(values: List<String>): List<SafariBiome>? {
        val biomes = values.mapNotNull { value -> SafariBiome.entries.firstOrNull { it.name == value } }
        return biomes.takeIf { values.size == defaults.size && it.toSet() == defaults.toSet() }
    }

    fun load(properties: Properties) {
        order = properties.getProperty(KEY)?.split(',')?.let(::parse) ?: defaults
    }

    fun save(properties: Properties) {
        properties.setProperty(KEY, order.joinToString(",") { it.name })
    }

    fun update(next: List<SafariBiome>) {
        require(next.size == defaults.size && next.toSet() == defaults.toSet())
        if (next == order) return
        val previous = order
        order = next.toList()
        try {
            ConfigManager.saveSettings()
        } catch (error: java.io.IOException) {
            order = previous
            throw error
        }
    }
}
