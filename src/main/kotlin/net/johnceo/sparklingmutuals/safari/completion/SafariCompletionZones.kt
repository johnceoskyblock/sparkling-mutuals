package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.SafariBiome

object SafariCompletionZones {
    fun forBiome(biome: SafariBiome) = when (biome) {
        SafariBiome.FOREST -> ForestCompletionZones.zones
        SafariBiome.CAVERN -> CavernCompletionZones.zones
        SafariBiome.ICY -> IcyCompletionZones.zones
        SafariBiome.HAUNTED -> HauntedCompletionZones.zones
    }
    val uuidMinimums = mapOf("Scrappy" to 3, "Troodon" to 3, "Gazer" to 4, "Hideyho" to 1, "Doomspiral" to 1)
}
