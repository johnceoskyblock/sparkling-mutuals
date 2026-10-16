package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules

object ShotFeedback {
    /** Canonical GAME dispatch only. No raw message or catcher identity survives this parser. */
    fun parse(text:String):ShotFeedbackObservation? {
        val line=SafariRules.strip(text)
        SafariEspRules.thrownSpecies(line)?.let {
            return ShotFeedbackObservation(ShotFeedbackKind.HIT,it,if(line.startsWith("You threw a Masterful ")) ShotVariant.MASTERFUL else ShotVariant.ORDINARY)
        }
        SafariEspRules.escapedSpecies(line)?.let {
            return ShotFeedbackObservation(ShotFeedbackKind.ESCAPE,it,if(line.contains("Masterful Critter Capsule")) ShotVariant.MASTERFUL else null)
        }
        SafariCatch.parse(line)?.let { return ShotFeedbackObservation(if(it.personal) ShotFeedbackKind.PERSONAL_CAPTURE else ShotFeedbackKind.SHARED_CAPTURE,it.critter.name) }
        return if(line=="The Shyworm fled because it was startled!") ShotFeedbackObservation(ShotFeedbackKind.STARTLE,"Shyworm") else null
    }
}
