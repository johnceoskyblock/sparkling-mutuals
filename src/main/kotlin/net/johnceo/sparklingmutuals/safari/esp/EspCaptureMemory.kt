package net.johnceo.sparklingmutuals.safari.esp

/** Matches confirmed server captures to recent aimed entities without guessing identities. */
class EspCaptureMemory {
    private data class Attempt(val id: java.util.UUID, val species: String, val at: Long)
    private val pending = mutableListOf<Attempt>()
    private val captured = mutableSetOf<java.util.UUID>()
    private val recent = mutableMapOf<String, Attempt>()
    private data class Throw(val at: Long, val aim: Map<String, Attempt>)
    private val input = mutableListOf<Throw>()
    fun sighted(id: java.util.UUID, species: String, now: Long) {
        if (!hidden(id)) recent[species] = Attempt(id, species, now)
    }
    /** Freeze aim at item use, before a capsule impact or camera turn changes the scan. */
    fun throwing(now: Long) {
        input.removeAll { now - it.at !in 0..3000 }
        val aim = recent.filterValues { now - it.at in 0..250 && !hidden(it.id) }
        if (aim.isNotEmpty()) input.add(Throw(now, aim))
    }
    fun threw(species: String, now: Long) {
        input.removeAll { now - it.at !in 0..3000 }
        val index = input.indexOfFirst { it.aim[species]?.let { aim -> !hidden(aim.id) } == true }
        // A server species name alone cannot identify a nearby UUID after the camera has moved.
        val attempt = if (index >= 0) input.removeAt(index).aim.getValue(species) else return
        aimed(attempt.id, species, now)
    }
    private fun expire(now: Long) { pending.removeAll { now - it.at !in 0..60000 } }
    fun aimed(id: java.util.UUID, species: String, now: Long) {
        expire(now)
        if (hidden(id)) return
        val index = pending.indexOfFirst { it.id == id }
        if (index < 0) pending.add(Attempt(id, species, now)) else pending[index] = Attempt(id, species, now)
    }
    fun escaped(species: String, now: Long): java.util.UUID? {
        expire(now)
        val attempt = pending.firstOrNull { it.species == species } ?: return null
        pending.remove(attempt)
        return attempt.id
    }
    fun caught(species: String, now: Long): java.util.UUID? {
        expire(now)
        val attempt = pending.firstOrNull { it.species == species } ?: return null
        pending.remove(attempt); captured.add(attempt.id)
        recent.entries.removeAll { it.value.id == attempt.id }
        return attempt.id
    }
    fun hidden(id: java.util.UUID) = id in captured
    fun reset() { pending.clear(); captured.clear(); recent.clear(); input.clear() }
}
