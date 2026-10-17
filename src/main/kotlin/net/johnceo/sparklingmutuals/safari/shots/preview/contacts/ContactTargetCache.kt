package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
class ContactTargetCache {
 private var world:Any?=null
 private val entries=linkedMapOf<String,Pair<ContactDiscovery,ContactTarget>>()
 private var complete=false
 private var overflow=false
 private var sweep:Iterator<ContactDiscovery?>?=null
 fun reset() { world=null;entries.clear();complete=false;overflow=false;sweep=null }
 fun tick(world:Any,slots:Iterator<ContactDiscovery?>,inspect:(ContactDiscovery)->ContactTarget?) {
  if(this.world!==world) { reset();this.world=world }
  var used=0
  for((key,pair) in entries.toMap()) {
   used++
   val current=inspect(pair.first)
   if(current==null || current.generation!=pair.first.generation) entries.remove(key)
   else entries[key]=pair.first to current
  }
  if(sweep!==slots) { overflow=false;sweep=slots }
  while(used<240 && slots.hasNext()) {
   used++
   val discovery=slots.next() ?: continue
   val target=inspect(discovery) ?: continue
   if(discovery.key in entries || entries.size<64) entries[discovery.key]=discovery to target else overflow=true
  }
  complete=!slots.hasNext() && !overflow
 }
 fun snapshot():Pair<List<ContactTarget>,ContactCoverage> = entries.values.map{it.second}.toList() to
  ContactCoverage(complete,if(complete)emptySet() else setOf(if(overflow)"target_limit" else "scan_pending"))
}
data class ContactDiscovery(val key:String,val generation:Int)
