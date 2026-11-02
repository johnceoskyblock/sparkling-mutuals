package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRules

/** Nine floor-food opportunities plus known starting food; only qualified renders establish spawns. */
class SafariBirdLedger {
    private val foods = listOf("Bag of Seeds", "Wriggleworm", "Yogi Berry")
    private val picked = mutableMapOf<String, Int>()
    private val forest = mutableMapOf<String, Int>()
    private val startingFoods = mutableMapOf<String, Int>()
    private val startingPickups = mutableMapOf<String, Int>()
    private val renderedTotals = mutableMapOf<String, Int>()
    private var floorDropsRemaining: Int? = null
    private var currentPopulation: Int? = null
    private var verifiedPopulation = 0
    private var heldFoods: Int? = null
    private val heldByType=mutableMapOf<String,Int>()
    private val spawned=mutableMapOf<String,Int>()
    var spawnEvents=0
        private set
    var feederFood: String?=null
        private set
    var feederCount=0
        private set
    fun feeder(name: String?, count: Int) { feederFood=name;feederCount=count.coerceAtLeast(0) }
    fun held(name: String)=heldByType[name] ?: 0
    fun remaining(name: String)=(if(floorDropsRemaining == 0) 0 else maxOf(0,3-(forest[name] ?: 0)))+held(name)

    private var reportedExtras = 0
    private val inferredFood = mutableSetOf<String>()
    private var inferredExtras = 0
    /** One provisional food per unreporting teammate; later starting reports replace the estimate. */
    fun earlyVisitor(name: String, elapsed: Long, nearBirds: Boolean, hasStartingReport: Boolean) {
        // Teammate supplies are intentionally not part of local source requirements.
    }
    private var reportedTarget: Int? = null
    private var announced = false
    private var forestPickupEvents = 0
    private fun retainSpawnProof() {
        if (floorDropsRemaining == 0 && heldFoods == 0)
            verifiedPopulation = maxOf(verifiedPopulation, currentPopulation ?: 0)
    }
    fun reenter() { currentPopulation = null }
    /** Food markers remain visible Safari-wide; travel must not erase a known remaining drop. */
    fun floor(dropsRemaining: Int) {
        floorDropsRemaining = if (dropsRemaining >= 0) dropsRemaining else floorDropsRemaining?.takeIf { it > 0 }
        retainSpawnProof()
    }
    fun scan(dropsRemaining: Int, population: Int? = null) { floor(dropsRemaining); if (population != null) currentPopulation = population; retainSpawnProof() }
    fun render(captures: (String) -> Int, live: List<String>) {
        for (name in listOf("Bluebird", "Parakeet", "Macaw")) {
            renderedTotals[name] = maxOf(renderedTotals[name] ?: 0, captures(name) + live.count { it == name })
        }
        currentPopulation = captures("Bluebird") + live.count { it == "Bluebird" } +
            captures("Parakeet") + live.count { it == "Parakeet" } +
            (captures("Macaw") + live.count { it == "Macaw" }) / 2
        retainSpawnProof()
    }
    /** Includes uncollected food and known party extras; render evidence estimates food already used. */
    val foodLeftToUse get() = maxOf(0, feedingTarget - maxOf(accountedFeedings, verifiedPopulation),
        (heldFoods ?: 0) + (floorDropsRemaining ?: 0))
    val accountedFeedings get() = maxOf(currentPopulation ?: 0,spawnEvents)
    val knownFoodUnresolved get() = (heldFoods ?: 0) > 0 || (floorDropsRemaining ?: 0) > 0
    val feedingResolved get() = floorDropsRemaining == 0 && heldFoods == 0 &&
        maxOf(accountedFeedings, verifiedPopulation) >= feedingTarget && feederCount == 0
    val allNineFloorFoodUsed get() = floorDropsRemaining==0 && heldFoods==0 && feederCount==0 &&
        maxOf(accountedFeedings,verifiedPopulation)>=feedingTarget && feedingTarget>=9
    val sparklingSpawned get() = feedingResolved
    val allSpawned get() = feedingResolved
    val collected get() = picked.values.sum()
    val forestCollected get() = forest.values.sum()
    val foodsComplete get() = foods.all { (forest[it] ?: 0) >= 3 }
    val reportSuppliesReady get() = floorDropsRemaining == 0 && heldFoods == 0
    private fun localExtra(food: String): Int {
        val early = startingPickups[food] ?: 0
        return maxOf(startingFoods[food] ?: 0, early) + (picked[food] ?: 0) - (forest[food] ?: 0) - early
    }
    private val localExtras get() = foods.sumOf(::localExtra)
    val feedingTarget get() = maxOf(9, forestCollected) + localExtras + reportedExtras + inferredExtras
    val sparklingTarget get() = feedingTarget
    /** Sender/type maxima deduplicate repeated notices; local notices overlap starting inventory. */
    fun contributions(local: String?, notices: Map<Pair<String, FloorDropType>, Int>, reporters: Set<String> = emptySet()) {
        reportedExtras=0; inferredExtras=0
    }
    fun report() { reportedTarget = feedingTarget }
    val reportCoversTarget get() = (reportedTarget ?: -1) >= feedingTarget
    val hasReport get() = reportedTarget != null
    val remoteReady get() = !knownFoodUnresolved && (reportedTarget ?: 0) >= feedingTarget
    fun spawnedCount(species: String) = maxOf(renderedTotals[species] ?: 0, spawned[species] ?: 0)
    fun startingGrant(name: String) { if (name in foods) startingFoods[name] = maxOf(startingFoods[name] ?: 0, 1) }
    fun startingInventory(stacks: List<Pair<String, Int>>, elapsed: Long) {
        if (elapsed !in 0L..3000L) return
        val held = stacks.groupBy { SafariRules.strip(it.first).trim() }
            .mapValues { (_, items) -> items.sumOf { it.second.coerceAtLeast(0) } }
        foods.forEach { food -> startingFoods[food] = maxOf(startingFoods[food] ?: 0,
            (held[food] ?: 0) - ((picked[food] ?: 0) - (startingPickups[food] ?: 0))) }
    }
    fun pickup(raw: String, biome: SafariBiome?, elapsed: Long? = null): Boolean {
        val text = SafariRules.strip(raw)
        if (!text.startsWith("FLOOR DROP!")) return false
        val food = foods.firstOrNull { Regex("\\b${Regex.escape(it)}\\b(?! Shard)").containsMatchIn(text) } ?: return false
        val quantity = FloorPickup.parse(text)?.quantity ?: 1
        currentPopulation = null
        verifiedPopulation = 0 // A newly found food contradicts the previous all-food-used proof.
        picked.merge(food, quantity, Int::plus)
        if (biome == SafariBiome.FOREST) { forest.merge(food, quantity, Int::plus); forestPickupEvents++ }
        else if (elapsed == null || elapsed in 0L..3000L) startingPickups.merge(food, quantity, Int::plus)
        return true
    }
    /** Each server feeder line is one food use; Macaws arrive as a pair. */
    fun spawn(raw: String): Boolean {
        val name=when(SafariRules.strip(raw)) {
            "A Bluebird was attracted to the Birdfeeder!" -> "Bluebird"
            "A Parakeet was attracted to the Birdfeeder!" -> "Parakeet"
            "Two Macaws were attracted to the Birdfeeder!" -> "Macaw"
            else -> return false
        }
        spawnEvents++
        spawned.merge(name,if(name == "Macaw") 2 else 1,Int::plus)
        feederCount=(feederCount-1).coerceAtLeast(0)
        return true
    }
    fun inventory(stacks: List<Pair<String, Int>>) {
        foods.forEach { food -> heldByType[food]=stacks.filter { SafariRules.strip(it.first).trim()==food }.sumOf { it.second.coerceAtLeast(0) } }
        heldFoods = stacks.filter { SafariRules.strip(it.first).trim() in foods }.sumOf { it.second.coerceAtLeast(0) }
        retainSpawnProof()
    }
    fun alert(biome: SafariBiome?, enabled: Boolean): Boolean {
        if (!enabled || biome != SafariBiome.FOREST || !foodsComplete || announced) return false
        announced = true
        return true
    }
    private fun pairs(caught: Int, remaining: Int) = maxOf(spawnedCount("Macaw") / 2, (caught + remaining.coerceAtLeast(0)) / 2)
    fun hasMacaws(caught: Int, remaining: Int) = spawnedCount("Macaw") > 0 || caught > 0 || remaining > 0
    fun requiredCaptures(macawsCaught: Int, macawsRemaining: Int) =
        (feedingTarget - pairs(macawsCaught, macawsRemaining) + if (hasMacaws(macawsCaught, macawsRemaining)) 1 else 0).coerceAtLeast(0)
    fun sufficient(catches: Int, macawsCaught: Int, macawsRemaining: Int) =
        !knownFoodUnresolved && catches >= requiredCaptures(macawsCaught, macawsRemaining)
    /** Full Clear keeps one-Macaw credit; fd never supplies missing captures. */
    fun capturesComplete(count: (String) -> Int): Boolean {
        if (knownFoodUnresolved) return false
        val macaws = count("Macaw")
        val macawCredit = if (macaws > 0) maxOf(spawnedCount("Macaw") / 2, (macaws + 1) / 2) else 0
        return count("Bluebird") >= spawnedCount("Bluebird") && count("Parakeet") >= spawnedCount("Parakeet") &&
            (!hasMacaws(macaws, 0) || macaws >= 1) &&
            count("Bluebird") + count("Parakeet") + macawCredit >= feedingTarget
    }
    fun debug() = "expected=$feedingTarget qualifiedPopulation=$currentPopulation verifiedPopulation=$verifiedPopulation floorRemaining=$floorDropsRemaining " +
        "heldFood=$heldFoods localForestPickups=$forestPickupEvents inferredExtras=$inferredExtras reportTarget=$reportedTarget renderedTotals=$renderedTotals"
}
