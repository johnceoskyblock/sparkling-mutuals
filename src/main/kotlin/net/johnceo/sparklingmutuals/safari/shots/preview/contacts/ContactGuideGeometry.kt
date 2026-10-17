package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import kotlin.math.*
data class ContactLine(val from:ShotVector,val to:ShotVector,val color:Int,val width:Float)
data class ContactGuideMesh(val core:List<ContactLine>,val outline:List<ContactLine>,val marker:List<ContactLine>)
object ContactGuideGeometry {
 private const val AMBER=0xFFFFB347.toInt()
 private const val CYAN=0xFF55DDEE.toInt()
 private const val OUTLINE=0xFF151515.toInt()
 private fun plus(a:ShotVector,b:ShotVector,s:Double=1.0)=ShotVector(a.x+b.x*s,a.y+b.y*s,a.z+b.z*s)
 private fun cross(a:ShotVector,b:ShotVector)=ShotVector(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x)
 private fun unit(v:ShotVector,fallback:ShotVector):ShotVector {
  val length=hypot(hypot(v.x,v.y),v.z)
  return if(length.isFinite() && length>1e-10) ShotVector(v.x/length,v.y/length,v.z/length) else fallback
 }
 fun build(trace:ContactTrace,camera:ShotVector,coreWidth:Float):ContactGuideMesh {
  val width=if(coreWidth.isFinite())coreWidth.coerceIn(1f,5f) else 3f
  val core=ArrayList<ContactLine>();val marker=ArrayList<ContactLine>();var distance=0.0
  for((a,b) in trace.nodes.zipWithNext()) {
   val length=sqrt(a.position.distanceSquared(b.position))
   if(!length.isFinite() || length<=1e-12) continue
   if(a.observedRegion && b.observedRegion) core.add(ContactLine(a.position,b.position,AMBER,width))
   else {
    var d=0.0
    while(d<length-1e-10) {
     val phase=(distance+d)%.55
     val on=phase<.35;val end=min(length,d+(if(on).35-phase else .55-phase).coerceAtLeast(1e-8))
     if(on) core.add(ContactLine(a.position.lerp(b.position,d/length),a.position.lerp(b.position,end/length),AMBER,width))
     d=end
    }
   }
   distance+=length
  }
  trace.primary?.let{hit->
   val center=hit.position
   val normal=if(hit.kind==ContactKind.TERRAIN) unit(hit.normal ?: ShotVector(0.0,1.0,0.0),ShotVector(0.0,1.0,0.0))
    else unit(camera-center,ShotVector(0.0,0.0,1.0))
   val u=unit(cross(normal,if(abs(normal.y)<.9)ShotVector(0.0,1.0,0.0) else ShotVector(1.0,0.0,0.0)),ShotVector(1.0,0.0,0.0))
   val v=unit(cross(normal,u),ShotVector(0.0,1.0,0.0))
   fun point(x:Double,y:Double)=plus(plus(center,u,x),v,y)
   if(hit.kind==ContactKind.TERRAIN) repeat(16){i->
    val a=2*PI*i/16;val b=2*PI*(i+1)/16
    marker.add(ContactLine(point(.25*cos(a),.25*sin(a)),point(.25*cos(b),.25*sin(b)),AMBER,width))
   } else {
    val points=listOf(point(0.0,.25),point(.25,0.0),point(0.0,-.25),point(-.25,0.0),point(0.0,.25))
    points.zipWithNext().forEach{(a,b)->marker.add(ContactLine(a,b,CYAN,width))}
    marker.add(ContactLine(point(-.06,0.0),point(.06,0.0),CYAN,width));marker.add(ContactLine(point(0.0,-.06),point(0.0,.06),CYAN,width))
   }
  }
  return ContactGuideMesh(core.toList(),(core+marker).map{it.copy(color=OUTLINE,width=width+2)},marker.toList())
 }
}
