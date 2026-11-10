package net.johnceo.sparklingmutuals.safari.resources

/** Inventory loss is confirmed after respawn, never guessed from the death's 50% chance. */
class DeathInventoryLoss {
    private var before=emptyMap<String,Int>()
    private var death: Map<String,Int>?=null
    private var candidate=emptyMap<String,Int>()
    private var stableAt=0L
    fun observe(alive: Boolean,stacks: List<Pair<String,Int>>,now: Long): Map<String,Int> {
        val current=stacks.filter { it.second>0 }.groupBy { net.johnceo.sparklingmutuals.safari.SafariRules.strip(it.first).trim() }
            .mapValues { (_,items) -> items.sumOf { it.second } }
        if(!alive) { if(death==null) death=before;candidate=emptyMap();stableAt=now;return emptyMap() }
        val baseline=death
        if(baseline==null) { before=current;return emptyMap() }
        if(current!=candidate) { candidate=current;stableAt=now;return emptyMap() }
        if(now-stableAt<1000) return emptyMap()
        death=null;before=current
        return baseline.mapValues { (name,n) -> (n-(current[name] ?: 0)).coerceAtLeast(0) }.filterValues { it>0 }
    }
}
