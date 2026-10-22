package net.johnceo.sparklingmutuals.api

import java.util.concurrent.CompletableFuture
import java.util.Locale

/** Retain discoveries for unchanged party members. Never hold the lock during HTTP or wait on the client thread. */
class PartyDiscoveryCache {
    private class Entry {
        var value: Set<String>? = null
        val earned = mutableSetOf<String>()
        var pending: CompletableFuture<Set<String>>? = null
    }
    private var provider: ApiProvider? = null
    private var members = emptySet<String>()
    private val entries = mutableMapOf<String, Entry>()
    private fun id(uuid: String) = uuid.replace("-", "").lowercase(Locale.ROOT)
    private fun key(species: String) = species.uppercase(Locale.ROOT).replace(' ', '_')

    @Synchronized fun select(roster: Set<String>, source: ApiProvider) {
        if (provider != source) { entries.clear(); provider = source }
        members = roster.map(::id).toSet()
        entries.keys.retainAll(members)
    }

    @Synchronized fun known(uuid: String, source: ApiProvider): Set<String>? =
        if (provider != source) null else entries[id(uuid)]?.let { entry -> entry.value?.plus(entry.earned) }

    /** Returns pre-catch discovery sets for history, then grants the species to every participating member. */
    @Synchronized fun caught(roster: Set<String>, species: String, source: ApiProvider, participants: Set<String> = roster): Map<String, Set<String>?> {
        select(roster, source)
        val before = roster.associateWith { known(it, source) }
        participants.map(::id).filter { it in members }.forEach { entries.getOrPut(it, ::Entry).earned.add(key(species)) }
        return before
    }

    fun load(uuid: String, source: ApiProvider, fetch: () -> Set<String>): Set<String> {
        val member = id(uuid)
        val entry: Entry
        val future: CompletableFuture<Set<String>>
        val owner: Boolean
        synchronized(this) {
            check(provider == source && member in members) { "Party changed during discovery lookup" }
            entry = entries.getOrPut(member, ::Entry)
            entry.value?.let { return it + entry.earned }
            owner = entry.pending == null
            future = entry.pending ?: CompletableFuture<Set<String>>().also { entry.pending = it }
        }
        if (owner) {
            try {
                val read = fetch().map(::key).toSet()
                val merged = synchronized(this) {
                    entry.value = read
                    entry.pending = null
                    read + entry.earned
                }
                future.complete(merged)
            } catch (error: Throwable) {
                synchronized(this) { entry.pending = null }
                future.completeExceptionally(error)
                throw error
            }
        }
        // All callers of load run on lookup workers; catches only use the nonblocking methods above.
        return try { future.join() } catch (error: java.util.concurrent.CompletionException) {
            throw (error.cause ?: error)
        }
    }

    @Synchronized fun clear() { entries.clear(); members = emptySet(); provider = null }
}
