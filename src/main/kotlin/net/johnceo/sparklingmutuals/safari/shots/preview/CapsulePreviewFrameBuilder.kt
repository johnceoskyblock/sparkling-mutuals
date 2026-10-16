package net.johnceo.sparklingmutuals.safari.shots.preview

import net.johnceo.sparklingmutuals.safari.shots.ShotVector

data class CapsulePreviewFrame(val points: List<ShotVector>, val status: String?)

object CapsulePreviewFrameBuilder {
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
