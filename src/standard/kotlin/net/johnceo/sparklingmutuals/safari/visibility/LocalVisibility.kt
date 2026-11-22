package net.johnceo.sparklingmutuals.safari.visibility

import net.johnceo.sparklingmutuals.compat.smCamera
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Recheck at the presentation boundary; never cache a sighting as proof of current visibility. */
internal object LocalVisibility {
    fun entity(entity: Entity, client: Minecraft = Minecraft.getInstance()): Boolean {
        val level = client.level ?: return false
        val camera = client.gameRenderer.smCamera.position()
        val headVisible = entity is ArmorStand && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty
        return !entity.isRemoved && LocalVisibilityPolicy.identifiable(!entity.isInvisible, false, headVisible) && level.getEntity(entity.id) === entity &&
            entity.shouldRender(camera.x, camera.y, camera.z) && position(entity.boundingBox.center, client)
    }

    /** Invisible armor stands can still have a vanilla-visible custom name. */
    fun label(entity: Entity, client: Minecraft = Minecraft.getInstance()): Boolean {
        val level = client.level ?: return false
        val camera = client.gameRenderer.smCamera.position()
        val nameVisible = entity is ArmorStand && entity.isCustomNameVisible && entity.customName != null
        val textVisible = entity is Display.TextDisplay && !entity.text.string.isBlank() && !entity.isInvisible
        val range = entity.distanceToSqr(camera.x, camera.y, camera.z)
        return !entity.isRemoved && level.getEntity(entity.id) === entity && range <= 64.0 * 64.0 &&
            (nameVisible || textVisible || !entity.isInvisible && entity.customName != null) &&
            entity.shouldRender(camera.x, camera.y, camera.z) &&
            position(entity.position().add(0.0, entity.bbHeight + .5, 0.0), client)
    }

    fun sighting(body: Entity, name: Entity, client: Minecraft = Minecraft.getInstance()): Boolean =
        if (body === name) entity(body, client) || label(name, client)
        else entity(body, client) && label(name, client)

    fun block(pos: BlockPos, client: Minecraft = Minecraft.getInstance()): Boolean {
        val level = client.level ?: return false
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir) return false
        return position(Vec3.atCenterOf(pos), client, pos)
    }

    fun position(pos: Vec3, client: Minecraft = Minecraft.getInstance(), surface: BlockPos? = null): Boolean {
        val level = client.level ?: return false
        val player = client.player ?: return false
        val camera = client.gameRenderer.smCamera
        val eye = camera.position()
        val offset = pos.subtract(eye)
        val distance = offset.lengthSqr()
        val range = client.options.renderDistance().get().toDouble() * 16.0
        // Camera rotation, rather than player rotation, also handles third-person view.
        val forward = org.joml.Vector3f(0f, 0f, -1f).rotate(camera.rotation())
        val inView = distance < .01 || offset.normalize().dot(Vec3(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())) >=
            kotlin.math.cos(Math.toRadians(client.options.fov().get().toDouble().coerceIn(30.0, 110.0) / 2.0))
        val loaded = level.hasChunkAt(BlockPos.containing(pos))
        if (!loaded || !inView || distance > range * range) return false
        val hit = level.clip(ClipContext(eye, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player))
        val clear = hit.type == HitResult.Type.MISS || surface != null && hit.blockPos == surface
        return LocalVisibilityPolicy.allows(loaded, true, inView, clear, distance, range * range)
    }
}
