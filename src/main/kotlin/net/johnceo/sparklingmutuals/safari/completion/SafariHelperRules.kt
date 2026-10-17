package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRun

object SafariHelperRules {
    fun showNestCounter(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        run?.nestCounterNeeded(mode, party) ?: (mode != SafariMode.SPARKLING || party.needs("Honeybug"))
    fun showNests(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        showNestCounter(mode, run, party)
    val species = setOf("Honeybug", "Rockmite", "Snoozle")
    fun needed(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) = when (mode) {
        SafariMode.FULL_CLEAR -> true
        SafariMode.UNIQUE -> if (species == "Honeybug") showNestCounter(mode, run, party) else
            run?.uniqueComplete(species, party) != true && ((run?.count(species) ?: 0) == 0 || party.needs(species))
        SafariMode.SPARKLING -> party.needs(species) && run?.sparklingComplete(species, party) != true
    }
}
