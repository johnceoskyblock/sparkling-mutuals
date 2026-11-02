package net.johnceo.sparklingmutuals.safari.automation

/** One ticket reminder per request cycle, sent only by the leader in Torrhus Canyon. */
class DowntimeAnnouncement {
    private val observed = mutableSetOf<String>()
    private var pending: String? = null
    private var due = 0L
    fun observe(message: String) { if (message == TICKET_REMINDER || message.startsWith("Downtime requested: ")) observed.add(message) }
    fun queue(message: String, now: Long, rank: Int) {
        pending = message
        due = now + 500 + rank * 1200L
    }
    fun poll(now: Long, current: String?, leader: Boolean, inCanyon: Boolean): String? {
        val message = pending ?: return null
        if (current == null) { pending = null; return null }
        // A resume/ticket/party change during the delay must not announce stale requests.
        if (message != current) { pending = current; due = now + 500; return null }
        if (!leader || !inCanyon || now < due) return null
        pending = null
        return message.takeIf { observed.add(it) }
    }
    fun clear() { pending = null; observed.clear() }

    companion object {
        const val TICKET_REMINDER = "[Reminder] Everyone get your ticket!"
    }
}
