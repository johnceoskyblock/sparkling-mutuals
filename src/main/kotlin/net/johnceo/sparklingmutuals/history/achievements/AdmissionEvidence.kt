package net.johnceo.sparklingmutuals.history.achievements

import net.johnceo.sparklingmutuals.history.TicketCounter

/** Client-thread correlator. A Manager line alone is not proof of a spent ticket. */
class AdmissionEvidence(
    private val observeEntry: (String, AchievementScope?, Long) -> Unit = { _, _, _ -> },
    private val emit: (String, AchievementScope?, Long) -> Unit
) {
    private data class Entry(val id: String, val scope: AchievementScope?, val at: Long, var bound: Boolean = false)
    private var entry: Entry? = null
    private var pendingAt: Long? = null

    fun acknowledge(text: String, at: Long) {
        if (!TicketCounter.isTicket(text)) return
        val current = entry
        if (current != null) {
            // A repeated line for an active run cannot become a ticket for the next run.
            if (!current.bound && at - current.at in 0..120000) bind(current, at)
            return
        }
        // Duplicate variants do not extend the pending acknowledgement's lifetime.
        if (pendingAt == null || at - pendingAt!! !in 0..120000) pendingAt = at
    }

    fun confirmEntry(runId: String, scope: AchievementScope, at: Long): Boolean = confirm(runId, scope, at)
    fun confirmUnscopedEntry(runId: String, at: Long): Boolean = confirm(runId, null, at)

    private fun confirm(runId: String, scope: AchievementScope?, at: Long): Boolean {
        if (runId.isBlank() || entry?.id == runId) return false
        val current = Entry(runId, scope, at)
        entry = current
        // Suspend the upper-bound proof before any intervening own-success callback.
        observeEntry(runId, scope, at)
        val pending = pendingAt
        pendingAt = null
        return if (pending != null && at - pending in 0..120000) bind(current, at) else false
    }

    private fun bind(current: Entry, at: Long): Boolean {
        current.bound = true
        emit(current.id, current.scope, at)
        return true
    }
    /** Leaving clears the active association, while an acknowledgement made between runs remains pending. */
    fun leave() { entry = null }
    /** Connection gaps cannot carry a pending acknowledgement into a later session. */
    fun disconnect() { entry = null; pendingAt = null }
}
