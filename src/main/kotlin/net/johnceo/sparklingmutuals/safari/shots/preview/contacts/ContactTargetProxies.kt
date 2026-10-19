package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import net.johnceo.sparklingmutuals.safari.esp.EspBodyBounds
object ContactTargetProxies {
 fun body(kind:String,position:ShotVector,native:ShotBounds,identified:Boolean,shulker:Boolean):Pair<ShotBounds,String>? {
  if(!identified || !position.finite() || kind in setOf("player","capsule","label")) return null
  val visual=EspBodyBounds.visual(position,kind,shulker)
  if(visual!=null) return visual to if(kind=="display") "display_body_v1" else "armor_stand_body_v1"
  return native.takeIf{it.valid()}?.let{it to "client_body_v1"}
 }
}
