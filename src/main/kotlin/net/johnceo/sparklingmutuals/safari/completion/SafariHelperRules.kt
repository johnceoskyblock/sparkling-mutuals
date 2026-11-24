package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRun

object SafariHelperRules {
    /** Source highlights follow profitable ESP without changing objective or completion requirements. */
    fun highlightSource(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState): Boolean {
        if (!BiomeObjectiveConfig.allowsEsp(species, mode, run)) return false
        val profitable=when(mode) {
            SafariMode.UNIQUE -> net.johnceo.sparklingmutuals.config.ConfigManager.profitableShardEsp
            SafariMode.SPARKLING -> net.johnceo.sparklingmutuals.config.ConfigManager.sparklingProfitableShardEsp
            SafariMode.FULL_CLEAR -> false
        }
        return needed(species,mode,run,party) || species in setOf("Rockmite","Snoozle") &&
            net.johnceo.sparklingmutuals.safari.esp.SafariEspRules.profitableEsp(species,profitable)
    }
    fun showNestCounter(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        if (!BiomeObjectiveConfig.allowsEsp("Honeybug", mode, run) ||
            run?.sharedCritterComplete("Honeybug", if (mode == SafariMode.UNIQUE && BiomeObjectiveConfig.needs("Honeybug", party, mode)) SafariMode.SPARKLING else mode) == true)
            false else run?.nestCounterNeeded(mode, party) ?: (mode != SafariMode.SPARKLING || party.needs("Honeybug"))
    fun showNests(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        showNestCounter(mode, run, party)
    val species = setOf("Honeybug", "Rockmite", "Snoozle")
    fun needed(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) = when (mode) {
        SafariMode.FULL_CLEAR -> run?.sharedCritterComplete(species, mode) != true
        SafariMode.UNIQUE -> if (species == "Honeybug") showNestCounter(mode, run, party) else
            run?.uniqueComplete(species, party) != true && ((run?.count(species) ?: 0) == 0 || BiomeObjectiveConfig.needs(species, party, mode))
        SafariMode.SPARKLING -> party.needs(species) && run?.sparklingComplete(species, party) != true
    }
}
