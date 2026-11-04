package net.johnceo.sparklingmutuals.sync

import java.util.Collections

internal data class PartyRoomSnapshot(val generation: Long, val active: Set<String>)

/** Current socket positives, visit-scoped negatives. No sighting-age claim. Client thread only. */
internal class PartyRoomDiscoveries {
    private var generation = -1L
    private var atomic = false
    private var ready = false
    private val caught = IntArray(2)
    private var active = emptySet<String>()
    fun welcome(generation: Long, atomic: Boolean) {
        this.generation=generation;this.atomic=atomic;ready=false;active=emptySet()
    }
    fun receive(generation: Long, payload: String): Boolean {
        if(generation!=this.generation || !atomic) return false
        val facts=PartySyncProtocol.numbers("S",payload)
        if(facts==null) { ready=false;active=emptySet();return false }
        caught[0]=caught[0] or facts[2];caught[1]=caught[1] or facts[3]
        active=Collections.unmodifiableSet(PartySyncProtocol.species.filterIndexed { i,_ ->
            facts[if(i<30) 0 else 1] and caught[if(i<30) 0 else 1].inv() and (1 shl(i%30)) != 0
        }.toSet())
        ready=true
        return true
    }
    fun snapshot(generation: Long): PartyRoomSnapshot? =
        if(ready && atomic && generation==this.generation) PartyRoomSnapshot(generation,active) else null
    fun reset() { generation=-1;atomic=false;ready=false;caught.fill(0);active=emptySet() }
}
