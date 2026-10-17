package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import kotlin.math.*
internal fun ShotVector.surfacePlus(b:ShotVector)=ShotVector(x+b.x,y+b.y,z+b.z)
internal fun ShotVector.scaled(s:Double)=ShotVector(x*s,y*s,z*s)
internal fun ShotVector.dot(b:ShotVector)=x*b.x+y*b.y+z*b.z
internal fun ShotVector.cross(b:ShotVector)=ShotVector(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
internal fun ShotVector.unit():ShotVector?=sqrt(dot(this)).takeIf{it.isFinite() && it>1e-10}?.let{scaled(1/it)}
data class ContactSurfaceHit(val fraction:Double,val position:ShotVector,val normal:ShotVector)
/** Affine unit box. Preserves display rotation/shear instead of colliding with a larger world AABB. */
data class ContactSurfaceBox(val origin:ShotVector,val x:ShotVector,val y:ShotVector,val z:ShotVector) {
    private val determinant=x.dot(y.cross(z))
    fun valid()=listOf(origin,x,y,z).all{it.finite()} && determinant.isFinite() && abs(determinant)>1e-10
    private fun inverse()=listOf(y.cross(z).scaled(1/determinant),z.cross(x).scaled(1/determinant),x.cross(y).scaled(1/determinant))
    fun local(p:ShotVector):ShotVector { val a=inverse();val d=p-origin;return ShotVector(a[0].dot(d),a[1].dot(d),a[2].dot(d)) }
    fun world(p:ShotVector)=origin.surfacePlus(x.scaled(p.x)).surfacePlus(y.scaled(p.y)).surfacePlus(z.scaled(p.z))
    fun contains(p:ShotVector,tolerance:Double=1e-7):Boolean {
        if(!valid() || !p.finite()) return false
        val q=local(p);return listOf(q.x,q.y,q.z).all{it>=-tolerance && it<=1+tolerance}
    }
    fun vertices()=(0..7).map{i->world(ShotVector((i and 1).toDouble(),((i shr 1) and 1).toDouble(),((i shr 2) and 1).toDouble()))}
    fun bounds():ShotBounds { val v=vertices();return ShotBounds(ShotVector(v.minOf{it.x},v.minOf{it.y},v.minOf{it.z}),ShotVector(v.maxOf{it.x},v.maxOf{it.y},v.maxOf{it.z})) }
    fun span(from:ShotVector,to:ShotVector):Pair<Double,Double>? {
        if(!valid() || !from.finite() || !to.finite()) return null
        val a=local(from);val b=local(to);val starts=listOf(a.x,a.y,a.z);val ends=listOf(b.x,b.y,b.z)
        var enter=0.0;var exit=1.0
        for(i in 0..2) {
            val d=ends[i]-starts[i]
            if(abs(d)<1e-12) {if(starts[i]<-1e-6 || starts[i]>1+1e-6)return null;continue}
            enter=max(enter,min(-starts[i]/d,(1-starts[i])/d));exit=min(exit,max(-starts[i]/d,(1-starts[i])/d))
            if(enter>exit+1e-10) return null
        }
        return (enter to exit).takeIf{enter<=1 && exit>=0}
    }
    fun entry(from:ShotVector,to:ShotVector):ContactSurfaceHit? {
        if(!valid() || !from.finite() || !to.finite()) return null
        val a=local(from);val b=local(to);val start=listOf(a.x,a.y,a.z);val end=listOf(b.x,b.y,b.z)
        val axes=inverse();var enter=0.0;var exit=1.0;var normal=(from-to).unit() ?: return null
        for(i in 0..2) {
            val d=end[i]-start[i]
            if(abs(d)<1e-12) { if(start[i]<0 || start[i]>1) return null;continue }
            val t0=-start[i]/d;val t1=(1-start[i])/d
            val near=min(t0,t1);val far=max(t0,t1)
            if(near>enter) { enter=near;normal=axes[i].scaled(if(d>0)-1.0 else 1.0).unit() ?: return null }
            exit=min(exit,far);if(enter>exit+1e-10) return null
        }
        if(enter !in 0.0..1.0 || exit<0) return null
        return ContactSurfaceHit(enter,from.lerp(to,enter),normal)
    }
    /** Closest clamped point on each affine face and its outward normal. */
    fun facesNear(p:ShotVector,radius:Double):List<Pair<ShotVector,ShotVector>> {
        if(!valid()) return emptyList()
        val q=local(p);val base=listOf(q.x.coerceIn(0.0,1.0),q.y.coerceIn(0.0,1.0),q.z.coerceIn(0.0,1.0));val axes=inverse()
        return (0..2).flatMap{i->(0..1).mapNotNull{side->
            val v=base.toMutableList();v[i]=side.toDouble();val point=world(ShotVector(v[0],v[1],v[2]))
            if(point.distanceSquared(p)>radius*radius) null else axes[i].scaled(if(side==0)-1.0 else 1.0).unit()?.let{point to it}
        }}
    }
    fun touches(other:ContactSurfaceBox):Boolean {
        if(!valid() || !other.valid()) return false
        val a=bounds();val b=other.bounds()
        if(a.max.x<b.min.x-1e-7 || b.max.x<a.min.x-1e-7 || a.max.y<b.min.y-1e-7 || b.max.y<a.min.y-1e-7 || a.max.z<b.min.z-1e-7 || b.max.z<a.min.z-1e-7) return false
        fun edgesHit(s:ContactSurfaceBox,t:ContactSurfaceBox):Boolean {
            val vs=s.vertices()
            if(vs.any{t.contains(it)}) return true
            return (0..7).any{i->listOf(1,2,4).any{bit->i and bit==0 && t.entry(vs[i],vs[i or bit])!=null}}
        }
        return edgesHit(this,other) || edgesHit(other,this)
    }
    companion object {
        fun axis(b:ShotBounds)=ContactSurfaceBox(b.min,ShotVector(b.max.x-b.min.x,0.0,0.0),ShotVector(0.0,b.max.y-b.min.y,0.0),ShotVector(0.0,0.0,b.max.z-b.min.z))
    }
}
