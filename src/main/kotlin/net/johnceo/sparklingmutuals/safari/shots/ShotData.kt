package net.johnceo.sparklingmutuals.safari.shots

data class ShotVector(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: ShotVector) = ShotVector(x - other.x, y - other.y, z - other.z)
    fun distanceSquared(other: ShotVector): Double = (x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z)
}
data class ShotBounds(val min: ShotVector, val max: ShotVector)
enum class ShotVariant(val itemId: String) { ORDINARY("CRITTER_CAPSULE"), MASTERFUL("MASTERFUL_CRITTER_CAPSULE");
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.itemId == id } }
}
