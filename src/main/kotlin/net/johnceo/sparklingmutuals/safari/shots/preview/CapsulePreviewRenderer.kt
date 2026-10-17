package net.johnceo.sparklingmutuals.safari.shots.preview

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

object CapsulePreviewRenderer {
    private val type by lazy { WorldRenderCompat.renderType("capsule_path_preview",depth=WorldDepthMode.OCCLUDED) }
    fun render(context: LevelRenderContext) {
        val client=Minecraft.getInstance()
        val frame=CapsulePreview.frame(client) ?: return
        val mesh=frame.mesh ?: return
        WorldRenderCompat.contactLines(context,type,mesh.outline)
        WorldRenderCompat.flush(context,type)
        WorldRenderCompat.contactLines(context,type,mesh.core+mesh.marker)
        WorldRenderCompat.flush(context,type)
    }
}
