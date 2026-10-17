package net.johnceo.sparklingmutuals.safari.shots.preview

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix4f
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.ContactGuideMesh
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel

object CapsulePreviewRenderer {
    private val occludedType by lazy { WorldRenderCompat.renderType("capsule_path_preview",depth=WorldDepthMode.OCCLUDED) }
    private val throughType by lazy { WorldRenderCompat.renderType("capsule_path_preview_through",depth=WorldDepthMode.THROUGH_TERRAIN) }
    private val shaderOccludedType by lazy { WorldRenderCompat.renderType("capsule_path_preview_shader_occluded",depth=WorldDepthMode.OCCLUDED,afterShaders=true) }
    private val shaderThroughType by lazy { WorldRenderCompat.renderType("capsule_path_preview_shader_through",depth=WorldDepthMode.THROUGH_TERRAIN,afterShaders=true) }
    private data class Pending(val context: LevelRenderContext, val mesh: ContactGuideMesh, val world: ClientLevel, val through: Boolean, val modelView: Matrix4f)
    private var pending: Pending? = null
    fun clearPending() { pending=null }

    fun render(context: LevelRenderContext) {
        if (IrisCapsuleCompat.isShadowPass()) return
        pending = null
        val client=Minecraft.getInstance()
        val frame=CapsulePreview.frame(client) ?: return
        val mesh=frame.mesh ?: return
        val through=ConfigManager.showCapsuleGuideThroughTerrain
        // 26.1.2 has an immediate renderer. Preserve its view matrix until Iris finishes composition.
        //? if <26.2 {
        if (IrisCapsuleCompat.shadersActive()) {
            val world=client.level ?: return
            val poses=PoseStack()
            poses.last().pose().set(context.poseStack().last().pose())
            poses.last().normal().set(context.poseStack().last().normal())
            val captured=object : LevelRenderContext by context { override fun poseStack() = poses }
            pending=Pending(captured,mesh,world,through,Matrix4f(RenderSystem.getModelViewMatrix()))
            return
        }
        //? }
        draw(context,mesh,through,false)
    }

    /** Called after the level's shader final pass; consume once and never carry geometry to another world. */
    fun afterShaders() {
        val captured=pending ?: return
        pending=null
        if (Minecraft.getInstance().level !== captured.world || !IrisCapsuleCompat.shadersActive() || IrisCapsuleCompat.isShadowPass()) return
        val matrices=RenderSystem.getModelViewStack()
        matrices.pushMatrix()
        try {
            matrices.set(captured.modelView)
            draw(captured.context,captured.mesh,captured.through,true)
        } finally { matrices.popMatrix() }
    }
    private fun draw(context: LevelRenderContext, mesh: ContactGuideMesh, through: Boolean, afterShaders: Boolean) {
        val type=if(afterShaders) {
            if(through) shaderThroughType else shaderOccludedType
        } else if(through) throughType else occludedType
        WorldRenderCompat.contactLines(context,type,mesh.outline)
        WorldRenderCompat.flush(context,type)
        WorldRenderCompat.contactLines(context,type,mesh.core+mesh.marker)
        WorldRenderCompat.flush(context,type)
    }
}
