package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome

/** Run-owned end-of-clearing trigger; candidates stay live until actually sent. */
class StartingResourceDelivery(private val resources: SafariResources) {
    private var active = false
    private var nextAt = 0L
    fun afterBiomeClear(now: Long) { active = true; nextAt = now + 4000 }
    fun dispatch(now: Long, player: String, biome: SafariBiome, party: PartySparklingState,
        send: (String, StartingResourceReminder) -> Unit): Boolean {
        if (!active || now < nextAt) return false
        // Rebuild from current inventory, destination and discoveries, avoiding stale quantities.
        val notice = resources.notices(player, biome, party).firstOrNull() ?: return false
        val reminder = StartingResourceReminder.parse(notice, party) ?: return false
        send(notice, reminder)
        resources.noticeSent(player, notice)
        nextAt = now + 4000 // Each title lasts 3.5 seconds; never overwrite it with the next.
        return true
    }
}
