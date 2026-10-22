package net.johnceo.sparklingmutuals.safari.esp

data class EspModelLabel(val id: java.util.UUID, val species: String, val distanceSquared: Double, val visible: Boolean = true)
class EspModelLabels {
    private data class Binding(val species: String, val label: java.util.UUID, var visible: Boolean, var seenAt: Long)
    private val bindings = mutableMapOf<java.util.UUID, Binding>()
    /** Once paired, retain label identity instead of adopting another nearby critter's name tag. */
    fun observe(id: java.util.UUID, species: String, labels: List<EspModelLabel>, now: Long) {
        val nearby = labels.filter { it.species == species && it.distanceSquared in 0.0..9.0 }
        if (bindings[id]?.species != species) bindings.remove(id)
        val binding = bindings[id] ?: nearby.minByOrNull { it.distanceSquared }?.let {
            Binding(species, it.id, it.visible, now).also { bound -> bindings[id] = bound }
        } ?: return
        nearby.firstOrNull { it.id == binding.label && (!binding.visible || it.visible) }?.let {
            binding.visible = binding.visible || it.visible
            binding.seenAt = now
        }
    }
    // Unknown models remain conservative; a known label gets four scan intervals of packet grace.
    fun current(id: java.util.UUID, now: Long) = bindings[id]?.let { now - it.seenAt in 0..1000 } ?: true
    fun visible(id: java.util.UUID) = bindings[id]?.visible == true
    fun label(id: java.util.UUID) = bindings[id]?.label
    fun release(id: java.util.UUID) { bindings.remove(id) }
    fun reset() { bindings.clear() }
}
