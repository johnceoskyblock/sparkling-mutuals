package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
import net.johnceo.sparklingmutuals.safari.shots.preview.CapsulePathModel
import kotlin.math.*

/** Spatial extrapolation of the frozen curve, not a flight-time or collision model. */
object ContactCurveSampler {
    const val HYPOTHESIS = "contact-guide-center-extrapolation-v1"
    fun sample(eye: ShotVector, aim: ShotVector, pitch: Double): ContactCurve {
        val magnitude=hypot(hypot(aim.x,aim.y),aim.z)
        fun invalid()=ContactCurve(emptyList(),ContactStop.INVALID,HYPOTHESIS)
        if(!eye.finite() || !aim.finite() || !pitch.isFinite() || pitch !in -90.0..90.0 || !magnitude.isFinite() || magnitude==0.0) return invalid()
        val direction=ShotVector(aim.x/magnitude,aim.y/magnitude,aim.z/magnitude)
        fun point(n:Double)=ShotVector(eye.x+CapsulePathModel.SPEED*n*direction.x,
            eye.y+CapsulePathModel.SPEED*n*direction.y+.5*CapsulePathModel.VERTICAL_ACCELERATION*n*(n-1),
            eye.z+CapsulePathModel.SPEED*n*direction.z)
        fun node(n:Double)=ContactNode(n,point(n),pitch in -10.0..1.0 && n in 2.0..CapsulePathModel.endParameter)
        val nodes=ArrayList<ContactNode>(257); nodes.add(node(0.0))
        var n=0.0; var length=0.0; var attempts=0
        while(nodes.size<=256 && attempts<4096) {
            val boundary=when { n<2.0 -> 2.0; n<CapsulePathModel.endParameter -> CapsulePathModel.endParameter; else -> 128.0 }
            var next=min(n+.5,boundary)
            var p=point(next)
            while(sqrt(nodes.last().position.distanceSquared(p))>.5 || abs(CapsulePathModel.VERTICAL_ACCELERATION)*(next-n).pow(2)/8>.01) {
                if(++attempts>=4096) return ContactCurve(nodes.toList(),ContactStop.WORK_LIMIT,HYPOTHESIS)
                next=(n+next)/2; p=point(next)
            }
            attempts++
            if(!p.finite() || next<=n) return invalid()
            val chord=sqrt(nodes.last().position.distanceSquared(p))
            if(length+chord>=64.0) {
                // Bisection stays on the parabola while approaching the remaining polyline budget from below.
                var lo=n; var hi=next
                repeat(48) { val mid=(lo+hi)/2; if(sqrt(nodes.last().position.distanceSquared(point(mid)))<=64-length) lo=mid else hi=mid }
                if(lo>n) nodes.add(node(lo))
                return ContactCurve(nodes.toList(),ContactStop.RANGE_LIMIT,HYPOTHESIS)
            }
            nodes.add(node(next)); length+=chord; n=next
            if(n>=128.0) return ContactCurve(nodes.toList(),ContactStop.ORDINAL_LIMIT,HYPOTHESIS)
        }
        return ContactCurve(nodes.toList(),ContactStop.WORK_LIMIT,HYPOTHESIS)
    }
}
