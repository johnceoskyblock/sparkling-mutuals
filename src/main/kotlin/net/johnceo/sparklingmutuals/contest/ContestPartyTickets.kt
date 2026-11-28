package net.johnceo.sparklingmutuals.contest

/** Confirmations are scoped to the local player, current roster, and contest window. */
internal class ContestPartyTickets {
    private var owner: String? = null
    private var window: Long? = null
    private val confirmed = mutableSetOf<String>()
    private var shown = false

    fun update(self: String, contest: Long, teammates: Set<String>) {
        if (self != owner || contest != window || teammates.isEmpty()) clear()
        owner = self
        window = contest
        confirmed.retainAll(teammates)
    }

    fun confirm(id: String, contest: Long, teammates: Set<String>) {
        if (contest == window && id in teammates) confirmed.add(id)
    }

    fun ready(contest: Long, teammates: Set<String>, localEarned: Boolean) =
        contest == window && localEarned && teammates.isNotEmpty() && confirmed.containsAll(teammates)

    fun shouldAnnounce() = !shown

    fun markShown() { shown = true }

    fun clear() {
        owner = null
        window = null
        confirmed.clear()
        shown = false
    }

    companion object {
        private val ticketReport = Regex(
            "(?:(?:basic |first[- ]class )?ticket earned|ticket|(?:i )?got (?:my |a |the )?(?:basic |first[- ]class )?ticket)[!.]*",
            RegexOption.IGNORE_CASE
        )

        fun reportsTicket(body: String) = ticketReport.matches(body.trim())
    }
}
