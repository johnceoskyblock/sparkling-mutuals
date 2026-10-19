package net.johnceo.sparklingmutuals.safari.shots

/** Recorder v2 policy. Priority controls observation density, never ownership or collision. */
internal class ShotTargetSampler {
    private var broad:Long?=null
    private var focused:Long?=null
    private val previous=mutableMapOf<String,ShotObservation.Target>()
    fun reset() { broad=null;focused=null;previous.clear() }
    fun sample(stamp:ShotStamp,pose:ShotPose,capsulePositions:List<ShotVector>,
        targets:List<ShotObservation.Target>,force:Boolean=false):List<ShotObservation.Target> {
        val eligible=targets.take(64).filter { target ->
            require(listOf(target.position,target.bounds.min,target.bounds.max).all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
            eligible(target.position,pose,capsulePositions)
        }
        previous.keys.retainAll(eligible.map { it.token }.toSet())
        val result=if(force || broad==null || stamp.micros-broad!!>=2_000_000) {
            broad=stamp.micros;focused=stamp.micros;eligible
        } else if(focused==null || stamp.micros-focused!!>=200_000) {
            focused=stamp.micros
            eligible.map { it to priority(it,pose,capsulePositions) }
                .filter { it.first.token==pose.crosshair?.target || it.second<=64.0 }
                .sortedWith(compareBy<Pair<ShotObservation.Target,Double>> { if(it.first.token==pose.crosshair?.target) 0 else 1 }
                    .thenBy { it.second }.thenBy { it.first.token })
                .take(8).map { it.first }.filter { previous[it.token]!=it }
        } else emptyList()
        result.forEach { previous[it.token]=it }
        return result
    }
    companion object {
        fun eligible(position:ShotVector,pose:ShotPose,capsules:List<ShotVector>)=
            position.distanceSquared(pose.eye)<=4096 || capsules.any { it.distanceSquared(position)<=4096 }
    }
    private fun priority(target:ShotObservation.Target,pose:ShotPose,capsules:List<ShotVector>):Double {
        val d=target.position-pose.eye;val a=pose.aim
        val length=a.x*a.x+a.y*a.y+a.z*a.z
        val along=if(length>0) ((d.x*a.x+d.y*a.y+d.z*a.z)/length).coerceIn(0.0,64.0) else 0.0
        val point=ShotVector(pose.eye.x+a.x*along,pose.eye.y+a.y*along,pose.eye.z+a.z*along)
        return minOf(target.position.distanceSquared(point),capsules.minOfOrNull { it.distanceSquared(target.position) } ?: Double.POSITIVE_INFINITY)
    }
}
