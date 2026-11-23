package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.visibility.LocalVisibility
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

internal object SafariPreviewVisibility {
    fun entity(client: Minecraft, entity: Entity) = LocalVisibility.entity(entity, client)
    fun label(client: Minecraft, entity: Entity) = LocalVisibility.label(entity, client)
}
