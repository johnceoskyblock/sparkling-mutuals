package net.johnceo.sparklingmutuals.safari.esp

/** Matches confirmed server captures to recent aimed entities without guessing identities. */
class EspCaptureMemory {
    private data class Attempt(val id: java.util.UUID, val species: String, val at: Long)
    private val pending = mutableListOf<Attempt>()
    private val labelOwners = mutableMapOf<java.util.UUID, Pair<String, java.util.UUID>>()
    /** Preserve observed pity through body/form replacements only when the exact label identity survives. */
    fun bindLabel(id: java.util.UUID, species: String, label: java.util.UUID, liveBodies: Set<java.util.UUID>) {
        val previous=labelOwners[label]
        if(previous!=null && previous.second!=id && previous.second in liveBodies)return
        if(previous!=null && previous.first==species && previous.second!=id && previous.second !in liveBodies) {
            val old=previous.second
            pity[id]=maxOf(pityFor(id),pityFor(old))
            pity.remove(old)
            for(index in pending.indices) if(pending[index].id==old) pending[index]=pending[index].copy(id=id)
            recent.entries.forEach {if(it.value.id==old)it.setValue(it.value.copy(id=id))}
            for(index in input.indices) input[index]=input[index].copy(aim=input[index].aim.mapValues {(_,aim)->if(aim.id==old)aim.copy(id=id) else aim})
        }
        labelOwners[label]=species to id
    }
    private val pity = mutableMapOf<java.util.UUID, Int>()
    fun pityFor(id: java.util.UUID) = pity[id] ?: 0
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
        // A new server-confirmed throw at this body also proves the previous attempt ended,
        // even if we missed its escape message.
        aimed(attempt.id, species, now, confirmedThrow = true)
    }
    private fun expire(now: Long) { pending.removeAll { now - it.at !in 0..60000 } }
    fun aimed(id: java.util.UUID, species: String, now: Long, confirmedThrow: Boolean = false) {
        expire(now)
        if (hidden(id)) return
        val index = pending.indexOfFirst { it.id == id }
        if (index < 0 || confirmedThrow) pity[id] = pityFor(id) + 1
        if (index < 0) pending.add(Attempt(id, species, now))
        else pending[index] = Attempt(id, species, now)
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
    fun retire(id: java.util.UUID) {
        captured.add(id)
        pending.removeAll { it.id == id }
        recent.entries.removeAll { it.value.id == id }
    }
    fun hidden(id: java.util.UUID) = id in captured
    fun reset() { pending.clear(); labelOwners.clear(); pity.clear(); captured.clear(); recent.clear(); input.clear() }
}
