package net.johnceo.sparklingmutuals.contest

/** Observe transitions continuously; enabling/joining mid-contest must not send a stale request. */
class ContestDowntimeState {
    private var previous: Long? = null
    fun poll(window: Long, secondsIntoWindow: Int, enabled: Boolean, skyblock: Boolean, inParty: Boolean, partyLeader: Boolean): Boolean {
        val before = previous
        previous = window
        return before != null && before != window && secondsIntoWindow in 0..9 && enabled && skyblock && inParty && partyLeader
    }
}
