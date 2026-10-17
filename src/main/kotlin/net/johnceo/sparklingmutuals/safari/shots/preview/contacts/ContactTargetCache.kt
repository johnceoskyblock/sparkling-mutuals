package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
class ContactTargetCache(private val priority:(ContactTarget)->Double = {0.0}) {
 private var world:Any?=null
 private val entries=linkedMapOf<String,Pair<ContactDiscovery,ContactTarget>>()
 private var complete=false
 private var overflow=false
 private var sweep:Iterator<ContactDiscovery?>?=null
 private var metadataDirty=false
 var scanInterrupted=false;private set
 fun invalidate() { complete=false;sweep=null;scanInterrupted=true }
 // Metadata cannot corrupt the map iterator. Keep scanning, but require a fresh sweep for full coverage.
 fun metadataChanged() { complete=false;metadataDirty=true }
 fun reset() { world=null;entries.clear();complete=false;overflow=false;sweep=null;scanInterrupted=false;metadataDirty=false }
 fun tick(world:Any,slots:Iterator<ContactDiscovery?>,inspect:(ContactDiscovery)->ContactTarget?) {
  if(this.world!==world) { reset();this.world=world }
  var used=0
  val ranks=mutableMapOf<String,Double>()
  fun rank(target:ContactTarget)=priority(target).takeIf{it.isFinite() && it>=0.0} ?: Double.POSITIVE_INFINITY
  for((key,pair) in entries.toMap()) {
   used++
   val current=inspect(pair.first)
   if(current==null || current.generation!=pair.first.generation) entries.remove(key)
   else { entries[key]=pair.first to current;ranks[key]=rank(current) }
  }
  if(sweep!==slots) { overflow=false;sweep=slots;scanInterrupted=false;metadataDirty=false }
  fun available():Boolean = try { slots.hasNext() } catch(_:java.util.ConcurrentModificationException) { invalidate();false }
  while(used<240 && available()) {
   used++
   // Only iterator access is guarded. Real identification/inspection failures still propagate.
   val discovery=try { slots.next() } catch(_:IndexOutOfBoundsException) { invalidate();break }
    catch(_:java.util.ConcurrentModificationException) { invalidate();break }
    catch(_:NoSuchElementException) { invalidate();break } ?: continue
   val target=inspect(discovery) ?: continue
   val score=rank(target)
   if(discovery.key in entries || entries.size<64) {
    entries[discovery.key]=discovery to target;ranks[discovery.key]=score
   } else {
    overflow=true
    val worst=ranks.maxByOrNull{it.value}
    if(worst!=null && score<worst.value) {
     entries.remove(worst.key);ranks.remove(worst.key)
     entries[discovery.key]=discovery to target;ranks[discovery.key]=score
    }
   }
  }
  val remaining=available()
  complete=!scanInterrupted && !metadataDirty && !remaining && !overflow
 }
 fun snapshot():Pair<List<ContactTarget>,ContactCoverage> = entries.values.map{it.second}.toList() to
  ContactCoverage(complete,if(complete)emptySet() else setOf(if(overflow)"target_limit" else "scan_pending"))
}
data class ContactDiscovery(val key:String,val generation:Int)
