package net.johnceo.sparklingmutuals.compat

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.CompareOp
*///? } else {
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
//? }
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.johnceo.sparklingmutuals.SparklingMutuals
import net.johnceo.sparklingmutuals.safari.esp.EspBoxLines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.network.chat.Component
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/** API changes only: all callers retain their world coordinates, colors and visibility rules. */
enum class WorldDepthMode { THROUGH_TERRAIN, OCCLUDED }

object WorldRenderCompat {
    fun renderType(name: String, filled: Boolean = false, cull: Boolean? = false, depth: WorldDepthMode = WorldDepthMode.THROUGH_TERRAIN): RenderType {
        val builder = RenderPipeline.builder(
            if (filled) RenderPipelines.DEBUG_FILLED_SNIPPET else RenderPipelines.LINES_SNIPPET)
            .withLocation(SparklingMutuals.id("pipeline/$name"))
            .withDepthStencilState(DepthStencilState(if(depth==WorldDepthMode.OCCLUDED) CompareOp.LESS_THAN_OR_EQUAL else CompareOp.ALWAYS_PASS, false))
        cull?.let { builder.withCull(it) }
        val pipeline = RenderPipelines.register(builder.build())
        val setup = RenderSetup.builder(pipeline).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        // 26.3 routes feature geometry through the frame's render passes; output targets no longer exist.
        //? if <26.3 {
        setup.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
        //? }
        return RenderType.create("sparkling-mutuals:$name", setup.createRenderSetup())
    }

    fun register(callback: (LevelRenderContext) -> Unit) {
        //? if >=26.2 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register { callback(it) }
        *///? } else {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register { callback(it) }
        //? }
    }

    private fun geometry(context: LevelRenderContext, type: RenderType, draw: (PoseStack.Pose, VertexConsumer) -> Unit) {
        //? if >=26.2 {
        /*context.submitNodeCollector().submitCustomGeometry(context.poseStack(), type) { pose, vertices -> draw(pose, vertices) }
        *///? } else {
        val buffers = context.bufferSource()
        draw(context.poseStack().last(), buffers.getBuffer(type))
        //? }
    }

    fun flush(context: LevelRenderContext, type: RenderType) {
        //? if <26.2 {
        context.bufferSource().endBatch(type)
        //? }
    }

    fun box(context: LevelRenderContext, type: RenderType, worldBox: AABB, color: Int, width: Float = 2f) {
        val camera = Minecraft.getInstance().gameRenderer.smCamera.position()
        val b = worldBox.move(-camera.x, -camera.y, -camera.z)
        geometry(context, type) { pose, vertices ->
            EspBoxLines.edges(b.minX.toFloat(), b.minY.toFloat(), b.minZ.toFloat(), b.maxX.toFloat(), b.maxY.toFloat(), b.maxZ.toFloat()) { x, y, z, ex, ey, ez, axis ->
                val nx = if (axis == 0) 1f else 0f; val ny = if (axis == 1) 1f else 0f; val nz = if (axis == 2) 1f else 0f
                vertices.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width)
                vertices.addVertex(pose, ex, ey, ez).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width)
            }
        }
    }

    fun polyline(context: LevelRenderContext, type: RenderType, worldPoints: List<Vec3>, color: Int, width: Float = 2f) {
        if(!width.isFinite() || width<=0f) return
        val client=Minecraft.getInstance()
        val world=client.level
        val lines=WorldLineGeometry.segments(worldPoints,client.gameRenderer.smCamera.position())
        if(lines.isEmpty()) return
        geometry(context,type) { pose,vertices ->
            if(client.level !== world) return@geometry
            for(line in lines) for(point in listOf(line.from,line.to)) {
                vertices.addVertex(pose,point.x.toFloat(),point.y.toFloat(),point.z.toFloat()).setColor(color)
                    .setNormal(pose,line.normal.x.toFloat(),line.normal.y.toFloat(),line.normal.z.toFloat()).setLineWidth(width)
            }
        }
    }

    fun tile(context: LevelRenderContext, type: RenderType, x: Double, y: Double, z: Double, color: Int) {
        val camera = Minecraft.getInstance().gameRenderer.smCamera.position()
        val rx = (x - camera.x).toFloat(); val ry = (y - camera.y).toFloat(); val rz = (z - camera.z).toFloat()
        geometry(context, type) { pose, vertices ->
            for ((dx, dz) in listOf(0f to 0f, 0f to 1f, 1f to 1f, 1f to 0f))
                vertices.addVertex(pose, rx + dx, ry, rz + dz).setColor(color)
        }
    }

    fun label(context: LevelRenderContext, text: String, color: Int, x: Double, y: Double, z: Double, scale: Float) {
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.smCamera
        val pos = camera.position()
        val poses = context.poseStack()
        poses.pushPose()
        poses.translate(x - pos.x, y - pos.y, z - pos.z)
        //? if >=26.3 {
        /*poses.rotate(camera.rotation())
        *///? } else {
        poses.mulPose(camera.rotation())
        //? }
        poses.scale(scale, -scale, scale)
        //? if >=26.2 {
        /*context.submitNodeCollector().submitText(poses, -client.font.width(text) / 2f, 0f,
            Component.literal(text).visualOrderText, false, Font.DisplayMode.SEE_THROUGH,
            LightCoordsUtil.FULL_BRIGHT, color, 0x40000000, 0)
        *///? } else {
        client.font.drawInBatch(text, -client.font.width(text) / 2f, 0f, color, false,
            Matrix4f(poses.last().pose()), context.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0x40000000, LightCoordsUtil.FULL_BRIGHT)
        //? }
        poses.popPose()
    }
}
