package net.johnceo.sparklingmutuals.safari.shots.preview

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.johnceo.sparklingmutuals.compat.*
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3

object CapsulePreviewRenderer {
    private val type by lazy { WorldRenderCompat.renderType("capsule_path_preview",depth=WorldDepthMode.OCCLUDED) }
    fun render(context: LevelRenderContext) {
        val client=Minecraft.getInstance()
        val available=CapsulePreview.availability(CapsulePreview.currentState(client))
        if(available!=CapsulePreviewAvailability.AVAILABLE) return
        val player=client.player ?: return
        val partial=client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val frame=CapsulePreviewFrameBuilder.build(available,ShotPacketCompat.vector(player.getEyePosition(partial)),
            ShotPacketCompat.vector(player.getViewVector(partial)))
        if(frame.points.isEmpty()) return
        WorldRenderCompat.polyline(context,type,frame.points.map { Vec3(it.x,it.y,it.z) },0xFFFFB347.toInt(),2f)
        WorldRenderCompat.flush(context,type)
    }
}
