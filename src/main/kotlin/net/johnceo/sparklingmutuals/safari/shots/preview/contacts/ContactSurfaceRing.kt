package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import kotlin.math.*
object ContactSurfaceRing {
    /** Small local decal projected onto connected faces, not a change to the projectile's curve. */
    fun build(center:ShotVector,normal:ShotVector,surfaces:List<ContactSurfaceBox>,width:Float):List<ContactLine> {
        val n=normal.unit() ?: return emptyList()
        if(!center.finite()) return emptyList()
        val source=surfaces.take(32).filter{it.valid() && it.facesNear(center,.65).isNotEmpty()}
        val roots=source.filter{it.facesNear(center,.012).any{(_,face)->face.dot(n)>.99}}
        if(roots.isEmpty()) return emptyList()
        val connected=roots.toMutableSet()
        repeat(source.size) { for(s in source) if(s !in connected && connected.any{it.touches(s)}) connected.add(s) }
        val normals=mutableListOf(n)
        for(s in connected) for((point,face) in s.facesNear(center,.27)) {
            if(face.dot(n)<-1e-8 || normals.any{it.dot(face)>.999}) continue
            // Internal faces shared by solid boxes must not pull the ring into the solid union.
            if(connected.any{it!==s && it.contains(point.surfacePlus(face.scaled(.002)),-1e-6)}) continue
            normals.add(face)
        }
        val direction=normals.fold(ShotVector(0.0,0.0,0.0)){a,b->a.surfacePlus(b)}.unit() ?: n
        val u=direction.cross(if(abs(direction.y)<.9)ShotVector(0.0,1.0,0.0) else ShotVector(1.0,0.0,0.0)).unit() ?: return emptyList()
        val v=direction.cross(u).unit() ?: return emptyList()
        fun project(angle:Double):ContactSurfaceHit? {
            val p=center.surfacePlus(u.scaled(.25*cos(angle))).surfacePlus(v.scaled(.25*sin(angle)))
            val from=p.surfacePlus(direction.scaled(.65));val to=p.surfacePlus(direction.scaled(-.65))
            return connected.mapNotNull{it.entry(from,to)}.minByOrNull{it.fraction}?.takeIf{it.position.distanceSquared(center)<.5*.5}
        }
        val result=ArrayList<ContactLine>()
        fun add(a:ShotVector,b:ShotVector,faceNormal:ShotVector) {
            if(result.size>=24 || a.distanceSquared(b) !in 1e-14..(.15*.15)) return
            // Endpoint normals alone cannot detect a slit between two connected surfaces.
            val intervals=connected.mapNotNull{s->
                if(s.facesNear(a,2.0).none{(p,n)->n.dot(faceNormal)>.999 && abs((a-p).dot(n))<1e-5 && abs((b-p).dot(n))<1e-5}) null
                else s.span(a,b)
            }.sortedBy{it.first}
            var covered=0.0
            for((start,end) in intervals) {if(start>covered+1e-6)break;covered=max(covered,end)}
            if(covered<1-1e-6) return
            val from=a.surfacePlus(faceNormal.scaled(.003));val to=b.surfacePlus(faceNormal.scaled(.003))
            // Also reject a face hidden inside another solid (e.g. floor underneath a step).
            if(connected.any{s->s.span(from,to)?.let{(lo,hi)->hi-lo>1e-6 && s.contains(from.lerp(to,(lo+hi)/2),-1e-7)}==true}) return
            result.add(ContactLine(from,to,0xFFFFB347.toInt(),width))
        }
        for(i in 0 until 16) {
            val aa=2*PI*i/16;val bb=2*PI*(i+1)/16
            val a=project(aa) ?: continue;val b=project(bb) ?: continue
            if(a.normal.dot(b.normal)>.999) add(a.position,b.position,a.normal)
            else {
                var lo=aa;var hi=bb;var left=a;var right=b
                repeat(14) {
                    val mid=(lo+hi)/2;val h=project(mid)
                    if(h!=null && h.normal.dot(a.normal)>.999) {lo=mid;left=h} else {hi=mid;if(h!=null)right=h}
                }
                // Split exactly at the face transition; never draw a chord through a corner.
                val seam=left.position.lerp(right.position,.5)
                if(left.position.distanceSquared(right.position)<1e-6) {add(a.position,seam,a.normal);add(seam,b.position,b.normal)}
            }
        }
        return result.toList()
    }
}
