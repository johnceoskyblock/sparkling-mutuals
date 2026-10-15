package net.johnceo.sparklingmutuals.safari

import java.util.UUID

/** A seen Sparkling remains a capture obligation even after unloading or owning its discovery. */
class SparklingCaptureState {
    private val seen = mutableSetOf<UUID>()
    private val pending = linkedMapOf<UUID, String>()
    private data class Proof(val species: String, val player: String?, val broadcast: Boolean, val at: Long)
    private val proofs = mutableListOf<Proof>()
    fun observe(id: UUID, species: String): Boolean {
        if (SafariRoster.named(species) == null || !seen.add(id)) return false
        pending[id] = species
        return true
    }
    fun pending(species: String) = species in pending.values
    fun pending(biome: SafariBiome) = pending.values.any { SafariRoster.named(it)?.biome == biome }
    fun anyPending() = pending.isNotEmpty()
    /** The global announcement and own/loot-share reward describe the same catch, in either order. */
    fun capture(species: String, player: String?, broadcast: Boolean, at: Long) {
        proofs.removeAll { at - it.at > 5000 }
        val duplicate = proofs.indexOfFirst { it.species == species && it.broadcast != broadcast &&
            (it.player == null || player == null || it.player.equals(player, true)) && kotlin.math.abs(at - it.at) <= 5000 }
        if (duplicate >= 0) { proofs.removeAt(duplicate); return }
        proofs += Proof(species, player, broadcast, at)
        pending.entries.firstOrNull { it.value == species }?.key?.let(pending::remove)
    }
}
