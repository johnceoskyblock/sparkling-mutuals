package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules

/** Last-seen packet origins, not entity identities or a count of distinct critters. */
class SparklingParticleLocations {
    data class Location(val id: UUID, var x: Double, var y: Double, var z: Double,
        val biome: SafariBiome, var seenAt: Long, var identified: Boolean = false) {
        fun distanceSquared(x: Double, y: Double, z: Double) =
            (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) + (this.z - z) * (this.z - z)
    }
    private val locations = mutableListOf<Location>()
    fun observe(inSafari: Boolean, waxOn: Boolean, x: Double, y: Double, z: Double, now: Long): Location? {
        if (!inSafari || !waxOn || !x.isFinite() || !y.isFinite() || !z.isFinite() || y !in 0.0..160.0) return null
        val biome = SafariEspRules.biomeAt(x, z) ?: return null
        val old = locations.filter { it.biome == biome && it.distanceSquared(x, y, z) <= 9.0 }
            .minByOrNull { it.distanceSquared(x, y, z) }
        if (old != null) {
            old.x = x; old.y = y; old.z = z; old.seenAt = now
            return old
        }
        if (locations.size >= 128) return null
        return Location(UUID.randomUUID(), x, y, z, biome, now).also(locations::add)
    }
    /** Only independently identified name/skin positives retire anonymous waypoints. */
    fun identify(x: Double, y: Double, z: Double): List<UUID> =
        locations.filter { it.distanceSquared(x, y, z) <= 9.0 }.onEach { it.identified = true }.map { it.id }
    fun unknown() = locations.filterNot { it.identified }
}
