package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

/** Bounded, client-thread-only identities awaiting an updated descriptor inspection. */
class ContactMetadataQueue {
    private val entries=linkedMapOf<String,ContactDiscovery>()
    val size:Int get()=entries.size
    fun add(discovery:ContactDiscovery):Boolean {
        if(discovery.key !in entries && entries.size==64) return false
        entries[discovery.key]=discovery
        return true
    }
    fun drain(limit:Int):List<ContactDiscovery> {
        require(limit in 0..16)
        val batch=entries.values.take(limit)
        batch.forEach{entries.remove(it.key)}
        return batch
    }
    fun reset()=entries.clear()
}
