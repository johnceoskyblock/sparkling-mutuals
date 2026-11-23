package net.johnceo.sparklingmutuals.alerts

import net.minecraft.client.Minecraft

/** Compatibility for shared mode code: requirements use the actual confirmed party roster. */
object SparkleWarping {
    internal val preparingParty get() = false
    fun retainingRequirements(client: Minecraft) = false
    fun requirementMembers(client: Minecraft, actual: Set<String>): Set<String> = actual
}
