package net.johnceo.sparklingmutuals.history.achievements

import java.util.Locale

data class AchievementScope(val playerUuid: String, val profileId: String) {
    val key: String get() = normalizePlayer(playerUuid) + ":" + profileId
}
internal fun normalizePlayer(value: String) = value.trim().replace("-", "").lowercase(Locale.ROOT)
data class DexSnapshot(val scope: AchievementScope, val discoveries: Set<String>, val observedAt: Long)
data class AchievementRunEvidence(val id: String, val scope: AchievementScope, val missingBefore: Set<String>?,
    val personalCounts: Map<String, Int>, val ownSparklingSpecies: Set<String>, val provenanceComplete: Boolean)

class DroughtEvidence(val scope: AchievementScope) {
    var currentOrdinaryHazard = 0.0
        private set
    var bestOrdinaryHazard = 0.0
        private set
    var currentUniqueHazard = 0.0
        private set
    var bestUniqueHazard = 0.0
        private set
    var coverageStart: Long? = null
        private set
    private val processed = mutableSetOf<String>()
    val processedRunIds: Set<String> get() = processed.toSet()
    private var active = true
    private var dex: DexSnapshot? = null
    /** Confirmed personal discoveries survive API cache lag and scope pauses. */
    private val confirmedOwnDiscoveries = mutableSetOf<String>()

    /** Only complete contemporaneous personal observations support a dry interval. */
    fun recordRun(event: AchievementRunEvidence): Boolean {
        if (!active || event.scope.key != scope.key || event.id.isBlank() || !processed.add(event.id)) return false
        if (!event.provenanceComplete) {
            currentOrdinaryHazard = 0.0
            currentUniqueHazard = 0.0
            return true
        }
        val exposures = event.personalCounts.mapNotNull { (species, count) ->
            AchievementCalibration.rates[species]?.let { species to minOf(count.coerceAtLeast(0).toDouble(), it) }
        }.toMap()
        currentOrdinaryHazard = if (event.ownSparklingSpecies.isNotEmpty()) 0.0
            else currentOrdinaryHazard + AchievementCalibration.hazard(exposures.values.sum())
        val missing = event.missingBefore?.minus(confirmedOwnDiscoveries)
        currentUniqueHazard = if (missing == null || missing.isEmpty() || event.ownSparklingSpecies.any { it in missing }) 0.0
            else currentUniqueHazard + AchievementCalibration.hazard(exposures.filterKeys { it in missing }.values.sum())
        confirmedOwnDiscoveries.addAll(event.ownSparklingSpecies.filter { it in AchievementCalibration.rates })
        bestOrdinaryHazard = maxOf(bestOrdinaryHazard, currentOrdinaryHazard)
        bestUniqueHazard = maxOf(bestUniqueHazard, currentUniqueHazard)
        return true
    }

    /** Changes establish a prospective baseline, never reconstruct old missing identities. */
    fun refreshDex(snapshot: DexSnapshot) {
        if (snapshot.scope.key != scope.key || snapshot.observedAt < (dex?.observedAt ?: Long.MIN_VALUE)) return
        if (dex == null || dex!!.discoveries + confirmedOwnDiscoveries != snapshot.discoveries + confirmedOwnDiscoveries)
            currentUniqueHazard = 0.0
        if (coverageStart == null) coverageStart = snapshot.observedAt
        dex = snapshot.copy(discoveries = snapshot.discoveries.toSet())
        active = true
    }
    fun invalidateScope() {
        active = false
        dex = null
        currentOrdinaryHazard = 0.0
        currentUniqueHazard = 0.0
    }
}

/** Main-thread response generation, owner and provider fence; provider credentials are never serialized. */
class ProfileResponseGuard {
    private var generation = 0L
    private var owner: String? = null
    private var provider: Any? = null
    fun begin(owner: String, provider: Any): Long {
        this.owner = normalizePlayer(owner)
        this.provider = provider
        return ++generation
    }
    fun accepts(token: Long, owner: String?, provider: Any): Boolean =
        token == generation && owner != null && normalizePlayer(owner) == this.owner && provider == this.provider
    fun invalidate() { generation++; owner = null; provider = null }
}
