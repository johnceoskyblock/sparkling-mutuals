package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.completion.ForestCompletionZones

/** Run-owned food accounting; each feeder announcement consumes one food. */
class SafariBirdLedger {
    private val foods = listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")
    private val picked = mutableMapOf<String, Int>()
    private val forest = mutableMapOf<String, Int>()
    private val removed = mutableMapOf<String, Int>()
    private var feedings = 0
    private var populationProof: Int? = null
    private var recoveredDemand = 0
    private var heldFoods: Int? = null
    fun reenter() { populationProof = null }
    /** A current, central scan can recover feeder evidence missed before Forest entry. */
    fun scan(x: Double, z: Double, population: Int, dropsRemaining: Int) {
        if (dropsRemaining != 0 || heldFoods != 0) populationProof = null
        if (!ForestCompletionZones.inBirdScanRadius(x, z)) return
        if (dropsRemaining == 0 && heldFoods == 0) populationProof = maxOf(populationProof ?: 0, population)
        // Keep a previously validated source proof through ordinary unloading. New food,
        // live drops, held food and reentry still invalidate it through their own paths.
        if ((populationProof ?: -1) >= feedingTarget) recoveredDemand = maxOf(recoveredDemand, feedingTarget)
    }
    private fun populationResolves(target: Int) = heldFoods == 0 && (populationProof ?: -1) >= target
    val feedingResolved get() = used >= feedingTarget || populationResolves(feedingTarget)
    private var macawPairs = 0
    private val spawned = mutableMapOf<String, Int>()
    private var announced = false
    private val startingFoods = mutableMapOf<String, Int>()
    private val startingPickups = mutableMapOf<String, Int>()
    fun startingGrant(name: String) {
        if (name in foods) {
            populationProof = null
            startingFoods[name] = maxOf(startingFoods[name] ?: 0, 1)
        }
    }
    // Starting messages and inventory snapshots describe overlapping items. Later pickups add new demand.
    private val extraFoods get() = foods.sumOf { food ->
        val early = startingPickups[food] ?: 0
        maxOf(startingFoods[food] ?: 0, early) + (picked[food] ?: 0) - (forest[food] ?: 0) - early
    }
    val sparklingTarget get() = maxOf(9 + extraFoods, recoveredDemand)
    val feedingTarget get() = maxOf(maxOf(9, forest.values.sum()) + extraFoods, recoveredDemand)
    val sparklingSpawned get() = used >= sparklingTarget || populationResolves(sparklingTarget)
    /** Sample every tick during Head Start; subtract already reported Forest floor pickups. */
    fun startingInventory(stacks: List<Pair<String, Int>>, elapsed: Long) {
        if (elapsed !in 0L..3000L) return
        val held = stacks.groupBy { SafariRules.strip(it.first).trim() }
            .mapValues { (_, items) -> items.sumOf { it.second.coerceAtLeast(0) } }
        foods.forEach { food ->
            val amount = maxOf(startingFoods[food] ?: 0, (held[food] ?: 0) - (forest[food] ?: 0))
            if (amount > (startingFoods[food] ?: 0)) populationProof = null
            startingFoods[food] = amount
        }
    }
    val foodsComplete get() = foods.all { (forest[it] ?: 0) >= 3 }
    val allSpawned get() = foodsComplete && used >= feedingTarget
    val collected get() = picked.values.sum()
    val used get() = feedings
    fun spawnedCount(species: String) = spawned[species] ?: 0
    fun alert(biome: SafariBiome?, enabled: Boolean): Boolean {
        if (!enabled || biome != SafariBiome.FOREST || !foodsComplete || announced) return false
        announced = true
        return true
    }
    fun pickup(raw: String, biome: SafariBiome?, elapsed: Long? = null): Boolean {
        val text = SafariRules.strip(raw)
        if (!text.startsWith("FLOOR DROP!")) return false
        val food = foods.firstOrNull { Regex("\\b${Regex.escape(it)}\\b(?! Shard)").containsMatchIn(text) } ?: return false
        val quantity = FloorPickup.parse(text)?.quantity ?: 1
        if (recoveredDemand > 0) recoveredDemand += quantity
        populationProof = null
        picked.merge(food, quantity, Int::plus)
        if (biome == SafariBiome.FOREST) forest.merge(food, quantity, Int::plus)
        else if (elapsed == null || elapsed in 0L..3000L) startingPickups.merge(food, quantity, Int::plus)
        return true
    }
    /** Exact announcements documented by CritterMod 0.9.0 BirdfeederWatch (MIT). */
    fun spawn(raw: String): Boolean {
        val species = when (SafariRules.strip(raw)) {
            "A Bluebird was attracted to the Birdfeeder!" -> "Bluebird"
            "A Parakeet was attracted to the Birdfeeder!" -> "Parakeet"
            "Two Macaws were attracted to the Birdfeeder!" -> { macawPairs++; "Macaw" }
            else -> return false
        }
        if (populationProof != null && populationResolves(feedingTarget)) recoveredDemand = feedingTarget + 1
        populationProof = null
        spawned.merge(species, if (species == "Macaw") 2 else 1, Int::plus)
        feedings++
        return true
    }
    fun inventory(stacks: List<Pair<String, Int>>) {
        val held = stacks.groupBy { SafariRules.strip(it.first).trim() }.mapValues { (_, items) -> items.sumOf { it.second.coerceAtLeast(0) } }
        heldFoods = foods.sumOf { held[it] ?: 0 }
        if (heldFoods != 0) populationProof = null
        foods.forEach { removed[it] = maxOf(removed[it] ?: 0, (picked[it] ?: 0) - (held[it] ?: 0)) }
    }
    private fun pairs(caught: Int, remaining: Int) = maxOf(macawPairs, (caught + remaining.coerceAtLeast(0) + 1) / 2)
    fun hasMacaws(caught: Int, remaining: Int) = pairs(caught, remaining) > 0
    fun requiredCaptures(macawsCaught: Int, macawsRemaining: Int) =
        (feedingTarget + pairs(macawsCaught, macawsRemaining) - macawsRemaining.coerceAtLeast(0)).coerceAtLeast(0)
    fun sufficient(catches: Int, macawsCaught: Int, macawsRemaining: Int) = allSpawned &&
        catches >= requiredCaptures(macawsCaught, macawsRemaining)
    fun capturesComplete(count: (String) -> Int) = allSpawned &&
        listOf("Bluebird", "Parakeet", "Macaw").all { species ->
            count(species) >= if (species == "Macaw") minOf(1, spawned[species] ?: 0) else spawned[species] ?: 0
        }
}
