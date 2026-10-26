package net.johnceo.sparklingmutuals.safari.sparkling

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster

/** Detection identities and confirmed captures for presentation; sightings never gate completion. */
class SparklingCaptureState {
    private val identities = mutableMapOf<UUID, UUID>()
    fun identity(id: UUID) = identities[id] ?: id
    fun resolved(id: UUID) = identities[id]?.let { it !in pending } == true
    private val pending = linkedMapOf<UUID, String>()
    private val capturedSpecies = mutableSetOf<String>()
    fun captured(species: String) = species in capturedSpecies
    private data class Proof(val species: String, val player: String?, val broadcast: Boolean, val at: Long)
    private val proofs = mutableListOf<Proof>()
    fun observe(id: UUID, species: String, aliases: Set<UUID> = emptySet()): Boolean {
        if (SafariRoster.named(species) == null) return false
        val all = aliases + id
        val known = all.mapNotNull(identities::get).toSet()
        val canonical = known.firstOrNull { it !in pending } ?: known.firstOrNull() ?: id
        if (known.isNotEmpty()) {
            known.filter { it != canonical }.forEach(pending::remove)
            identities.entries.filter { it.value in known }.forEach { it.setValue(canonical) }
        }
        all.forEach { identities[it] = canonical }
        if (known.isNotEmpty()) return false
        pending[canonical] = species
        return true
    }
    fun pending(species: String) = species in pending.values
    fun pending(biome: SafariBiome) = pending.values.any { SafariRoster.named(it)?.biome == biome }
    fun anyPending() = pending.isNotEmpty()
    /** The global announcement and own/loot-share reward describe the same catch, in either order. */
    fun capture(species: String, player: String?, broadcast: Boolean, at: Long): Boolean {
        proofs.removeAll { at - it.at > 5000 }
        val duplicate = proofs.indexOfFirst { it.species == species && it.broadcast != broadcast &&
            (it.player == null || player == null || it.player.equals(player, true)) && kotlin.math.abs(at - it.at) <= 5000 }
        if (duplicate >= 0) { proofs.removeAt(duplicate); return false }
        capturedSpecies.add(species)
        proofs += Proof(species, player, broadcast, at)
        pending.entries.firstOrNull { it.value == species }?.key?.let(pending::remove)
        return true
    }
}
