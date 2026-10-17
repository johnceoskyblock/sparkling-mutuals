package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
object ContactIntersections {
    /** Closed slab intersection; a degenerate body is never promoted to a hitbox. */
    fun entry(from:ShotVector,to:ShotVector,bounds:ShotBounds):Double? {
        if(!from.finite() || !to.finite() || !bounds.valid()) return null
        var near=0.0; var far=1.0
        val starts=doubleArrayOf(from.x,from.y,from.z); val ends=doubleArrayOf(to.x,to.y,to.z)
        val mins=doubleArrayOf(bounds.min.x,bounds.min.y,bounds.min.z); val maxs=doubleArrayOf(bounds.max.x,bounds.max.y,bounds.max.z)
        for(i in 0..2) {
            val delta=ends[i]-starts[i]
            if(delta==0.0) { if(starts[i]<mins[i] || starts[i]>maxs[i]) return null }
            else {
                val a=(mins[i]-starts[i])/delta; val b=(maxs[i]-starts[i])/delta
                near=maxOf(near,minOf(a,b)); far=minOf(far,maxOf(a,b))
                if(near>far) return null
            }
        }
        return near.takeIf { it in 0.0..1.0 }
    }
}
