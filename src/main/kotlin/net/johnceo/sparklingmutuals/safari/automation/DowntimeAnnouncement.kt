package net.johnceo.sparklingmutuals.safari.automation

/** Stagger clients and cancel on hearing the same report, including before returning from the run. */
class DowntimeAnnouncement {
    private val observed = mutableSetOf<String>()
    private var pending: String? = null
    private var due = 0L
    fun observe(message: String) { if (message.startsWith("Downtime requested: ")) observed.add(message) }
    fun queue(message: String, now: Long, rank: Int) {
        pending = message
        due = now + 500 + rank * 1200L
    }
    fun poll(now: Long, current: String?): String? {
        val message = pending ?: return null
        if (current == null) { pending = null; return null }
        // A resume/ticket/party change during the delay must not announce stale requests.
        if (message != current) { pending = current; due = now + 500; return null }
        if (now < due) return null
        pending = null
        return message.takeIf { observed.add(it) }
    }
    fun clear() { pending = null; observed.clear() }

    companion object {
        /** Fit one party-chat line, sharing the remaining space fairly between long reasons. */
        fun format(requests: Map<String, String>): String {
            val prefix = "Downtime requested: "
            val fixed = prefix.length + requests.keys.sumOf { it.length + 3 } + (requests.size - 1).coerceAtLeast(0) * 2
            val budget = ((252 - fixed) / requests.size.coerceAtLeast(1)).coerceAtLeast(3)
            return prefix + requests.entries.joinToString("; ") { (name, reason) ->
                val detail = reason.ifBlank { "no reason" }
                "$name (${if (detail.length <= budget) detail else detail.take(budget - 3) + "..."})"
            }
        }
    }
}
