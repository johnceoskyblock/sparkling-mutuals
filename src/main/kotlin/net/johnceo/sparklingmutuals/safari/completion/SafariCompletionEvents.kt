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
    fun announcement(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState): String? {
        if (run.lastBiome != biome) { pending.remove(biome); return null }
        val targets = biome.critters.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        if (run.sparklingCaptures.pending(biome) || !run.hasCaptureEvidence(biome) || mode == SafariMode.SPARKLING && !party.ready ||
            run.completed(mode, targets, party, localOnly = true) != targets.size) {
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
        if (run.sparklingCaptures.anyPending() || !enabled || alerted || mode == SafariMode.SPARKLING && !party.ready) return false
        val targets = SafariRoster.all.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        if (targets.isEmpty() || run.completed(mode, targets, party) != targets.size) return false
        alerted = true
        return true
    }
}
