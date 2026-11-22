package net.johnceo.sparklingmutuals.safari.visibility

/** Fail closed on stale, hidden, invalid or out-of-range observations. */
internal object LocalVisibilityPolicy {
    fun identifiable(modelVisible: Boolean, nameVisible: Boolean, headVisible: Boolean) = modelVisible || nameVisible || headVisible
    fun allows(loaded: Boolean, identifiable: Boolean, inView: Boolean, unobstructed: Boolean,
        distanceSquared: Double, maxDistanceSquared: Double): Boolean =
        loaded && identifiable && inView && unobstructed && distanceSquared.isFinite() &&
            maxDistanceSquared.isFinite() && distanceSquared >= 0 && distanceSquared <= maxDistanceSquared
}

internal class VisibleDiscoveryMemory {
    private val seen = mutableSetOf<String>()
    fun first(key: String, locallyVisible: Boolean) = locallyVisible && seen.add(key)
    fun contains(key: String) = key in seen
    fun reset() = seen.clear()
}
