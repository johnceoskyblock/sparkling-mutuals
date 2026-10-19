package net.johnceo.sparklingmutuals.safari.esp

import net.johnceo.sparklingmutuals.safari.shots.ShotVector

data class ShywormBodyEntity(val id:Int,val type:String,val position:ShotVector,val head:Boolean=false,
    val vehicle:Int?=null,val passengers:List<Int> = emptyList())
data class ShywormBodySelection(val heads:List<Int>,val candidates:List<ShywormBodyEntity>,
    val scanTruncated:Boolean,val headsOmitted:Int,val candidatesOmitted:Int)

object ShywormBodySelector {
    const val SCAN_LIMIT=4096
    private val visualTypes=setOf("armor_stand","item_display","block_display","text_display","interaction")
    private fun ShotVector.finite()=x.isFinite() && y.isFinite() && z.isFinite()
    fun select(entities:List<ShywormBodyEntity>,eye:ShotVector):ShywormBodySelection {
        val truncated=entities.size>SCAN_LIMIT
        if(!eye.finite()) return ShywormBodySelection(emptyList(),emptyList(),truncated,0,0)
        val source=entities.take(SCAN_LIMIT).filter{it.position.finite() && it.type !in setOf("player","item")}.distinctBy{it.id}
        val heads=source.filter{it.head && it.position.distanceSquared(eye)<=24.0*24}.sortedWith(compareBy({it.position.distanceSquared(eye)},{it.id}))
        val selected=heads.take(2);val ids=selected.map{it.id}.toSet()
        val linked=selected.flatMap{it.passengers+listOfNotNull(it.vehicle)}.toSet()
        fun explicit(p:ShywormBodyEntity)=p.id in linked || p.vehicle in ids || p.passengers.any{it in ids}
        fun distance(p:ShywormBodyEntity)=selected.minOf{it.position.distanceSquared(p.position)}
        val candidates=if(selected.isEmpty()) emptyList() else source.filter {
            it.id in ids || explicit(it) || it.type in visualTypes && distance(it)<=8.0*8
        }.sortedWith(compareBy({if(it.id in ids)0 else if(explicit(it))1 else 2},{distance(it)},{it.id}))
        return ShywormBodySelection(selected.map{it.id},candidates.take(64),truncated,
            (heads.size-selected.size).coerceAtLeast(0),(candidates.size-64).coerceAtLeast(0))
    }
}
