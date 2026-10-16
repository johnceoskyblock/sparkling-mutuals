package net.johnceo.sparklingmutuals.compat

import net.minecraft.world.phys.Vec3
import kotlin.math.hypot

data class RenderLine(val from: Vec3, val to: Vec3, val normal: Vec3)

/** Detached, float-safe camera-relative geometry for immediate or deferred submission. */
object WorldLineGeometry {
    private fun relative(point: Vec3, camera: Vec3): Vec3? {
        val x=(point.x-camera.x).toFloat(); val y=(point.y-camera.y).toFloat(); val z=(point.z-camera.z).toFloat()
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
        return Vec3(x.toDouble(), y.toDouble(), z.toDouble())
    }
    fun segments(worldPoints: List<Vec3>, camera: Vec3): List<RenderLine> {
        if (!camera.x.isFinite() || !camera.y.isFinite() || !camera.z.isFinite()) return emptyList()
        val points=worldPoints.take(64).map { relative(it,camera) }
        return points.zipWithNext().mapNotNull { (from,to) ->
            if (from==null || to==null) return@mapNotNull null
            val dx=to.x-from.x; val dy=to.y-from.y; val dz=to.z-from.z
            val length=hypot(hypot(dx,dy),dz)
            if (!length.isFinite() || length==0.0) null else RenderLine(from,to,Vec3(dx/length,dy/length,dz/length))
        }
    }
}
