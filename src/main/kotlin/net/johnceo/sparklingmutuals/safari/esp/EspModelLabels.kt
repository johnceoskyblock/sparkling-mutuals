package net.johnceo.sparklingmutuals.safari.esp

data class EspModelLabel(val id: java.util.UUID, val species: String, val distanceSquared: Double, val visible: Boolean = true)
class EspModelLabels {
    private data class Binding(val species: String, val label: java.util.UUID, var visible: Boolean, var seenAt: Long, var capturingAt: Long? = null)
    private val bindings = mutableMapOf<java.util.UUID, Binding>()
    private val missingNearby = mutableSetOf<java.util.UUID>()
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
            binding.capturingAt = null
            missingNearby.remove(id)
        }
    }
    /** Follow the already-bound label into its capsule state; do not guess by nearest model. */
    fun captureState(id: java.util.UUID, text: String?, now: Long) {
        val binding=bindings[id] ?: return
        if (text?.let { net.johnceo.sparklingmutuals.safari.SafariRules.strip(it).startsWith("CAPTURING",true) } == true)
            binding.capturingAt=now
    }
    fun captureCandidate(species: String, now: Long) = bindings.entries.filter {
        it.value.species == species && it.value.capturingAt?.let { at -> now-at in 0..60000 } == true
    }.singleOrNull()?.key
    /** Nearby loss is evidence against a leftover model; distant tag unloading is not. */
    fun observeRange(id: java.util.UUID, species: String, distanceSquared: Double, now: Long) {
        if (species !in setOf("Shyworm", "Litterbug") && distanceSquared <= 32.0 * 32 &&
            label(id) != null && !current(id, now)) missingNearby.add(id)
    }
    /** Visual fallback only: never supplies capture, contact or completion evidence. */
    fun visualCurrent(id: java.util.UUID, species: String, distanceSquared: Double, now: Long,
        hiddenHeight: Double? = null): Boolean = when (species) {
        "Chuckwalla" -> SafariEspRules.chuckwallaCurrent(label(id) != null, current(id, now), visible(id)) ||
            distanceSquared > 32.0 * 32 && id !in missingNearby
        "Shyworm", "Litterbug" -> hiddenHeight != null || current(id, now) ||
            label(id) != null && distanceSquared > 32.0 * 32
        else -> current(id, now) || label(id) != null && distanceSquared > 32.0 * 32 && id !in missingNearby
    }
    // Unknown models remain conservative; a known label gets four scan intervals of packet grace.
    fun current(id: java.util.UUID, now: Long) = bindings[id]?.let { now - it.seenAt in 0..1000 } ?: true
    /** Completion cannot treat the legacy unknown-label fallback as positive evidence. */
    fun confirmed(id: java.util.UUID, now: Long) = label(id) != null && current(id, now)
    fun visible(id: java.util.UUID) = bindings[id]?.visible == true
    fun label(id: java.util.UUID) = bindings[id]?.label
    fun release(id: java.util.UUID) { bindings.remove(id); missingNearby.remove(id) }
    fun reset() { bindings.clear(); missingNearby.clear() }
}
