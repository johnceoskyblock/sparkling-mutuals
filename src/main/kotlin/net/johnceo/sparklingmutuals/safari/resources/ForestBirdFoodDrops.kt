package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.esp.EspDrop
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules

/** Forest reward markers include non-food rewards; only known non-food groups can be excluded. */
object ForestBirdFoodDrops {
    private val food = setOf(FloorDropType.WORM, FloorDropType.BERRY, FloorDropType.SEEDS)
    fun remaining(raw: List<EspDrop>, completeGroups: List<PredictedFloorDrop>): Int = raw.count { marker ->
        if (SafariEspRules.biomeAt(marker.x.toDouble(), marker.z.toDouble()) != SafariBiome.FOREST) return@count false
        val group = completeGroups.firstOrNull { drop ->
            drop.live && drop.biome == SafariBiome.FOREST && marker.id in drop.id..drop.id + 2
        }
        // Use full, active reward triples regardless of ESP selection. Unknown/partial markers
        // still block, but a complete XP/essence/shard group is not remaining bird food.
        val type = group?.actual?.type ?: group?.type
        group == null || type == null || type in food
    }
}
