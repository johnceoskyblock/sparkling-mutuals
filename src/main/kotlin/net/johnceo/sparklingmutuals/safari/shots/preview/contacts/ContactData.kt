package net.johnceo.sparklingmutuals.safari.shots.preview.contacts

import net.johnceo.sparklingmutuals.safari.shots.*

data class ContactNode(val parameter: Double, val position: ShotVector, val observedRegion: Boolean)
enum class ContactStop { CONTACT, RANGE_LIMIT, ORDINAL_LIMIT, WORK_LIMIT, INVALID, UNLOADED, FLUID, TARGET_COVERAGE }
data class ContactCurve(val nodes: List<ContactNode>, val stop: ContactStop, val hypothesis: String)
data class ContactTarget(val key: String, val generation: Int, val species: String, val bounds: ShotBounds, val proxy: String)
data class ContactCandidate(val kind: ContactKind, val parameter: Double, val position: ShotVector,
    val normal: ShotVector?, val targetKey: String?, val proxy: String?)
enum class ContactKind { TERRAIN, CRITTER }
data class ContactBoundary(val fraction: Double, val reason: ContactStop)
data class ContactSegmentScene(val terrain: ContactCandidate?, val boundary: ContactBoundary?)
data class ContactCoverage(val complete: Boolean, val reasons: Set<String>)
data class ContactTrace(val nodes: List<ContactNode>, val primary: ContactCandidate?, val stop: ContactStop, val warnings: Set<String>)

internal fun ShotVector.finite() = x.isFinite() && y.isFinite() && z.isFinite()
internal fun ShotVector.lerp(to: ShotVector, f: Double) = ShotVector(x+(to.x-x)*f,y+(to.y-y)*f,z+(to.z-z)*f)
internal fun ShotBounds.valid() = min.finite() && max.finite() && min.x<max.x && min.y<max.y && min.z<max.z

/** Exportable reason codes only; never insert entity names or exception messages. */
object ContactWarnings {
    val allowed = setOf("contact_tie", "scan_pending", "target_limit", "unsupported_proxy", "stale_target", "identity_budget", "shape_limit")
}
