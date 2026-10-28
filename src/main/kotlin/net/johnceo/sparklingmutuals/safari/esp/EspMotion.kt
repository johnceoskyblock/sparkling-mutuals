package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID

/** Reversible exclusion for captured moving models that the server leaves loaded. */
class EspMotion {
    private data class Sample(val x: Double, val y: Double, val z: Double, val since: Long, val nearby: Boolean, val hidden: Boolean = false, val moved: Boolean = false, val movedAt: Long? = null)
    private val samples = mutableMapOf<UUID, Sample>()
    companion object {
        val movingSpecies = setOf("Shyworm", "Flitter", "Driftling", "Foxtrot", "Bluebird", "Parakeet", "Macaw", "Solsnatcher",
            "Litterbug", "Tepid", "Mantis Shrimp", "Nozzlenose", "Gemzie", "Shuddersquid")
    }
    fun observe(id: UUID, species: String, x: Double, y: Double, z: Double, distanceSquared: Double, now: Long) {
        if (species !in movingSpecies) { samples.remove(id); return }
        val old = samples[id]
        val nearby = distanceSquared <= 900
        val moved = old == null || (x - old.x) * (x - old.x) + (y - old.y) * (y - old.y) +
            (z - old.z) * (z - old.z) > .000001
        samples[id] = when {
            moved -> Sample(x, y, z, now, nearby, moved = old != null, movedAt = now.takeIf { old != null })
            !current(id, now) -> old.copy(nearby = nearby, hidden = true)
            !nearby || !old.nearby -> Sample(x, y, z, now, nearby, moved = old.moved, movedAt = old.movedAt)
            else -> old
        }
    }
    /** Position evidence expires even outside thirty blocks; retained captured heads can remain loaded there. */
    fun recentMovement(id: UUID, now: Long) = samples[id]?.let { it.movedAt?.let { at -> now - at in 0 until 2000 } == true } == true
    fun movementObserved(id: UUID) = samples[id]?.moved == true
    fun current(id: UUID, now: Long) = samples[id]?.let { !it.hidden && (!it.nearby || now - it.since < 2000) } ?: true
    fun retain(ids: Set<UUID>) { samples.keys.retainAll(ids) }
    fun reset() = samples.clear()
}
