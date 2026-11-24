package net.johnceo.sparklingmutuals.alerts

/** One expiry action for the newest timer; cancellation and stale contexts cannot fire a warp. */
class WarpAlertTimer {
    private var generation = 0L
    private var pending = false
    private var context: Any? = null
    private var lastTransfer: Long? = null
    fun arm(context: Any? = null): Long { this.context = context; pending = true; lastTransfer = null; return ++generation }
    fun cancel() { generation++; pending = false; context = null; lastTransfer = null }
    fun traveled(token: Long, now: Long) {
        if (pending && token == generation) lastTransfer = now
    }
    fun dueAt(normalDue: Long, startedAt: Long): Long {
        val lastSafeAt = startedAt + SparkleWarpPolicy.WARP_WINDOW_MILLIS - 250
        val transferDue = lastTransfer?.let { minOf(it + 2_000L, lastSafeAt) } ?: normalDue
        return maxOf(normalDue, transferDue)
    }
    fun consume(token: Long, ready: Boolean, context: Any? = null): Boolean {
        if (!pending || token != generation) return false
        pending = false
        return ready && this.context === context
    }
}
