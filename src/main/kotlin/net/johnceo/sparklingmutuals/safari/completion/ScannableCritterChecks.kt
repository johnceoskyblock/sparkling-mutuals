package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.commands.SafariLookup
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*

/** Optional visual checklist; party discovery data and manual source requirements stay independent. */
object ScannableCritterChecks {
    fun enabled(name: String) = ConfigManager.allScannableCritterChecks && SafariFullClear.mode==SafariMode.SPARKLING &&
        SafariRoster.named(name)!=null && !SafariLookup.isManualCheck(name)
    fun pending(name: String,run: SafariRun?,party: PartySparklingState) =
        enabled(name) && run?.sparklingComplete(name,party)!=true
    fun pending(biome: SafariBiome,run: SafariRun?,party: PartySparklingState) =
        biome.critters.any { pending(it.name,run,party) }
}
