package net.johnceo.sparklingmutuals.safari

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

/** The Full previews can use loaded encounter models. Standard supplies local sight checks. */
internal object SafariPreviewVisibility {
    fun entity(client: Minecraft, entity: Entity) = true
    fun label(client: Minecraft, entity: Entity) = true
}
