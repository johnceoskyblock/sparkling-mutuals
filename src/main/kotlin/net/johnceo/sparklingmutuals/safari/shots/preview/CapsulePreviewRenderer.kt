package net.johnceo.sparklingmutuals.safari.shots.preview

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft

object CapsulePreviewRenderer {
    private val occludedType by lazy { WorldRenderCompat.renderType("capsule_path_preview",depth=WorldDepthMode.OCCLUDED) }
    private val throughType by lazy { WorldRenderCompat.renderType("capsule_path_preview_through",depth=WorldDepthMode.THROUGH_TERRAIN) }
    private val occludedTube by lazy { WorldRenderCompat.renderType("capsule_path_tube",filled=true,cull=true,depth=WorldDepthMode.OCCLUDED) }
    private val throughTube by lazy { WorldRenderCompat.renderType("capsule_path_tube_through",filled=true,cull=true,depth=WorldDepthMode.THROUGH_TERRAIN) }
    fun render(context: LevelRenderContext) {
        val client=Minecraft.getInstance()
        val frame=CapsulePreview.frame(client) ?: return
        val mesh=frame.mesh ?: return
        val type=if(ConfigManager.showCapsuleGuideThroughTerrain) throughType else occludedType
        val tubeType=if(ConfigManager.showCapsuleGuideThroughTerrain) throughTube else occludedTube
        WorldRenderCompat.contactQuads(context,tubeType,mesh.tube.outline)
        WorldRenderCompat.flush(context,tubeType)
        WorldRenderCompat.contactQuads(context,tubeType,mesh.tube.core)
        WorldRenderCompat.flush(context,tubeType)
        WorldRenderCompat.contactLines(context,type,mesh.outline.takeLast(mesh.marker.size))
        WorldRenderCompat.flush(context,type)
        WorldRenderCompat.contactLines(context,type,mesh.marker)
        WorldRenderCompat.flush(context,type)
    }
}
