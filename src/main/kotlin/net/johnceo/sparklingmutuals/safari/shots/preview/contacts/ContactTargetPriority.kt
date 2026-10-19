package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.ShotVector

/** Retention heuristic only: distance from body center to a coarse current curve, never coverage evidence. */
class ContactTargetPriority(curve:ContactCurve) {
    private val points=curve.nodes.take(257).filterIndexed{i,_->i%8==0 || i==curve.nodes.lastIndex}.map{it.position}
    fun score(target:ContactTarget):Double {
        if(points.size<2 || points.any{!it.finite()} || !target.bounds.valid()) return Double.POSITIVE_INFINITY
        val b=target.bounds
        val center=ShotVector(b.min.x/2+b.max.x/2,b.min.y/2+b.max.y/2,b.min.z/2+b.max.z/2)
        var closest=Double.POSITIVE_INFINITY
        for((a,b) in points.zipWithNext()) {
            val dx=b.x-a.x;val dy=b.y-a.y;val dz=b.z-a.z
            val length=dx*dx+dy*dy+dz*dz
            val f=if(length>0) (((center.x-a.x)*dx+(center.y-a.y)*dy+(center.z-a.z)*dz)/length).coerceIn(0.0,1.0) else 0.0
            closest=minOf(closest,center.distanceSquared(a.lerp(b,f)))
        }
        return closest
    }
}
