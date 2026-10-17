package net.johnceo.sparklingmutuals.safari.esp
import net.johnceo.sparklingmutuals.safari.shots.*
/** Existing visual body placement only. Padding and interpolation remain with ESP. */
object EspBodyBounds {
 fun visual(p:ShotVector,kind:String,shulker:Boolean):ShotBounds? = when(kind) {
  "display" -> if(shulker) ShotBounds(ShotVector(p.x-.5,p.y,p.z-.5),ShotVector(p.x+.5,p.y+1,p.z+.5))
   else ShotBounds(ShotVector(p.x-.3,p.y-.45,p.z-.3),ShotVector(p.x+.3,p.y+.15,p.z+.3))
  "armor_stand" -> ShotBounds(ShotVector(p.x-.3,p.y+1.35,p.z-.3),ShotVector(p.x+.3,p.y+1.95,p.z+.3))
  else -> null
 }
}
