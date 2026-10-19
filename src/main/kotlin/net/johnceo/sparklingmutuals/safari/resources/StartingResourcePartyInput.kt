package net.johnceo.sparklingmutuals.safari.resources

import java.util.concurrent.CompletableFuture
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.party.PartyRosterSignals
import net.johnceo.sparklingmutuals.safari.*
import net.minecraft.client.Minecraft

/** Resource synchronization never depends on discovery, mode, HUD, ESP or entry-automation settings. */
object StartingResourcePartyInput {
    private val inbox = StartingResourceInbox()
    private var owner: SafariRun? = null
    private var epoch = 0L
    private val names = mutableMapOf<String, String>()
    private val resolving = mutableSetOf<String>()
    private val retries = mutableMapOf<String, Long>()
    private var partyRequestedAt = 0L
    private var lastRoster: Set<String>? = null
    private fun invalidatePending() { epoch++; inbox.clear(); resolving.clear(); retries.clear() }
    private fun observe(run: SafariRun?) {
        if (owner === run) return
        owner = run; invalidatePending(); names.clear(); partyRequestedAt = 0; lastRoster = null
    }
    fun receive(client: Minecraft, run: SafariRun, text: String) {
        if (SafariTracking.ledger.current !== run || run.endedAt != null) return
        observe(run)
        if (PartyRosterSignals.changed(text)) { invalidatePending(); return }
        tick(client) // Synchronize a changed roster before binding this receipt.
        if (inbox.add(run, text, System.currentTimeMillis())) tick(client)
    }
    fun tick(client: Minecraft) {
        val run = SafariTracking.ledger.current
        observe(run)
        if (run == null || client.level == null || client.connection == null || client.player == null) return
        val self = client.player!!
        val now = System.currentTimeMillis()
        run.localPlayer(self.name.string, self.uuid.toString())
        val roster = PartyManager.withLocal(self.uuid.toString())?.mapTo(linkedSetOf(), String::lowercase)
        run.resources.contributions.roster(roster)
        // The local player can announce their own witnessed grants while party info loads.
        // Unknown recipients still cannot be credited as reported or assigned any inventory.
        val report = run.startingItems(run.floorCatalog.scope)
        if (run.startingAnnouncement.dispatch(now, SafariTracking.ledger.current === run, report) { text ->
                if (roster == null || roster.size > 1) client.connection!!.sendCommand("pc $text")
                true
            }) run.ownStartingReport()
        if (roster == null) {
            if (now - partyRequestedAt >= 5000) { partyRequestedAt = now; PartyManager.requestPartyInfo() }
            return
        }
        if (lastRoster != null && roster != lastRoster) invalidatePending()
        lastRoster = roster
        names.entries.removeIf { it.value !in roster }
        client.connection!!.onlinePlayers.forEach { info ->
            val id = info.profile.id.toString().lowercase()
            if (id in roster) names[info.profile.name.lowercase()] = id
        }
        names[self.name.string.lowercase()] = self.uuid.toString().lowercase()
        inbox.names.filter { it !in names && it !in resolving && now >= (retries[it] ?: 0) }.forEach { name ->
            resolving.add(name)
            val generation = epoch
            val connection = client.connection
            val world = client.level
            CompletableFuture.supplyAsync { HypixelApi.getPlayerUuid(name) }.whenComplete { id, error -> client.execute {
                if (generation != epoch || owner !== run || SafariTracking.ledger.current !== run ||
                    client.connection !== connection || client.level !== world) return@execute
                resolving.remove(name)
                if (error == null && id != null) {
                    val resolved = StartingResourceInbox.canonicalUuid(id)
                    if (resolved != null && resolved in PartyManager.withLocal(self.uuid.toString()).orEmpty()) names[name] = resolved
                    else retries[name] = now + 10000
                } else retries[name] = now + 10000
            } }
        }
        inbox.drain(run, names, roster, now, run.floorCatalog::acceptsScope) { id, chat ->
            if (!run.resources.reportedBy(id, chat.body)) {
                val raw = "Party > ${chat.sender}: ${chat.body}"
                run.sparklingChecks.chat(raw, manualAllowed = true)
                run.partyFullClear(raw)
            }
        }
    }
}
