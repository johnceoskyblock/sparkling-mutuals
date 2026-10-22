package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiFailureKind
import net.johnceo.sparklingmutuals.api.ApiProvider
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariRoster

object SafariLookup {
    private val cache = net.johnceo.sparklingmutuals.api.PartyDiscoveryCache()
    private var account: String? = null
    @Synchronized fun useAccount(identity: String): Boolean {
        if (account == identity) return false
        cache.clear(); account = identity
        return true
    }
    fun selectParty(members: Set<String>, provider: ApiProvider = HypixelApi.provider) = cache.select(members, provider)
    fun known(uuid: String, provider: ApiProvider = HypixelApi.provider) = cache.known(uuid, provider)
    fun caught(members: Set<String>, species: String, participants: Set<String> = members) =
        cache.caught(members, species, HypixelApi.provider, participants)
    private val timesaves = linkedMapOf("ROCKMITE" to "Rockmite", "SNOOZLE" to "Snoozle",
        "GEMZIE" to "Gemzie", "HONEYBUG" to "Honeybug", "GAZER" to "Gazer",
        "GIMMIEGOLD" to "Gimmiegold", "DOOMSPIRAL" to "Doomspiral", "WUMPA" to "Wumpa")
    private val birds = linkedMapOf("BLUEBIRD" to "Bluebird", "PARAKEET" to "Parakeet", "MACAW" to "Macaw")

    fun clearCache() = cache.clear()
    /** No profile/Safari history means no discoveries for party requirements, not an unfinished lookup. */
    internal fun discoveriesOrEmpty(lookup: () -> Set<String>): Set<String> = try {
        lookup()
    } catch (failure: ApiFailure) {
        if (failure.kind == ApiFailureKind.DATA_UNAVAILABLE) emptySet() else throw failure
    }
    /** Retained for this party membership; only missing/new members need HTTP. */
    fun discoveries(uuid: String, key: String = ConfigManager.apiKey): Set<String> =
        discoveries(uuid, ApiProvider.capture(key))
    fun discoveries(uuid: String, provider: ApiProvider): Set<String> {
        check(provider == HypixelApi.provider) { "API provider changed during lookup" }
        if (!provider.enabled) throw ApiFailure(ApiFailureKind.MISSING_KEY, "No Hypixel API key. Use /apikey or settings.")
        return cache.load(uuid, provider) {
            val read = discoveriesOrEmpty {
                HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            }
            check(provider == HypixelApi.provider) { "API provider changed during lookup" }
            read
        }
    }

    fun run(command: PartyCommand, members: List<String>, localName: String = "You"): String = when (command.kind) {
        PartyCommandKind.PB_DOOM -> if (command.trackedPlayer) ConfigManager.personalBests.trackedResponse(command.ign, "Doomspiral") else ConfigManager.personalBests.response(localName, "Doomspiral", command.duo)
        PartyCommandKind.PB_WUMPA -> if (command.trackedPlayer) ConfigManager.personalBests.trackedResponse(command.ign, "Wumpa") else ConfigManager.personalBests.response(localName, "Wumpa", command.duo)
        PartyCommandKind.PB_FOREST -> ConfigManager.personalBests.response(localName, "Forest")
        PartyCommandKind.PB_HAUNTED -> ConfigManager.personalBests.response(localName, "Haunted")
        PartyCommandKind.PB_ICY -> ConfigManager.personalBests.response(localName, "Icy")
        PartyCommandKind.PB_CAVERN -> ConfigManager.personalBests.response(localName, "Cavern")
        PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE -> SafariSinceReply.current(command)
        PartyCommandKind.HELP -> CommandHelp.partyReply()
        PartyCommandKind.MISSING_RUN -> SafariMissingReply.current(command.biome)
        PartyCommandKind.MUTUALS -> {
            val discoveries = members.map { discoveries(it) }.reduceOrNull(Set<String>::intersect).orEmpty()
            "Mutual ${if (ConfigManager.timesaveOnly) "Timesave " else ""}Shiny Critters: ${mutualResult(discoveries, ConfigManager.timesaveOnly)}"
        }
        PartyCommandKind.MISSING -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val discovered = HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid))
                        "Missing ${if (ConfigManager.timesaveOnly) "Timesave " else ""}Shinies for ${command.ign}: ${missingResult(discovered, ConfigManager.timesaveOnly)}"
        }
        PartyCommandKind.TICKETS -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val tickets = HypixelApi.getSafariTickets(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            "${command.ign}: Basic (${tickets["basic"]}), Economy (${tickets["economy"]}), " +
                "Premium (${tickets["premium"]}), First-Class (${tickets["first_class"]}), Total (${tickets.values.sum()})"
        }
    }

    fun mutualResult(discoveries: Set<String>, timesaveOnly: Boolean) =
        formatDiscoveries(discoveries, timesaveOnly).takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None. :skull:"
    fun missingResult(discoveries: Set<String>, timesaveOnly: Boolean): String {
        val missing = formatMissing(discoveries, timesaveOnly)
        return when {
            missing.isEmpty() -> "None"
            missing == formatMissing(emptySet(), timesaveOnly) -> "All. :skull:"
            else -> missing.joinToString(", ")
        }
    }
    fun isManualCheck(name: String) = discoveryKey(name) in timesaves || discoveryKey(name) in birds
    fun isBird(name: String) = discoveryKey(name) in birds
    private fun discoveryKey(name: String) = name.uppercase(java.util.Locale.ROOT).replace(' ', '_')
    fun formatDiscoveries(discoveries: Set<String>, timesaveOnly: Boolean): List<String> =
        if (timesaveOnly) formatTimesaves(discoveries) else SafariRoster.all.filter { discoveryKey(it.name) in discoveries }.map { it.name }
    fun formatMissing(discoveries: Set<String>, timesaveOnly: Boolean): List<String> =
        if (!timesaveOnly) SafariRoster.all.filter { discoveryKey(it.name) !in discoveries }.map { it.name }
        else timesaves.filterKeys { it !in discoveries }.values.toMutableList().also {
            it.addAll(birds.filterKeys { it !in discoveries }.values)
        }

    private fun formatTimesaves(discoveries: Set<String>): List<String> =
        timesaves.filterKeys { it in discoveries }.values.toMutableList().also {
            it.addAll(birds.filterKeys { it in discoveries }.values)
        }
}
