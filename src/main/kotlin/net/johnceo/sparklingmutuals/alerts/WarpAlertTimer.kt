package net.johnceo.sparklingmutuals.alerts

/** One expiry action for the newest timer; cancellation and stale contexts cannot fire a warp. */
class WarpAlertTimer {
    private var generation = 0L
    private var pending = false
    private var context: Any? = null
    fun arm(context: Any? = null): Long { this.context = context; pending = true; return ++generation }
    fun cancel() { generation++; pending = false; context = null }
    fun consume(token: Long, ready: Boolean, context: Any? = null): Boolean {
        if (!pending || token != generation) return false
        pending = false
        return ready && this.context === context
    }
}
