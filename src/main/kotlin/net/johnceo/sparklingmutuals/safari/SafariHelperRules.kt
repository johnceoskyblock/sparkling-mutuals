package net.johnceo.sparklingmutuals.safari

object SafariHelperRules {
    fun showNestCounter(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        run?.nestCounterNeeded(mode, party) ?: (mode != SafariMode.SPARKLING || party.needs("Honeybug"))
    fun showNests(mode: SafariMode, run: SafariRun?, party: PartySparklingState) =
        (run?.nestsResolved != true) && needed("Honeybug", mode, run, party)
    val species = setOf("Honeybug", "Rockmite", "Snoozle")
    fun needed(species: String, mode: SafariMode, run: SafariRun?, party: PartySparklingState) = when (mode) {
        SafariMode.FULL_CLEAR -> true
        SafariMode.UNIQUE -> run?.uniqueComplete(species, party) != true &&
            ((run?.count(species) ?: 0) == 0 || party.needs(species))
        SafariMode.SPARKLING -> party.needs(species) && run?.sparklingComplete(species, party) != true
    }
}
