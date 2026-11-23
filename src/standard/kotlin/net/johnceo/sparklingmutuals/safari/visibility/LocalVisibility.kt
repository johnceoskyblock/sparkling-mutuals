package net.johnceo.sparklingmutuals.safari.visibility

import java.lang.ref.WeakReference
import net.johnceo.sparklingmutuals.compat.smCamera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.painting.Painting
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Recheck at the presentation boundary; never cache a sighting as proof of current visibility. */
internal object LocalVisibility {
    private var paintingLevel = WeakReference<ClientLevel>(null)
    private var paintingTick: Int? = null
    private var paintings = emptyList<AABB>()

    fun entity(entity: Entity, client: Minecraft = Minecraft.getInstance(), bounds: AABB = entity.boundingBox): Boolean {
        val level = client.level ?: return false
        val camera = client.gameRenderer.smCamera.position()
        val headVisible = entity is ArmorStand && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty
        if (entity.isRemoved || !LocalVisibilityPolicy.identifiable(!entity.isInvisible, false, headVisible) ||
            level.getEntity(entity.id) !== entity || !entity.shouldRender(camera.x, camera.y, camera.z) ||
            !inView(bounds, client)) return false
        val center = bounds.center
        return reaches(center, client) ||
            reaches(Vec3(center.x, bounds.minY + bounds.ysize * .8, center.z), client) ||
            reaches(Vec3(center.x, bounds.minY + bounds.ysize * .2, center.z), client) ||
            reaches(center.add(bounds.xsize * .25, 0.0, bounds.zsize * .25), client) ||
            reaches(center.add(-bounds.xsize * .25, 0.0, -bounds.zsize * .25), client)
    }

    /** The vanilla outline pass can draw a whole model. Require clear sight across the body. */
    fun clearModel(entity: Entity, client: Minecraft, bounds: AABB): Boolean {
        if (!entity(entity, client, bounds)) return false
        val center = bounds.center
        if (!reaches(center, client)) return false
        val sample = bounds.deflate(.015)
        for (x in listOf(sample.minX, center.x, sample.maxX)) {
            for (y in listOf(sample.minY, center.y, sample.maxY)) {
                for (z in listOf(sample.minZ, center.z, sample.maxZ)) {
                    if (!reaches(Vec3(x, y, z), client)) return false
                }
            }
        }
        return true
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
        return inView(AABB(pos), client) && reaches(Vec3.atCenterOf(pos), client, pos)
    }

    fun position(pos: Vec3, client: Minecraft = Minecraft.getInstance()): Boolean =
        inView(AABB(pos, pos).inflate(.05), client) && reaches(pos, client)

    fun floorSurface(pos: BlockPos, client: Minecraft): Boolean {
        if (!inView(AABB(pos).move(0.0, 1.0, 0.0).inflate(.02), client)) return false
        val top = Vec3(pos.x + .5, pos.y + 1.02, pos.z + .5)
        return reaches(top, client) || reaches(top.add(.34, 0.0, .34), client) ||
            reaches(top.add(-.34, 0.0, -.34), client) || reaches(top.add(.34, 0.0, -.34), client) ||
            reaches(top.add(-.34, 0.0, .34), client)
    }

    private fun inView(box: AABB, client: Minecraft) =
        client.gameRenderer.smCamera.cullFrustum?.isVisible(box) == true

    private fun reaches(pos: Vec3, client: Minecraft, surface: BlockPos? = null): Boolean {
        val level = client.level ?: return false
        val player = client.player ?: return false
        val eye = client.gameRenderer.smCamera.position()
        val range = minOf(128.0, client.options.renderDistance().get() * 16.0)
        val distance = eye.distanceToSqr(pos)
        if (!distance.isFinite() || distance > range * range || !level.hasChunkAt(BlockPos.containing(pos))) return false
        val hit = level.clip(ClipContext(eye, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player))
        if (hit.type != HitResult.Type.MISS && hit.blockPos != surface) return false
        // Block raycasts miss paintings, including those hiding critters.
        if (paintingLevel.get() !== level || paintingTick != player.tickCount) {
            paintingLevel = WeakReference(level)
            paintingTick = player.tickCount
            paintings = level.entitiesForRendering().filterIsInstance<Painting>()
                .map { it.boundingBox.inflate(.01) }
        }
        val end = hit.location
        return paintings.none { it.contains(eye) || it.clip(eye, end).isPresent }
    }
}
