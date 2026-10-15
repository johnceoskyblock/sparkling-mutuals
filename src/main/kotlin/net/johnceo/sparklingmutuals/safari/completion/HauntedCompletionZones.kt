package net.johnceo.sparklingmutuals.safari.completion

object HauntedCompletionZones {
    val zones = listOf("Areita", "Bloodbat", "Solsnatcher", "Litterbug", "Duplico", "Hideonwall")
        .associateWith { CavernCompletionZones.Zone(-3.0, -63.0, 30.0, 40.0) }
}
