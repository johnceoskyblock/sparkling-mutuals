package net.johnceo.sparklingmutuals.compat

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.minecraft.world.phys.Vec3

object CapsuleGeometryCompat {
    fun vector(v: Vec3) = ShotVector(v.x, v.y, v.z)
}
