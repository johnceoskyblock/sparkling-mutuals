package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
class ContactTargetCache {
 private var world:Any?=null
 private val entries=linkedMapOf<String,Pair<ContactDiscovery,ContactTarget>>()
 private var complete=false
 private var overflow=false
 private var sweep:Iterator<ContactDiscovery?>?=null
 var scanInterrupted=false;private set
 fun invalidate() { complete=false;sweep=null;scanInterrupted=true }
 fun reset() { world=null;entries.clear();complete=false;overflow=false;sweep=null;scanInterrupted=false }
 fun tick(world:Any,slots:Iterator<ContactDiscovery?>,inspect:(ContactDiscovery)->ContactTarget?) {
  if(this.world!==world) { reset();this.world=world }
  var used=0
  for((key,pair) in entries.toMap()) {
   used++
   val current=inspect(pair.first)
   if(current==null || current.generation!=pair.first.generation) entries.remove(key)
   else entries[key]=pair.first to current
  }
  if(sweep!==slots) { overflow=false;sweep=slots;scanInterrupted=false }
  fun available():Boolean = try { slots.hasNext() } catch(_:java.util.ConcurrentModificationException) { invalidate();false }
  while(used<240 && available()) {
   used++
   // Only iterator access is guarded. Real identification/inspection failures still propagate.
   val discovery=try { slots.next() } catch(_:IndexOutOfBoundsException) { invalidate();break }
    catch(_:java.util.ConcurrentModificationException) { invalidate();break }
    catch(_:NoSuchElementException) { invalidate();break } ?: continue
   val target=inspect(discovery) ?: continue
   if(discovery.key in entries || entries.size<64) entries[discovery.key]=discovery to target else overflow=true
  }
  val remaining=available()
  complete=!scanInterrupted && !remaining && !overflow
 }
 fun snapshot():Pair<List<ContactTarget>,ContactCoverage> = entries.values.map{it.second}.toList() to
  ContactCoverage(complete,if(complete)emptySet() else setOf(if(overflow)"target_limit" else "scan_pending"))
}
data class ContactDiscovery(val key:String,val generation:Int)
