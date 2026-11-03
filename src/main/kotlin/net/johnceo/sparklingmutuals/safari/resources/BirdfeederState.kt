package net.johnceo.sparklingmutuals.safari.resources

/** Feeder slot snapshots and spawn lines can describe the same consumption in either order. */
class BirdfeederState {
    var food: String?=null
        private set
    var count=0
        private set
    private var rawFood: String?=null
    private var rawCount=0
    private var observed=false
    private var slotDrops=0
    private var slotDropsAt=0L
    private var spawnDebt=0
    fun newMenu() { observed=false;slotDrops=0;spawnDebt=0 }
    fun observe(name: String?,amount: Int,now: Long) {
        val next=amount.coerceAtLeast(0)
        val type=name.takeIf { next>0 }
        if(observed && rawFood==type && rawCount==next) return
        if(observed && (rawFood==type || type==null) && next<rawCount) {
            val decrease=rawCount-next
            val matched=minOf(spawnDebt,decrease)
            spawnDebt-=matched
            slotDrops=if(now-slotDropsAt<=2000) slotDrops+decrease-matched else decrease-matched
            slotDropsAt=now
        } else { slotDrops=0;spawnDebt=0 }
        observed=true;rawFood=type;rawCount=next
        food=type;count=next
    }
    fun spawn(now: Long) {
        if(slotDrops>0 && now-slotDropsAt in 0..2000) { slotDrops--;return }
        if(count>0) { count--;spawnDebt++;if(count==0) food=null }
    }
}

/** Cursor/resync handling adapted from SafariUtils BirdfeederWatch (MIT). */
class BirdCursorGuard {
    private var expected=emptyMap<String,Int>()
    private var until=0L
    private var feederUntil=0L
    private val stable=mutableMapOf<String,Int>()
    private val pending=mutableMapOf<String,Pair<Int,Long>>()
    fun extra(held: Map<String,Int>,cursor: Map<String,Int>,feederOpen: Boolean,feederFood: String?,now: Long): Map<String,Int> {
        if(feederOpen) {
            feederUntil=now+500
            expected=if(cursor.isEmpty()) emptyMap() else cursor.mapValues { (food,count) -> (held[food] ?: 0)+count }
            until=now+if(cursor.keys.any { feederFood!=null && it!=feederFood }) 12000 else 2000
        }
        if(now>until) expected=emptyMap()
        val extras=cursor.toMutableMap()
        if(!feederOpen) {
            expected=expected.filter { (food,count) -> (held[food] ?: 0)+(cursor[food] ?: 0)<count }
            expected.forEach { (food,count) -> extras[food]=maxOf(extras[food] ?: 0,count-(held[food] ?: 0)) }
        }
        // Match SafariUtils' short transaction buffer: a refresh gap or rejected click
        // must not count as a deposit before cursor/inventory packets settle.
        for(food in (held.keys+extras.keys+stable.keys).toSet()) {
            val amount=(held[food] ?: 0)+(extras[food] ?: 0)
            val previous=stable[food] ?: 0
            if(amount<previous && now<=feederUntil) {
                val candidate=pending[food]?.takeIf { it.first==amount } ?: (amount to now+150).also { pending[food]=it }
                if(now<candidate.second) { extras[food]=(extras[food] ?: 0)+previous-amount;continue }
            }
            stable[food]=amount;pending.remove(food)
        }
        return extras
    }
}
