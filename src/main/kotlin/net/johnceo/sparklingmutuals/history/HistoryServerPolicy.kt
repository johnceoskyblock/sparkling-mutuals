package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.history.importing.HistoryBackfill.Realm
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft

/** Keep the last explicit scoreboard address through transient empty scoreboards. */
class HistoryServerState {
    var alpha = false
        private set
    fun observe(lines: List<String>, run: SafariRun? = null): Boolean {
        val rows = lines.map { SafariRules.strip(it).trim() }
        when {
            rows.any { it.equals("alpha.hypixel.net", true) } -> alpha = true
            rows.any { it.equals("www.hypixel.net", true) } -> alpha = false
        }
        if (alpha) run?.excludeFromHistory()
        return !alpha && run?.historyExcluded != true
    }
}

object HistoryServerPolicy {
    private var state = HistoryServerState()
    private var connection: Any? = null
    fun alpha(client: Minecraft = Minecraft.getInstance()): Boolean { allows(client); return state.alpha }
    fun verifiedMain(client: Minecraft): Boolean =
        Realm.fromHost(client.currentServer?.ip) == Realm.MAIN && !alpha(client)
    fun allows(client: Minecraft = Minecraft.getInstance()): Boolean {
        if (client.connection !== connection) {
            connection = client.connection
            state = HistoryServerState()
        }
        val realm = Realm.fromHost(client.currentServer?.ip)
        val lines = if (realm == Realm.ALPHA) listOf("alpha.hypixel.net") else SkyblockSidebar.lines(client)
        return state.observe(lines, SafariTracking.ledger.current)
    }
}
