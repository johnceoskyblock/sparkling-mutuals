package net.johnceo.sparklingmutuals.safari

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.johnceo.sparklingmutuals.SparklingMutuals
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.fish.TropicalFish
import net.minecraft.world.entity.animal.parrot.Parrot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.minecraft.world.phys.AABB

/** Nebulune SafariESP identification and box offsets, adapted to our Fabric renderer without Athen. */
object SafariEsp {
    private data class Target(val entity: Entity, val species: String, val shulker: Boolean)
    private var targets = emptyList<Target>()
    private var drops = emptyList<EspDrop>()
    private var level: Any? = null
    private var ticks = 0
    private fun renderType(name: String, snippet: RenderPipeline.Snippet) = RenderType.create("sparkling-mutuals:$name",
        RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(snippet)
            .withLocation(SparklingMutuals.id("pipeline/$name"))
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).build()))
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup())
    private val lines = renderType("safari_esp", RenderPipelines.LINES_SNIPPET)
    private val fill = renderType("safari_drops", RenderPipelines.DEBUG_FILLED_SNIPPET)

    fun register() { LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(::render) }
    private fun texture(profile: GameProfile?) = SafariEspRules.textureHash(profile?.properties()?.get("textures")?.firstOrNull()?.value())
    private fun texture(stack: ItemStack) = texture(stack.get(DataComponents.PROFILE)?.partialProfile())
    private fun describe(entity: Entity): EspEntity {
        val item = (entity as? Display.ItemDisplay)?.itemStack
        val shulker = when (entity) {
            is Shulker -> entity.color
            is Display.BlockDisplay -> (entity.blockState.block as? ShulkerBoxBlock)?.color
            is Display.ItemDisplay -> (Block.byItem(entity.itemStack.item) as? ShulkerBoxBlock)?.color
            else -> null
        }
        val texture = when (entity) {
            is ArmorStand -> texture(entity.getItemBySlot(EquipmentSlot.HEAD))
            is Display.ItemDisplay -> texture(item!!)
            is Player -> texture(entity.gameProfile)
            else -> null
        }
        return EspEntity(entity.type.toShortString(), texture, shulker?.name,
            (entity as? TropicalFish)?.let { "${it.pattern.name}/${it.baseColor.name}/${it.patternColor.name}" },
            (entity as? Parrot)?.variant?.name, entity.isInvisible, entity.passengers.isNotEmpty())
    }
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        if (!SafariAssist.inSafari || client.player == null || client.level == null || SafariEspConfig.groups.values.none { it.enabled }) {
            targets = emptyList(); drops = emptyList(); ticks = 0; return
        }
        if (++ticks < 5) return
        ticks = 0
        val strings = mutableListOf<EspDrop>()
        targets = client.level!!.entitiesForRendering().filterNot { it.isRemoved }.mapNotNull { entity ->
            if (entity is Display.ItemDisplay && entity.itemStack.`is`(Items.STRING)) {
                val pos = entity.blockPosition()
                strings.add(EspDrop(entity.id, pos.x, pos.y, pos.z))
                return@mapNotNull null
            }
            val mob = SafariEspRules.identify(describe(entity)) ?: return@mapNotNull null
            val descriptor = mob.identifiers.any { it.shulker != null }
            Target(entity, mob.name, descriptor)
        }.toList()
        drops = SafariEspRules.floorDrops(strings)
    }
    private fun bounds(target: Target, delta: Float): AABB {
        val e = target.entity
        val box = when {
            e is Display && target.shulker -> AABB(e.x - .5, e.y, e.z - .5, e.x + .5, e.y + 1, e.z + .5)
            e is Display -> AABB(e.x - .3, e.y - .45, e.z - .3, e.x + .3, e.y + .15, e.z + .3)
            e is ArmorStand -> AABB(e.x - .3, e.y + 1.35, e.z - .3, e.x + .3, e.y + 1.95, e.z + .3)
            else -> e.boundingBox
        }
        return box.move(e.getPosition(delta).subtract(e.position()))
    }
    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (!SafariAssist.inSafari || client.player == null || client.level == null || client.options.hideGui) return
        val camera = client.gameRenderer.mainCamera.position()
        val playerBiome = SafariEspRules.biomeAt(client.player!!.x, client.player!!.z)
        val poses = context.poseStack()
        val buffers = context.bufferSource()
        val vertices = buffers.getBuffer(lines)
        val pose = poses.last()
        fun frame(box: AABB, color: Int) {
            val b = box.move(-camera.x, -camera.y, -camera.z)
            val corners = List(8) { i -> floatArrayOf((if (i and 1 == 0) b.minX else b.maxX).toFloat(),
                (if (i and 2 == 0) b.minY else b.maxY).toFloat(), (if (i and 4 == 0) b.minZ else b.maxZ).toFloat()) }
            for (i in 0..7) for (bit in listOf(1, 2, 4)) if (i and bit == 0) {
                val axis = if (bit == 1) 0 else if (bit == 2) 1 else 2
                if (corners[i].contentEquals(corners[i or bit])) continue
                for (endpoint in listOf(i, i or bit)) {
                    val p = corners[endpoint]
                    vertices.addVertex(pose, p[0], p[1], p[2]).setColor(color)
                        .setNormal(pose, if (axis == 0) 1f else 0f, if (axis == 1) 1f else 0f, if (axis == 2) 1f else 0f).setLineWidth(2f)
                }
            }
        }
        val delta = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        for (target in targets) {
            val e = target.entity
            if (e.isRemoved || e.level() !== client.level) continue
            val mob = SafariRoster.named(target.species)!!
            val group = SafariEspConfig.groups.getValue(mob.biome.name.lowercase())
            val setting = SafariEspConfig.mobs.getValue(mob.name)
            val mobBiome = SafariEspRules.biomeAt(e.x, e.z)
            if (setting.enabled && mobBiome == mob.biome && SafariEspRules.visible(true, group.enabled, group.onlyInBiome, playerBiome, mobBiome))
                frame(bounds(target, delta), SafariEspConfig.rgb(setting.color))
        }
        val floor = SafariEspConfig.groups.getValue("floor")
        val tiles = drops.filter { SafariEspRules.visible(true, floor.enabled, floor.onlyInBiome, playerBiome,
            SafariEspRules.biomeAt(it.x.toDouble(), it.z.toDouble())) }
        val color = SafariEspConfig.rgb(SafariEspConfig.floorColor)
        tiles.forEach { frame(AABB(it.x.toDouble(), it.y + 1.0, it.z.toDouble(), it.x + 1.0, it.y + 1.0, it.z + 1.0), color) }
        buffers.endBatch(lines)
        val quads = buffers.getBuffer(fill)
        for (tile in tiles) {
            val x = (tile.x - camera.x).toFloat(); val y = (tile.y + 1 - camera.y).toFloat(); val z = (tile.z - camera.z).toFloat()
            for ((dx, dz) in listOf(0f to 0f, 0f to 1f, 1f to 1f, 1f to 0f))
                quads.addVertex(pose, x + dx, y, z + dz).setColor(color and 0xFFFFFF or ((color ushr 25) shl 24))
        }
        buffers.endBatch(fill)
    }
    fun reset() { targets = emptyList(); drops = emptyList(); level = null; ticks = 0 }
}
