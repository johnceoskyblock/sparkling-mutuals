package net.johnceo.sparklingmutuals.safari

object IcyCompletionZones {
    private fun zone(x: Double, z: Double) = CavernCompletionZones.Zone(x,z,30.0,40.0)
    val zones = mapOf("Tepid" to zone(-73.0,-46.0), "Strongarm" to zone(-107.0,-56.0),
        "Polaris" to zone(-110.0,-78.0), "Shuddersquid" to zone(-127.0,-48.0),
        "Nozzlenose" to zone(-73.0,-46.0), "Mantis Shrimp" to zone(-73.0,-46.0),
        "Billygoat" to zone(-121.0,-55.0), "Wumpa" to zone(-110.0,-78.0))
}

object SafariCompletionZones {
    fun forBiome(biome: SafariBiome) = when (biome) {
        SafariBiome.CAVERN -> CavernCompletionZones.zones
        SafariBiome.ICY -> IcyCompletionZones.zones
        SafariBiome.HAUNTED -> HauntedCompletionZones.zones
        else -> emptyMap()
    }
    val uuidMinimums = mapOf("Scrappy" to 3, "Troodon" to 3, "Gazer" to 4, "Hideyho" to 1, "Doomspiral" to 1)
}
