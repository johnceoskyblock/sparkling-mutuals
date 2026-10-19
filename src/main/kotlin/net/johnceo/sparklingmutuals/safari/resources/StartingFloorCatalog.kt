package net.johnceo.sparklingmutuals.safari.resources

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariBiome

/** Structural coverage of the existing order classifier, separate from pickup and rendering evidence. */
class StartingFloorCatalog {
    private val observed = sortedMapOf<Int, PredictedFloorDrop>()
    private val members = mutableMapOf<Int, UUID>()
    private val coins = linkedSetOf<Int>()
    private val foods = linkedSetOf<Int>()
    private var conflictingIdentity = false
    var complete = false
        private set
    var revision = 0L
        private set
    val coinIds get() = coins.toSet()
    val foodIds get() = foods.toSet()
    val scope get() = members.minByOrNull { it.key }?.value?.toString()?.replace("-", "")
    fun acceptsScope(token: String) = members.values.any { it.toString().replace("-", "") == token }

    fun observe(groups: List<PredictedFloorDrop>, activeMembers: Map<Int, UUID>) {
        val previous = Triple(complete, coins.toSet(), foods.toSet())
        groups.forEach { group ->
            val ids = (0..2).map { group.id + it }
            if (ids.all { it in activeMembers }) {
                ids.forEach { id ->
                    val uuid = activeMembers.getValue(id)
                    if (members[id]?.let { it != uuid } == true) conflictingIdentity = true
                    members.putIfAbsent(id, uuid)
                }
                observed[group.id] = group.copy()
            } else if (group.id in observed) {
                // Reprediction may refine a retained slot; unloading does not delete it.
                observed[group.id] = group.copy()
            }
        }
        val classified = observed.values.filter { it.type != FloorDropType.GENERIC }
        val cavern = classified.filter { it.biome == SafariBiome.CAVERN }
        val forest = classified.filter { it.biome == SafariBiome.FOREST }
        val haunted = classified.filter { it.biome == SafariBiome.HAUNTED }
        val icy = classified.filter { it.biome == SafariBiome.ICY }
        fun sequence(drops: List<PredictedFloorDrop>, types: List<FloorDropType>): Boolean =
            drops.map { it.type } == types && drops.zipWithNext().all { (a,b) -> b.id - a.id == 3 } &&
                drops.all { it.actual == null || it.actual?.type == it.type }
        val gemSet = listOf(FloorDropType.LIME, FloorDropType.PURPLE, FloorDropType.ORANGE)
        val foodSet = List(3) { FloorDropType.WORM } + List(3) { FloorDropType.BERRY } + List(3) { FloorDropType.SEEDS }
        val coveredMembers = observed.keys.flatMap { id -> (0..2).map { id + it } }.toSet()
        val phaseOrder = listOf(cavern, forest, haunted, icy)
        complete = !conflictingIdentity && activeMembers.keys.all { it in coveredMembers } &&
            sequence(cavern, gemSet + gemSet) && sequence(forest, foodSet) && haunted.size >= 5 &&
            sequence(haunted, List(5) { FloorDropType.INCENSE } + List((haunted.size - 5).coerceAtLeast(0)) { FloorDropType.COIN }) &&
            sequence(icy, List(5) { FloorDropType.ICEBREAKER }) &&
            phaseOrder.zipWithNext().all { (a,b) -> a.isNotEmpty() && b.isNotEmpty() && b.first().id - a.last().id == 3 }
        if (complete) {
            coins.addAll(haunted.filter { it.type == FloorDropType.COIN }.map { it.id })
            foods.addAll(forest.map { it.id })
        }
        observed.values.filter { it.actual?.type == FloorDropType.COIN }.forEach { coins.add(it.id) }
        if (previous != Triple(complete, coins.toSet(), foods.toSet())) revision++
    }
}
