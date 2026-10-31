package net.johnceo.sparklingmutuals.alerts

/** Queue one reminder per new contest, including while the leader's party is disbanded. */
class WarpContestReminder {
    private var window: Long? = null
    private var pending: Long? = null
    fun poll(current: Long, enabled: Boolean, leader: Boolean?, members: Int): Boolean {
        if (window != current) {
            pending = current.takeIf { window != null && enabled && leader != false }
            window = current
        }
        if (!enabled || leader == false) pending = null
        if (pending != current || leader != true || members < 2) return false
        pending = null
        return true
    }
    fun reset() { window = null; pending = null }
}
