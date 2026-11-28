package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules

object SafariHelperRules {
    /** Source highlights follow profitable ESP without changing objective or completion requirements. */
    fun highlightSource(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState): Boolean {
        val afterClear = sourceAfterClear(species, mode, run, party)
        if (!afterClear && !BiomeObjectiveConfig.allowsEsp(species, mode, run)) return false
        val required = if (afterClear && run != null) needsPostClearCheck(species, run, party) else needed(species, mode, run, party)
        val profitable=when(mode) {
            SafariMode.UNIQUE -> ConfigManager.profitableShardEsp
            SafariMode.SPARKLING -> ConfigManager.sparklingProfitableShardEsp
            SafariMode.FULL_CLEAR -> false
        }
        return required || species in setOf("Rockmite","Snoozle") &&
            SafariEspRules.profitableEsp(species,profitable)
    }
    fun postClearActive(mode: SafariMode, run: SafariRun?, biome: SafariBiome, party: PartySparklingState, biomeComplete: Boolean? = null) =
        mode == SafariMode.UNIQUE && SafariEspConfig.uniqueRemainingChecks &&
            run != null && run.endedAt == null && (run.completion.locallyReported(biome, mode) ||
                (biomeComplete ?: run.biomeComplete(mode, biome, party)))

    fun needsPostClearCheck(name: String, run: SafariRun, party: PartySparklingState) =
        party.ready && party.needs(name) && !run.sparklingComplete(name, party, trustBiomeReports = false)

    private fun sourceAfterClear(name: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        !SafariSettings.standardEdition && name in species && postClearActive(mode, run, SafariRoster.named(name)!!.biome, party)

    fun postClearSource(name: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        run != null && sourceAfterClear(name, mode, run, party) && needsPostClearCheck(name, run, party)
    fun showNestCounter(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        if (!BiomeObjectiveConfig.allowsEsp("Honeybug", mode, run) ||
            run?.sharedCritterComplete("Honeybug", if (mode == SafariMode.UNIQUE && BiomeObjectiveConfig.needs("Honeybug", party, mode)) SafariMode.SPARKLING else mode) == true)
            false else run?.nestCounterNeeded(mode, party) ?: (mode != SafariMode.SPARKLING || party.needs("Honeybug"))
    fun showNests(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        if (run != null && sourceAfterClear("Honeybug", mode, run, party)) needsPostClearCheck("Honeybug", run, party)
        else showNestCounter(mode, run, party)
    fun showKnownNestMarkers(mode: SafariMode, ordinarilyNeeded: Boolean, localRemaining: Boolean, standard: Boolean) =
        ordinarilyNeeded || !standard && mode == SafariMode.FULL_CLEAR && localRemaining
    val species = setOf("Honeybug", "Rockmite", "Snoozle")
    fun needed(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) = when (mode) {
        SafariMode.FULL_CLEAR -> run?.sharedCritterComplete(species, mode) != true
        SafariMode.UNIQUE -> if (species == "Honeybug") showNestCounter(mode, run, party) else
            run?.uniqueComplete(species, party) != true && ((run?.count(species) ?: 0) == 0 || BiomeObjectiveConfig.needs(species, party, mode))
        SafariMode.SPARKLING -> party.needs(species) && run?.sparklingComplete(species, party) != true
    }
}
