package net.johnceo.sparklingmutuals.safari

object ForestCompletionZones {
    val zones = mapOf("Foxtrot" to 30.0, "Fluffling" to 30.0, "Woodchucker" to 50.0,
        "Treefrog" to 50.0, "Hideonfloor" to 50.0)
        .mapValues { (_, radius) -> CavernCompletionZones.Zone(3.0, 47.0, radius, radius + 10) }
}
