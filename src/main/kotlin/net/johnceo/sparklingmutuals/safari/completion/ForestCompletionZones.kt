package net.johnceo.sparklingmutuals.safari.completion

object ForestCompletionZones {
    const val BIRD_RADIUS = 40.0
    private fun within(x: Double, z: Double, radius: Double) =
        (x - 6.0) * (x - 6.0) + (z - 51.0) * (z - 51.0) <= radius * radius
    fun inBirdScanRadius(x: Double, z: Double) = within(x, z, BIRD_RADIUS)
    fun nearBirdScanRadius(x: Double, z: Double) = within(x, z, BIRD_RADIUS + 20.0)
    val zones = mapOf("Foxtrot" to 30.0, "Fluffling" to 30.0, "Woodchucker" to 50.0,
        "Treefrog" to 50.0, "Hideonfloor" to 50.0)
        .mapValues { (_, radius) -> CavernCompletionZones.Zone(3.0, 47.0, radius, if (radius == 50.0) 55.0 else radius + 10) }
}
