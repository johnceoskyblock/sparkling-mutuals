package net.johnceo.sparklingmutuals.safari.esp

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes as EntityType
*///? } else {
import net.minecraft.world.entity.EntityType
//? }

/** Outline-only silhouette: no visible replacement block, silverfish model, or world edits. */
object DuplicoGlow {
    private val white = Identifier.fromNamespaceAndPath("sparkling-mutuals", "textures/misc/outline_white.png")
    fun submit(state: EntityRenderState, poses: PoseStack, collector: SubmitNodeCollector) {
        if (state.entityType != EntityType.INTERACTION || !(state as CritterGlowState).`sm$isCritterGlow`()) return
        val w = state.boundingBoxWidth
        val h = state.boundingBoxHeight
        val color = state.outlineColor
        if (color == 0 || !w.isFinite() || !h.isFinite() || w <= 0 || h <= 0) return
        val half = w / 2
        collector.submitCustomGeometry(poses, RenderTypes.outline(white)) { pose, vertices ->
            faces(-half, 0f, -half, half, h, half) { x,y,z ->
                vertices.addVertex(pose,x,y,z).setColor(color).setUv(.5f,.5f)
            }
        }
    }
    internal inline fun faces(x0: Float,y0: Float,z0: Float,x1: Float,y1: Float,z1: Float,
        vertex: (Float,Float,Float)->Unit) {
        vertex(x0,y0,z0);vertex(x0,y0,z1);vertex(x0,y1,z1);vertex(x0,y1,z0)
        vertex(x1,y0,z1);vertex(x1,y0,z0);vertex(x1,y1,z0);vertex(x1,y1,z1)
        vertex(x0,y0,z1);vertex(x0,y0,z0);vertex(x1,y0,z0);vertex(x1,y0,z1)
        vertex(x0,y1,z0);vertex(x0,y1,z1);vertex(x1,y1,z1);vertex(x1,y1,z0)
        vertex(x1,y0,z0);vertex(x0,y0,z0);vertex(x0,y1,z0);vertex(x1,y1,z0)
        vertex(x0,y0,z1);vertex(x1,y0,z1);vertex(x1,y1,z1);vertex(x0,y1,z1)
    }
}
