package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.*

data class CapsulePreviewFrame(val points: List<ShotVector>, val status: String?,val mesh:ContactGuideMesh?=null)

object CapsulePreviewFrameBuilder {
    fun contacts(trace:ContactTrace,camera:ShotVector,width:Float,estimateLabel:String?=null):CapsulePreviewFrame {
        // The ordinary stationary observation window does not apply to other launch conditions.
        val display=if(estimateLabel==null) trace else trace.copy(nodes=trace.nodes.map{it.copy(observedRegion=false)})
        val reason=when {
            trace.primary?.kind==ContactKind.CRITTER -> "Possible critter contact"
            trace.primary?.kind==ContactKind.TERRAIN -> "Estimated terrain contact"
            trace.stop==ContactStop.UNLOADED -> "Unloaded terrain"
            trace.stop==ContactStop.FLUID -> "Fluid behavior unavailable"
            trace.stop==ContactStop.TARGET_COVERAGE -> "Critter coverage unavailable"
            trace.stop==ContactStop.INVALID -> "Capsule path unavailable"
            else -> "No contact within preview range"
        }
        val partial=trace.primary!=null && trace.warnings.any{it in setOf("scan_pending","target_limit","unsupported_proxy","stale_target","identity_budget")}
        return CapsulePreviewFrame(display.nodes.map{it.position},"Uncalibrated · "+(estimateLabel?.let{"$it\n"} ?: "")+(if(partial)"Provisional\n" else "")+reason+
            (if(partial)"\nOther critters unverified" else "")+
            (if("contact_tie" in trace.warnings) " (ambiguous)" else ""),ContactGuideGeometry.build(display,camera,width))
    }
    fun build(availability: CapsulePreviewAvailability, eye: ShotVector, aim: ShotVector): CapsulePreviewFrame {
        if (availability==CapsulePreviewAvailability.AVAILABLE) {
            val points=CapsulePathModel.points(eye,aim)
            return CapsulePreviewFrame(points,if(points.isEmpty()) "Capsule path unavailable" else "Experimental path")
        }
        val status=when(availability) {
            CapsulePreviewAvailability.HIDDEN -> null
            CapsulePreviewAvailability.STAND_STILL -> "Stand still for capsule path"
            CapsulePreviewAvailability.TESTED_ANGLE_ONLY -> "Look within tested angle"
            CapsulePreviewAvailability.VIEW_UNAVAILABLE -> "Capsule path unavailable in this view"
            CapsulePreviewAvailability.HIDING_UNAVAILABLE -> "Disable capsule hiding for preview"
            CapsulePreviewAvailability.AVAILABLE -> error("Handled above")
        }
        return CapsulePreviewFrame(emptyList(),status)
    }
}
