package net.johnceo.sparklingmutuals.safari.resources

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome

/** Run-owned end-of-clearing trigger; candidates stay live until actually sent. */
class StartingResourceDelivery(private val resources: SafariResources) {
    private var active = false
    private var nextAt = 0L
    fun afterBiomeClear(now: Long) {
        if (active) return
        active = true; nextAt = now + 4000
    }
    fun dispatch(now: Long, player: String, biome: SafariBiome, party: PartySparklingState,
        send: (String, StartingResourceNotice) -> Unit): Boolean {
        if (!active || now < nextAt) return false
        // Rebuild from current inventory, destination and discoveries, avoiding stale quantities.
        val items = resources.notices(player, biome, party).mapNotNull { StartingResourceReminder.parse(it, party) }
        if (items.isEmpty()) return false
        val reminder = StartingResourceNotice(items)
        send(reminder.text, reminder)
        resources.noticeSent(player, reminder.text)
        nextAt = now + 4000 // Keep a later newly eligible reminder from overwriting this title.
        return true
    }
}
