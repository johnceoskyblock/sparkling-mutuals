package net.johnceo.sparklingmutuals.contest

/** One local announcement per player and absolute contest window, independent of profile/HUD refreshes. */
class ContestTicketState {
    private val announced = mutableSetOf<Pair<String, Long>>()
    fun claim(player: String, window: Long, tier: String, secondsIntoWindow: Int): Boolean {
        if (player.isBlank() || secondsIntoWindow < 10 || tier.uppercase() !in earnedTiers) return false
        announced.removeAll { it.second < window - 2 }
        return announced.add(player.lowercase() to window)
    }
    companion object {
        val earnedTiers = setOf("UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL")
    }
}
