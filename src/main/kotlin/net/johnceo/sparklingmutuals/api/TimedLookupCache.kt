package net.johnceo.sparklingmutuals.api

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/** Successful reads only; concurrent requests share work and HTTP never holds the cache lock. */
class TimedLookupCache<K,V>(private val ttl: Long = 60*60_000L, private val clock: () -> Long = System::currentTimeMillis) {
    private class Entry<V>(var value: V?=null, var at: Long=0, var pending: CompletableFuture<V>?=null)
    private val entries=mutableMapOf<K,Entry<V>>()
    @Synchronized fun peek(key: K): V?=entries[key]?.value
    @Synchronized fun put(key: K,value: V) { entries[key]=Entry(value,clock()) }
    @Synchronized fun clear() { entries.clear() }
    fun load(key: K, fetch: () -> V): V {
        val entry: Entry<V>;val future: CompletableFuture<V>;val owner: Boolean
        synchronized(this) {
            val now=clock()
            entries.entries.removeIf { it.key != key && it.value.pending == null && now-it.value.at >= ttl }
            entry=entries.getOrPut(key) { Entry() }
            entry.value?.takeIf { now-entry.at < ttl }?.let { return it }
            owner=entry.pending == null
            future=entry.pending ?: CompletableFuture<V>().also { entry.pending=it }
        }
        if (owner) {
            try {
                val result=fetch()
                synchronized(this) { entry.value=result;entry.at=clock();entry.pending=null }
                future.complete(result)
            } catch (failure: Throwable) {
                synchronized(this) { entry.pending=null }
                future.completeExceptionally(failure)
                throw failure
            }
        }
        return try { future.join() } catch (failure: CompletionException) { throw failure.cause ?: failure }
    }
}
