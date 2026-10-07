package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig

/** Per-run pickups survive item consumption; a manual choice lasts only for this biome visit. */
class FloorDropState {
    private val gems = mutableSetOf<String>()
    private val gemNames = setOf("Purple Gem", "Lime Gem", "Orange Gem")
    private var pickaxes = 0
    private var inventoryPickaxes = 0
    private var biome: SafariBiome? = null
    private var override: Boolean? = null
    fun visit(biome: SafariBiome?) {
        if (this.biome != biome) { override = null; this.biome = biome }
    }
    fun record(text: String, biome: SafariBiome?) {
        if (!text.startsWith("FLOOR DROP!")) return
        if (biome == SafariBiome.CAVERN) gems.addAll(gemNames.filter(text::contains))
        if (biome == SafariBiome.ICY && Regex("\\bIcebreaker\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) pickaxes++
    }
    fun inventory(stacks: List<Pair<String, Int>>) {
        stacks.filter { it.second > 0 }.forEach { (name, _) -> SafariRules.strip(name).takeIf { it in gemNames }?.let(gems::add) }
        inventoryPickaxes = maxOf(inventoryPickaxes, stacks.filter { SafariRules.strip(it.first).equals("Icebreaker", true) }.sumOf { it.second.coerceAtLeast(0) })
    }
    fun force(enabled: Boolean) { if (biome in setOf(SafariBiome.CAVERN, SafariBiome.ICY)) override = enabled }
    fun enabled(biome: SafariBiome?, fullClear: Boolean, configured: Boolean): Boolean {
        if (biome == this.biome && override != null) return override!!
        return when (biome) {
            SafariBiome.CAVERN -> gems.size < 3
            SafariBiome.ICY -> fullClear && maxOf(pickaxes, inventoryPickaxes) < 2
            else -> configured
        }
    }
    fun reset() { gems.clear(); pickaxes = 0; inventoryPickaxes = 0; biome = null; override = null }
}

object SafariFloorDrops {
    val state = FloorDropState()
    fun enabled(biome: SafariBiome? = SafariAssist.biome): Boolean {
        state.visit(SafariAssist.biome)
        return state.enabled(biome, ConfigManager.fullClearMode, SafariEspConfig.groups.getValue("floor").enabled)
    }
}
