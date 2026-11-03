package net.johnceo.sparklingmutuals.skyblock.market

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Latest private snapshot only; foreground offers retry unchanged state after bounded elapsed backoff. */
internal class WatchSaveQueue(private val executor: Executor,private val write: (String)->Boolean,
    private val elapsed: ()->Long={System.nanoTime()/1_000_000}) {
    private val pending=AtomicReference<String?>()
    private val queued=AtomicBoolean(false)
    @Volatile private var acknowledged: String?=null
    @Volatile private var retryAt=Long.MIN_VALUE
    private var failures=0
    fun offer(raw: String) {
        pending.set(raw)
        if(raw==acknowledged) {if(!queued.get())pending.compareAndSet(raw,null);return}
        if(elapsed()<retryAt || !queued.compareAndSet(false,true))return
        try {executor.execute {
            try {
                val latest=pending.getAndSet(null) ?: return@execute
                if(latest==acknowledged)return@execute
                if(runCatching {write(latest)}.getOrDefault(false)) {
                    acknowledged=latest;failures=0;retryAt=Long.MIN_VALUE
                } else {
                    pending.compareAndSet(null,latest)
                    failures=(failures+1).coerceAtMost(7)
                    val delay=minOf(900_000L,15_000L shl (failures-1))
                    retryAt=runCatching {Math.addExact(elapsed(),delay)}.getOrDefault(Long.MAX_VALUE)
                }
            } finally {queued.set(false)}
        }} catch(_: RuntimeException) {
            queued.set(false)
            retryAt=runCatching {Math.addExact(elapsed(),15_000L)}.getOrDefault(Long.MAX_VALUE)
        }
    }
}
