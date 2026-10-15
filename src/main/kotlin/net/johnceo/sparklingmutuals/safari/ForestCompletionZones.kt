package net.johnceo.sparklingmutuals.safari

object ForestCompletionZones {
    // Use the existing Forest scan center, with the same central coverage radius as Haunted.
    fun inBirdScanRadius(x: Double, z: Double) = (x - 6.0) * (x - 6.0) + (z - 51.0) * (z - 51.0) <= 225.0
    val zones = mapOf("Foxtrot" to 30.0, "Fluffling" to 30.0, "Woodchucker" to 50.0,
        "Treefrog" to 50.0, "Hideonfloor" to 50.0)
        .mapValues { (_, radius) -> CavernCompletionZones.Zone(3.0, 47.0, radius, if (radius == 50.0) 55.0 else radius + 10) }
}
