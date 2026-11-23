package net.johnceo.sparklingmutuals.safari.sparkling
import java.util.UUID
/** Local, bounded particle corroboration. Does not create catches or remote evidence. */
internal class StandardParticleEvidence {
 private data class Evidence(val species:String, val count:Int, val at:Long)
 private val evidence=mutableMapOf<UUID,Evidence>()
 fun observe(id:UUID,species:String,now:Long) {
  evidence.entries.removeIf { now-it.value.at > 2500 || now < it.value.at }
  val old=evidence[id]?.takeIf { it.species==species && now-it.at in 0..2500 }
  if(old==null && evidence.size>=256) return
  evidence[id]=Evidence(species,((old?.count ?: 0)+1).coerceAtMost(3),now)
 }
 fun confirmed(id:UUID,species:String,now:Long)=evidence[id]?.let { it.species==species && it.count>=3 && now-it.at in 0..2500 } == true
 fun clear()=evidence.clear()
 companion object {
  fun pattern(wax:Boolean,count:Int,x:Float,y:Float,z:Float,speed:Float)= wax && count==5 &&
   listOf(x to .3f,y to .5f,z to .3f,speed to 1f).all { (a,b)->a.isFinite() && kotlin.math.abs(a-b)<=.011f }
 }
}
