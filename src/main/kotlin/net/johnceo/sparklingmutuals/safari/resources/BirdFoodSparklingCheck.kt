package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.SafariRules

/** Local food/marker evidence only. Spawn messages confirm food use, never fabricate catches. */
class BirdFoodSparklingCheck {
    private var held: Int? = null
    private var floor: Int? = null
    private var consumed = false
    private var population = 0
    private val earlySpawns = ArrayDeque<Long>()
    var pendingSpawns = 0
        private set
    val leftToUse: Int? get() = held?.let { h -> floor?.let { it+h } }
    val complete get() = floor == 0 && held == 0 && pendingSpawns == 0 && (consumed || population >= 9)
    fun floor(remaining: Int) { floor = if(remaining >= 0) remaining else floor?.takeIf { it > 0 } }
    fun population(accountedFeedings: Int) { population=maxOf(population,accountedFeedings) }
    fun inventory(stacks: List<Pair<String,Int>>, now: Long) {
        val amount=stacks.filter { SafariRules.strip(it.first).trim() in foods }.sumOf { it.second.coerceAtLeast(0) }
        val previous=held
        if(previous != null && amount < previous) {
            consumed=true
            // The latest use batch supersedes older missed confirmations.
            pendingSpawns = previous-amount
            // The server can send the spawn line before the inventory packet reaches the client.
            val since=now-2000
            while(earlySpawns.isNotEmpty() && earlySpawns.first() < since) earlySpawns.removeFirst()
            while(pendingSpawns > 0 && earlySpawns.isNotEmpty()) { earlySpawns.removeFirst();pendingSpawns-- }
        }
        held=amount
        while(earlySpawns.isNotEmpty() && now-earlySpawns.first() > 2000) earlySpawns.removeFirst()
    }
    fun spawn(raw: String, now: Long): Boolean {
        if(SafariRules.strip(raw) !in spawns) return false
        if(pendingSpawns > 0) pendingSpawns-- else earlySpawns.addLast(now)
        return true
    }
    companion object {
        private val foods=setOf("Bag of Seeds","Wriggleworm","Yogi Berry")
        private val spawns=setOf("A Bluebird was attracted to the Birdfeeder!",
            "A Parakeet was attracted to the Birdfeeder!","Two Macaws were attracted to the Birdfeeder!")
    }
}
