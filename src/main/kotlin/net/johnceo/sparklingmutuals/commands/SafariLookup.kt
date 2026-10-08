package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.HypixelApi
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariRoster

object SafariLookup {
    private data class Cached(val discoveries: Set<String>, val timestamp: Long)
    private data class CacheKey(val key: String, val uuid: String)
    private val cache = mutableMapOf<CacheKey, Cached>()
    private val timesaves = linkedMapOf("ROCKMITE" to "Rockmite", "SNOOZLE" to "Snoozle",
        "GEMZIE" to "Gemzie", "HONEYBUG" to "Honeybug", "GAZER" to "Gazer",
        "GIMMIEGOLD" to "Gimmiegold", "DOOMSPIRAL" to "Doomspiral", "WUMPA" to "Wumpa")
    private val birds = setOf("BLUEBIRD", "PARAKEET", "MACAW")

    @Synchronized fun clearCache() = cache.clear()
    /** Both party commands and mode refreshes share a key-scoped, serialized 60-second cache. */
    @Synchronized fun discoveries(uuid: String, key: String = ConfigManager.apiKey): Set<String> {
        check(key == ConfigManager.apiKey) { "API key changed during lookup" }
        val id = CacheKey(key, uuid)
        val now = System.currentTimeMillis()
        cache[id]?.takeIf { now - it.timestamp < 60_000 }?.let { return it.discoveries }
        val result = HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid))
        check(key == ConfigManager.apiKey) { "API key changed during lookup" }
        cache[id] = Cached(result, System.currentTimeMillis())
        cache.entries.removeIf { System.currentTimeMillis() - it.value.timestamp > 60_000 }
        return result
    }

    fun run(command: PartyCommand, members: List<String>, localName: String = "You"): String = when (command.kind) {
        PartyCommandKind.PB_DOOM -> ConfigManager.personalBests.response(localName, "Doomspiral")
        PartyCommandKind.PB_WUMPA -> ConfigManager.personalBests.response(localName, "Wumpa")
        PartyCommandKind.PB_FOREST -> ConfigManager.personalBests.response(localName, "Forest")
        PartyCommandKind.PB_HAUNTED -> ConfigManager.personalBests.response(localName, "Haunted")
        PartyCommandKind.PB_ICY -> ConfigManager.personalBests.response(localName, "Icy")
        PartyCommandKind.PB_CAVERN -> ConfigManager.personalBests.response(localName, "Cavern")
        PartyCommandKind.HELP -> CommandHelp.partyReply()
        PartyCommandKind.MUTUALS -> {
            val discoveries = members.map { discoveries(it) }.reduceOrNull(Set<String>::intersect).orEmpty()
            "Mutual ${if (ConfigManager.timesaveOnly) "Timesave " else ""}Sparkling Critters: ${formatDiscoveries(discoveries, ConfigManager.timesaveOnly).ifEmpty { listOf("None") }.joinToString(", ")}"
        }
        PartyCommandKind.MISSING -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val discovered = HypixelApi.getSparklingCritters(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            val missing = formatMissing(discovered, ConfigManager.timesaveOnly)
            "Missing ${if (ConfigManager.timesaveOnly) "Timesave " else ""}Sparklings for ${command.ign}: ${missing.ifEmpty { listOf("None") }.joinToString(", ")}"
        }
        PartyCommandKind.TICKETS -> {
            val uuid = HypixelApi.getPlayerUuid(command.ign)
            val tickets = HypixelApi.getSafariTickets(uuid, HypixelApi.getCurrentProfileUuid(uuid))
            "${command.ign}: Basic (${tickets["basic"]}), Economy (${tickets["economy"]}), " +
                "Premium (${tickets["premium"]}), First-Class (${tickets["first_class"]})"
        }
    }

    private fun discoveryKey(name: String) = name.uppercase(java.util.Locale.ROOT).replace(' ', '_')
    fun formatDiscoveries(discoveries: Set<String>, timesaveOnly: Boolean): List<String> =
        if (timesaveOnly) formatTimesaves(discoveries) else SafariRoster.all.filter { discoveryKey(it.name) in discoveries }.map { it.name }
    fun formatMissing(discoveries: Set<String>, timesaveOnly: Boolean): List<String> =
        if (!timesaveOnly) SafariRoster.all.filter { discoveryKey(it.name) !in discoveries }.map { it.name }
        else timesaves.filterKeys { it !in discoveries }.values.toMutableList().also { if (!discoveries.containsAll(birds)) it.add("All Birds") }

    private fun formatTimesaves(discoveries: Set<String>): List<String> =
        timesaves.filterKeys { it in discoveries }.values.toMutableList().also {
            if (discoveries.containsAll(birds)) it.add("All Birds")
        }
}
