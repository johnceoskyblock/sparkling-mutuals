package net.johnceo.sparklingmutuals.safari

/** Coin-source presence is independent of visible ESP and remaining incense work. */
object HauntedCoinDrops {
    fun remaining(raw: List<EspDrop>, completeGroups: List<PredictedFloorDrop>): Int = raw.count { marker ->
        if (SafariEspRules.biomeAt(marker.x.toDouble(), marker.z.toDouble()) != SafariBiome.HAUNTED) return@count false
        val group = completeGroups.firstOrNull { drop ->
            // Triple members can straddle block boundaries; IDs bind them to their validated group.
            drop.live && drop.biome == SafariBiome.HAUNTED && marker.id in drop.id..drop.id + 2
        }
        // Only a complete typed incense group is irrelevant to coins. Unknown, partial,
        // generic and coin markers remain blockers, including when ESP hides them.
        group == null || (group.actual?.type ?: group.type) != FloorDropType.INCENSE
    }
}
