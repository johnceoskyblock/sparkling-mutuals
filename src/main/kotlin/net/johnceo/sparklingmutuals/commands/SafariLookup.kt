package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.config.ConfigManager

object SafariLookup {
    private data class Cached(val discoveries: Set<String>, val timestamp: Long)
    private val cache = mutableMapOf<String, Cached>()
    private val timesaves = linkedMapOf("ROCKMITE" to "Rockmite", "SNOOZLE" to "Snoozle",
        "GEMZIE" to "Gemzie", "HONEYBUG" to "Honeybug", "GAZER" to "Gazer",
        "GIMMIEGOLD" to "Gimmiegold", "DOOMSPIRAL" to "Doomspiral", "WUMPA" to "Wumpa")
    private val birds = setOf("BLUEBIRD", "PARAKEET", "MACAW")

    fun clearCache() = cache.clear()

    fun run(command: PartyCommand, members: List<String>, localName: String = "You"): String = when (command.kind) {
        PartyCommandKind.PB_DOOM -> ConfigManager.personalBests.response(localName, "Doomspiral")
        PartyCommandKind.PB_WUMPA -> ConfigManager.personalBests.response(localName, "Wumpa")
        PartyCommandKind.HELP -> CommandHelp.partyReply()
        PartyCommandKind.MUTUALS -> {
            val discoveries = members.map { uuid ->
                val cached = cache[uuid]
                if (cached != null && System.currentTimeMillis() - cached.timestamp < 60_000) cached.discoveries
                else HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid)).also {
                    cache[uuid] = Cached(it, System.currentTimeMillis())
                }
            }.reduceOrNull(Set<String>::intersect).orEmpty()
            "Mutual Timesave Sparkling Critters: ${formatTimesaves(discoveries).ifEmpty { listOf("None") }.joinToString(", ")}"
        }
        PartyCommandKind.MISSING -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val discovered = HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            val missing = timesaves.filterKeys { it !in discovered }.values.toMutableList()
            if (!discovered.containsAll(birds)) missing.add("All Birds")
            "Missing Timesave Sparklings for ${command.ign}: ${missing.ifEmpty { listOf("None") }.joinToString(", ")}"
        }
        PartyCommandKind.TICKETS -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val tickets = HypixelApi.getSafariTickets(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            "${command.ign}: Basic (${tickets["basic"]}), Economy (${tickets["economy"]}), " +
                "Premium (${tickets["premium"]}), First-Class (${tickets["first_class"]})"
        }
    }

    private fun formatTimesaves(discoveries: Set<String>): List<String> =
        timesaves.filterKeys { it in discoveries }.values.toMutableList().also {
            if (discoveries.containsAll(birds)) it.add("All Birds")
        }
}
