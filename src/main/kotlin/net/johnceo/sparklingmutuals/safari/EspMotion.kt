package net.johnceo.sparklingmutuals.safari

import java.util.UUID

/** Reversible exclusion for captured moving models that the server leaves loaded. */
class EspMotion {
    private data class Sample(val x: Double, val y: Double, val z: Double, val since: Long, val nearby: Boolean, val hidden: Boolean = false)
    private val samples = mutableMapOf<UUID, Sample>()
    private val moving = setOf("Driftling", "Foxtrot", "Bluebird", "Parakeet", "Macaw", "Solsnatcher",
        "Litterbug", "Tepid", "Mantis Shrimp", "Nozzlenose", "Gemzie", "Shuddersquid")
    fun observe(id: UUID, species: String, x: Double, y: Double, z: Double, distanceSquared: Double, now: Long) {
        if (species !in moving) { samples.remove(id); return }
        val old = samples[id]
        val nearby = distanceSquared <= 900
        val moved = old == null || (x - old.x) * (x - old.x) + (y - old.y) * (y - old.y) +
            (z - old.z) * (z - old.z) > .000001
        samples[id] = when {
            moved -> Sample(x, y, z, now, nearby)
            !current(id, now) -> old.copy(nearby = nearby, hidden = true)
            !nearby || !old.nearby -> Sample(x, y, z, now, nearby)
            else -> old
        }
    }
    fun current(id: UUID, now: Long) = samples[id]?.let { !it.hidden && (!it.nearby || now - it.since < 2000) } ?: true
    fun retain(ids: Set<UUID>) { samples.keys.retainAll(ids) }
    fun reset() = samples.clear()
}
