package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.johnceo.sparklingmutuals.safari.shots.ShotVariant
import kotlin.math.abs
import kotlin.math.hypot

data class CapsulePreviewState(
    val enabled: Boolean, val inSafari: Boolean, val capsule: ShotVariant?,
    val firstPerson: Boolean, val onGround: Boolean, val sneaking: Boolean,
    val sprinting: Boolean, val swimming: Boolean, val flying: Boolean, val riding: Boolean,
    val hideFlying: Boolean, val hideGround: Boolean, val screenOpen: Boolean, val hudHidden: Boolean,
    val pitch: Double, val position: ShotVector, val velocity: ShotVector,
    val allowUncalibratedEstimates: Boolean = false,
    val inWater:Boolean = false, val inLava:Boolean = false,
) {
    val stationaryForCalibration: Boolean get() = onGround && !sneaking && !sprinting &&
        !swimming && !inWater && !inLava && !flying && !riding && position.finite() && velocity.finite() &&
        hypot(velocity.x,velocity.z) <= .001
    val estimateLabel: String? get() = when {
        inWater || swimming -> if(capsule==ShotVariant.MASTERFUL) "Masterful / Water" else "Water"
        capsule==ShotVariant.MASTERFUL && !stationaryForCalibration -> "Masterful / Moving"
        capsule==ShotVariant.MASTERFUL -> "Masterful"
        !stationaryForCalibration -> "Moving"
        else -> null
    }
}

private fun ShotVector.finite() = x.isFinite() && y.isFinite() && z.isFinite()

enum class CapsulePreviewAvailability { HIDDEN, STAND_STILL, TESTED_ANGLE_ONLY, VIEW_UNAVAILABLE, HIDING_UNAVAILABLE, AVAILABLE }

/** Controls visible aiming geometry; no recorder-specific eligibility gates. */
class CapsulePreviewRules {
    private var world: Any? = null
    private var previous: ShotVector? = null
    private var stableTicks = 0

    fun reset() { world = null; previous = null; stableTicks = 0 }

    private fun expanded(s: CapsulePreviewState) = s.allowUncalibratedEstimates
    private fun supportedCapsule(s: CapsulePreviewState) = s.capsule==ShotVariant.ORDINARY ||
        expanded(s) && s.capsule==ShotVariant.MASTERFUL
    private fun motionAllowed(s: CapsulePreviewState) = s.position.finite() && s.velocity.finite() &&
        !s.inLava && !s.flying && !s.riding && (expanded(s) || s.stationaryForCalibration)
    private fun within(a: ShotVector, b: ShotVector) = abs(a.x-b.x) <= .001 && abs(a.y-b.y) <= .001 && abs(a.z-b.z) <= .001
    private fun physicalEligibility(s: CapsulePreviewState) = s.enabled && s.inSafari && supportedCapsule(s) &&
        motionAllowed(s) && angle(s)
    private fun angle(s:CapsulePreviewState) = s.pitch.isFinite() && s.pitch in
        (if(s.allowUncalibratedEstimates) -90.0..90.0 else -10.0..1.0)

    fun tick(worldIdentity: Any?, state: CapsulePreviewState?): CapsulePreviewAvailability {
        if (worldIdentity == null || state == null) { reset(); return CapsulePreviewAvailability.HIDDEN }
        if (worldIdentity !== world) { reset(); world = worldIdentity }
        if (!physicalEligibility(state) || state.capsule!=ShotVariant.ORDINARY || !state.stationaryForCalibration) { previous = null; stableTicks = 0 }
        else {
            stableTicks = if (previous == null) 1 else if (within(previous!!, state.position)) (stableTicks + 1).coerceAtMost(2) else 0
            previous = state.position
        }
        return evaluate(state)
    }

    fun evaluate(state: CapsulePreviewState?): CapsulePreviewAvailability {
        val s = state ?: run { stableTicks = 0; return CapsulePreviewAvailability.HIDDEN }
        if (!physicalEligibility(s)) { stableTicks = 0; previous = null }
        if (!s.enabled || !s.inSafari || !supportedCapsule(s) || s.screenOpen || s.hudHidden) return CapsulePreviewAvailability.HIDDEN
        if (!motionAllowed(s) || !expanded(s) && previous?.let { !within(it, s.position) } == true) {
            stableTicks = 0
            return CapsulePreviewAvailability.STAND_STILL
        }
        if (!angle(s)) return CapsulePreviewAvailability.TESTED_ANGLE_ONLY
        if (expanded(s) && world!=null) return CapsulePreviewAvailability.AVAILABLE
        return if (stableTicks >= 2) CapsulePreviewAvailability.AVAILABLE else CapsulePreviewAvailability.STAND_STILL
    }
}
