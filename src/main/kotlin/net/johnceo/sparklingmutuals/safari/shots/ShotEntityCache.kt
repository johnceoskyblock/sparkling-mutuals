package net.johnceo.sparklingmutuals.safari.shots

/** Independent of ESP filters/retirement. Identities here are private runtime values and never encoded. */
class ShotEntityCache {
    data class Entry(val id:Int,val identity:String,val token:String,val generation:Int,val variant:ShotVariant?,
        val species:String?,val discovery:ShotDiscovery,val spawnMicros:Long?,var base:ShotVector?) {
        var display:ShotObservation.DisplayState?=null
        var sample:ShotVector?=null
    }
    data class Pending(val identity:String,val position:ShotVector,val micros:Long,
        val updates:MutableList<Pair<Long,ShotPacketUpdate>> = mutableListOf(),var currentBase:ShotVector=position)
    private val live=linkedMapOf<Int,Entry>()
    private val pending=linkedMapOf<Int,Pending>()
    private val gaps=linkedSetOf<ShotGap>()
    private var generation=0
    private var capsuleCount=0
    private val expired=mutableSetOf<String>()
    val pendingCount get()=pending.size
    fun entries()=live.values.toList()
    fun get(id:Int)=live[id]
    fun packetBase(id:Int)=live[id]?.base ?: pending[id]?.currentBase
    fun pendingSpawn(id:Int,identity:String,position:ShotVector,micros:Long) {
        if (pending.size>=256 && id !in pending) { pending.remove(pending.keys.first());gaps.add(ShotGap.CACHE_BACKLOG) }
        pending[id]=Pending(identity,position,micros)
    }
    fun remember(update:ShotPacketUpdate,micros:Long) {
        val p=pending[update.id] ?: return
        if(p.updates.size>=8) { p.updates.removeAt(0);gaps.add(ShotGap.CACHE_BACKLOG) }
        p.updates.add(micros to update)
        when(update) {
            is ShotPacketUpdate.Move -> update.resolvedEnd?.let { p.currentBase=it }
            is ShotPacketUpdate.PositionSync -> (update.end ?: update.points.lastOrNull()?.position)?.let { p.currentBase=it }
            is ShotPacketUpdate.Spawn -> p.currentBase=update.position
            else -> {}
        }
    }
    fun takePending(id:Int)=pending.remove(id)
    fun expire(micros:Long) { pending.entries.removeIf { micros-it.value.micros>500_000 } }
    fun discover(id:Int,identity:String,variant:ShotVariant?,species:String?,discovery:ShotDiscovery,micros:Long):Entry? {
        if(identity in expired) return null
        live[id]?.let { if(it.identity==identity && it.variant==variant && it.species==species) return it;live.remove(id) }
        if(variant==null && species==null) return null
        if(variant!=null && (live.values.count { it.variant!=null }>=16 || capsuleCount>=256)) { gaps.add(ShotGap.CAPSULE_LIMIT);return null }
        if(species!=null && live.values.count { it.species!=null }>=64) { gaps.add(ShotGap.TARGET_LIMIT);return null }
        val p=pending[id]?.takeIf { it.identity==identity }
        val n=++generation
        if(variant!=null) capsuleCount++
        return Entry(id,identity,"${if(variant!=null) "c" else "t"}$n",n,variant,species,discovery,
            p?.micros ?: micros.takeIf { discovery==ShotDiscovery.SPAWN },p?.position).also { live[id]=it }
    }
    fun remove(id:Int):Entry? { pending.remove(id);return live.remove(id) }
    fun expireWindow():List<Entry> = live.values.filter { it.variant!=null }.toList().also { entries ->
        entries.forEach { expired.add(it.identity);live.remove(it.id) }
    }
    fun newWindow() { expired.clear() }
    fun gap(reason:ShotGap) { gaps.add(reason) }
    fun takeGaps()=gaps.toList().also { gaps.clear() }
    fun reset() { live.clear();pending.clear();gaps.clear();expired.clear();generation=0;capsuleCount=0 }
}
