package net.johnceo.sparklingmutuals.safari

import java.util.Locale
import net.johnceo.sparklingmutuals.api.ApiProvider
import net.johnceo.sparklingmutuals.api.HypixelApi

/** A partial or different-party lookup must never claim a discovery is shared by everyone. */
/** The party discovery intersection, invalidated when membership or API provider changes. */
class PartySparklingState {
    private var members = emptySet<String>()
    private var shared: Set<String>? = null
    private var individual = emptyMap<String, Set<String>>()
    private var provider: ApiProvider? = null
    val ready get() = shared != null
    fun select(roster: Set<String>, selectedProvider: ApiProvider = HypixelApi.provider) {
        if (members != roster || provider != selectedProvider) {
            members = roster.toSet(); provider = selectedProvider; shared = null; individual = emptyMap()
        }
    }
    fun accept(roster: Set<String>, discoveries: Map<String, Set<String>>, source: ApiProvider = HypixelApi.provider): Boolean {
        if (source != provider || source != HypixelApi.provider || roster.isEmpty() || roster != members || !discoveries.keys.containsAll(roster)) return false
        individual = roster.associateWith { discoveries.getValue(it).map(::key).toSet() }
        shared = individual.values.reduce(Set<String>::intersect)
        return true
    }
    private fun key(species: String) = species.uppercase(Locale.ROOT).replace(' ', '_')
    fun everyoneHas(vararg species: String) = shared?.containsAll(species.map(::key)) == true
    fun needs(species: String) = !everyoneHas(species)
    fun missingMembers(species: String): List<String> = if (!ready) emptyList() else
        members.filter { key(species) !in individual.getValue(it) }
    fun debug(name: (String) -> String): String = "ready=$ready members=" + members.associate { id ->
        name(id) to individual[id]?.let { found -> SafariRoster.all.filter { key(it.name) !in found }.map { it.name } }
    }.toString() + " (missing discoveries per member)"
    fun reset() { members = emptySet(); shared = null; provider = null; individual = emptyMap() }
}
