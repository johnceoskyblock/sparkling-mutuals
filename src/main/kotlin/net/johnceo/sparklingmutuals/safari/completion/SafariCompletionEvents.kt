package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun

/** Run-owned notification memory never changes capture or PB evidence. */
class SafariCompletionEvents {
    private val announced = mutableSetOf<SafariBiome>()
    private val pending = mutableSetOf<SafariBiome>()
    private var alerted = false
    fun reopen(biome: SafariBiome) { announced.remove(biome); pending.remove(biome); alerted = false }
    /** Local clearing can trigger supplies even if a teammate's report suppresses our announcement. */
    fun locallyComplete(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState): Boolean {
        if (run.lastBiome != biome || !run.hasCaptureEvidence(biome) || run.hasUnusedStartingForBiome(biome, party) ||
            mode == SafariMode.SPARKLING && !party.ready) return false
        val targets = biome.critters.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        return run.completed(mode, targets, party, localOnly = true) == targets.size
    }
    /** Completion feedback belongs to each client even when a peer sent the shared biome report first. */
    fun armSharedClearReminder(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState, now: Long) {
        if ((biome in announced || run.uniqueReportReceived(biome)) && locallyComplete(run, biome, mode, party))
            run.partyResourceReminders.afterBiomeReport(now)
    }
    fun announcement(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState): String? {
        if (!run.canReportBiome(biome, party)) { pending.remove(biome); return null }
        if (run.lastBiome != biome) { pending.remove(biome); return null }
        if ((mode == SafariMode.UNIQUE || biome == SafariBiome.FOREST || biome == SafariBiome.HAUNTED) &&
            run.uniqueReportReceived(biome)) { pending.remove(biome); return null }
        if (!locallyComplete(run, biome, mode, party)) {
            pending.remove(biome)
            return null
        }
        if (biome in announced || !pending.add(biome)) return null
        return when (biome) { SafariBiome.FOREST -> "fd"; SafariBiome.CAVERN -> "cd"; SafariBiome.ICY -> "id"; SafariBiome.HAUNTED -> "hd" }
    }
    fun sent(biome: SafariBiome) {
        if (pending.remove(biome)) announced.add(biome)
    }
    fun alert(run: SafariRun, mode: SafariMode, party: PartySparklingState, enabled: Boolean): Boolean {
        if (!enabled || alerted || mode == SafariMode.SPARKLING && !party.ready) return false
        val targets = SafariRoster.all.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        if (targets.isEmpty() || run.completed(mode, targets, party) != targets.size) return false
        alerted = true
        return true
    }
}
