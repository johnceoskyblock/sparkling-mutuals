package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun

/** Run-owned notification memory never changes capture or PB evidence. */
class SafariCompletionEvents(private val clock: () -> Long = System::currentTimeMillis) {
    private data class Departure(val mode: SafariMode,val needed: Set<String>,val until: Long)
    private val departures=mutableMapOf<SafariBiome,Departure>()
    private var hideyhoDialogueAt: Long?=null
    fun hideyhoDialogue(now: Long) { hideyhoDialogueAt=now }
    fun pendingDeparture(biome: SafariBiome) = departures[biome]?.let { clock()<=it.until } == true
    private fun rememberDeparture(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState) {
        if(run.lastBiome!=biome) return
        departures.remove(biome)
        val special=when(biome) {
            SafariBiome.FOREST -> setOf("Bluebird","Parakeet","Macaw")
            SafariBiome.HAUNTED -> setOf("Hideyho")
            else -> return
        }
        val pending=if(biome==SafariBiome.FOREST)
            run.birdFoodCheck.floorDropsRemaining==0 && run.birdFoodCheck.leftToUse==0 &&
                (run.birdFoodCheck.pendingSpawns>0 || run.birds.feederCount>0 || run.birdSpawnsComplete(localOnly=true))
        else run.count("Hideyho")>0 || hideyhoDialogueAt?.let { clock()-it in 0..30_000 }==true
        if(!pending || !run.hasCaptureEvidence(biome) || mode==SafariMode.SPARKLING && !party.ready) return
        val others=biome.critters.filter { it.name !in special && (mode!=SafariMode.SPARKLING || party.needs(it.name)) }
        if(run.completed(mode,others,party,localOnly=true)!=others.size ||
            mode==SafariMode.UNIQUE && others.any { party.needs(it.name) && !run.sparklingComplete(it.name,party,true) }) return
        // A full feeder batch can take longer than a short travel grace; retain its actual source proof.
        departures[biome]=Departure(mode,biome.critters.filter { party.needs(it.name) }.map { it.name }.toSet(),
            if(biome==SafariBiome.FOREST) Long.MAX_VALUE else clock()+30_000)
    }
    private fun departureComplete(run: SafariRun,biome: SafariBiome,mode: SafariMode,party: PartySparklingState): Boolean {
        val saved=departures[biome] ?: return false
        if(!pendingDeparture(biome) || saved.mode!=mode ||
            saved.needed!=biome.critters.filter { party.needs(it.name) }.map { it.name }.toSet() ||
            biome.critters.any { run.sparklePending(it.name) }) return false
        if(biome==SafariBiome.HAUNTED) return run.count("Hideyho")>0
        val birds=biome.critters.filter { it.name in setOf("Bluebird","Parakeet","Macaw") &&
            (mode!=SafariMode.SPARKLING || party.needs(it.name)) }
        return run.birdSpawnsComplete(localOnly=true) && run.completed(mode,birds,party,localOnly=true)==birds.size
    }
    private val announced = mutableMapOf<SafariBiome, SafariMode?>()
    private val pending = mutableSetOf<SafariBiome>()
    private var alerted = false
    fun reopen(biome: SafariBiome) { announced.remove(biome); pending.remove(biome); alerted = false }
    /** Local clearing can trigger supplies even if a teammate's report suppresses our announcement. */
    fun locallyComplete(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState): Boolean {
        if (run.endedAt!=null || !run.hasCaptureEvidence(biome) || run.hasUnusedStartingForBiome(biome) ||
            mode == SafariMode.SPARKLING && !party.ready) return false
        if(run.lastBiome!=biome) return departureComplete(run,biome,mode,party)
        val targets = biome.critters.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        return run.completed(mode, targets, party, localOnly = true) == targets.size &&
            (mode != SafariMode.UNIQUE || allNeededChecked(run, biome, party, localOnly = true))
    }
    fun allNeededChecked(run: SafariRun, biome: SafariBiome, party: PartySparklingState, localOnly: Boolean = false) =
        party.ready && biome.critters.none { party.needs(it.name) && !run.sparklingComplete(it.name, party, localOnly) }

    /** Completion feedback belongs to each client even when a peer sent the shared biome report first. */
    fun armSharedClearReminder(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState, now: Long) {
        if ((biome in announced || run.uniqueReportReceived(biome) || biome == SafariBiome.ICY && run.count("Wumpa") > 0) && locallyComplete(run, biome, mode, party))
            run.partyResourceReminders.afterBiomeReport(now)
    }
    fun chatReportAllowed(biome: SafariBiome, mode: SafariMode) =
        biome != SafariBiome.ICY || mode != SafariMode.UNIQUE
    fun announcement(run: SafariRun, biome: SafariBiome, mode: SafariMode, party: PartySparklingState, enabled: Boolean = true): String? {
        rememberDeparture(run,biome,mode,party)
        if (!enabled || mode == SafariMode.SPARKLING && biome.critters.none { party.needs(it.name) }) {
            pending.remove(biome)
            return null
        }
        if (!run.canReportBiome(biome, party, mode)) { pending.remove(biome); return null }
        if (!locallyComplete(run, biome, mode, party)) {
            pending.remove(biome)
            return null
        }
        if (biome in announced || !pending.add(biome)) return null
        return when (biome) { SafariBiome.FOREST -> "fd"; SafariBiome.CAVERN -> "cd"; SafariBiome.ICY -> "id"; SafariBiome.HAUNTED -> "hd" }
    }
    fun sent(biome: SafariBiome, mode: SafariMode? = null) {
        if (pending.remove(biome)) announced[biome] = mode
    }
    fun reported(biome: SafariBiome)=biome in announced
    fun locallyReported(biome: SafariBiome, mode: SafariMode) = announced[biome] == mode
    fun alert(run: SafariRun, mode: SafariMode, party: PartySparklingState, enabled: Boolean): Boolean {
        if (!enabled || alerted || mode == SafariMode.SPARKLING && !party.ready) return false
        val targets = SafariRoster.all.filter { mode != SafariMode.SPARKLING || party.needs(it.name) }
        if (targets.isEmpty() || SafariBiome.entries.any { !run.biomeComplete(mode,it,party) }) return false
        alerted = true
        return true
    }
}
