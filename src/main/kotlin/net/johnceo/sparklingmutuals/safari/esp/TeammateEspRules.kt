package net.johnceo.sparklingmutuals.safari.esp

import java.util.UUID

object TeammateEspRules {
    fun supportedArea(mode: String?, area: String?) = if (mode != null)
        mode.equals("safari", true) || mode.equals("foraging_3", true)
    else area in setOf("Torrhus Canyon", "Torrhus Springs", "Critter Safari Entrance", "Critter Safari")

    fun eligible(inSafari: Boolean, rosterKnown: Boolean, uuid: UUID, local: UUID?, members: List<String>) =
        inSafari && rosterKnown && local != null && uuid != local && members.any { it.equals(uuid.toString(), true) }
    fun nameScale(distance: Double) = (distance * .12).coerceAtLeast(1.0)
}
