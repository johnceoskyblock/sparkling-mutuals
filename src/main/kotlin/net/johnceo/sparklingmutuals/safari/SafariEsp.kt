package net.johnceo.sparklingmutuals.safari

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.johnceo.sparklingmutuals.SparklingMutuals
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
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

data class EspCritterObservation(val id: Int, val species: String, val biome: SafariBiome, val x: Double, val y: Double, val z: Double, val mound: Boolean)
data class SafariEspObservations(val critters: List<EspCritterObservation>, val floorDrops: List<EspDrop>, val scanned: Boolean = false)

/** Nebulune SafariESP identification and box offsets, adapted to our Fabric renderer without Athen. */
object SafariEsp {
    private data class Target(val entity: Entity, val species: String, val shulker: Boolean)
    private var targets = emptyList<Target>()
    private var drops = emptyList<EspDrop>()
    private var level: Any? = null
    private var ticks = 0
    private var scanned = false
    private var stringDisplays = emptyList<EspDrop>()
    private val captures = EspCaptureMemory()
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
            (entity as? Parrot)?.variant?.name, entity.isInvisible || !modelVisible(entity), entity.passengers.isNotEmpty())
    }
    private fun modelVisible(entity: Entity): Boolean {
        if (entity !is Display) return true
        val scale = entity.renderState()?.transformation()?.get(1f)?.scale() ?: return false
        return entity.shouldRenderAtSqrDistance(0.0) && SafariEspRules.modelVisible(scale.x(), scale.y(), scale.z())
    }
    /** Associate a throw with the aimed model before capture removes or hides it. */
    fun threw(client: Minecraft) {
        val player = client.player ?: return
        val eye = player.eyePosition
        val look = player.lookAngle
        val candidates = targets.filter { live(it, client) }.map {
            val offset = it.entity.position().subtract(eye)
            val along = offset.dot(look)
            EspCaptureCandidate(it.entity.id, it.species, SafariEspRules.captureModel(it.entity.type.toShortString()),
                it.species == "Rockmite" && it.entity is Display.ItemDisplay,
                player.distanceToSqr(it.entity), if (along < 0) Double.POSITIVE_INFINITY else (offset.lengthSqr() - along * along).coerceAtLeast(0.0))
        }
        val aimed = candidates.filter { it.display && !it.mound && it.rangeSquared <= 80.0 * 80 && it.aimSquared <= 16 }
            .minByOrNull { it.aimSquared + it.rangeSquared * .001 } ?: return
        val id = SafariEspRules.capturedDisplay(aimed.species, candidates.filter { it.aimSquared <= 16 }) ?: return
        targets.firstOrNull { it.entity.id == id }?.let { captures.aimed(it.entity.uuid, it.species, System.currentTimeMillis()) }
    }
    fun caught(species: String) {
        val id = captures.caught(species, System.currentTimeMillis()) ?: return
        targets = targets.filterNot { it.entity.uuid == id }
    }
    fun clearCaptured() { captures.reset() }
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        if (!SafariAssist.inSafari || client.player == null || client.level == null) {
            targets = emptyList(); drops = emptyList(); stringDisplays = emptyList(); ticks = 0; scanned = false; return
        }
        if (++ticks < 5) return
        ticks = 0
        val strings = mutableListOf<EspDrop>()
        targets = client.level!!.entitiesForRendering().filter { !it.isRemoved && !captures.hidden(it.uuid) && client.level!!.getEntity(it.id) === it }.mapNotNull { entity ->
            if (entity is Display.ItemDisplay && !entity.isInvisible && entity.itemStack.`is`(Items.STRING)) {
                val pos = entity.blockPosition()
                strings.add(EspDrop(entity.id, pos.x, pos.y, pos.z))
                return@mapNotNull null
            }
            val mob = SafariEspRules.identify(describe(entity)) ?: return@mapNotNull null
            val descriptor = mob.identifiers.any { it.shulker != null }
            Target(entity, mob.name, descriptor)
        }.toList()
        stringDisplays = strings.toList()
        drops = SafariEspRules.floorDrops(strings)
        scanned = true
    }
    private fun live(target: Target, client: Minecraft): Boolean {
        val entity = target.entity
        return SafariEspRules.targetCurrent(target.species, describe(entity), entity.isRemoved,
            !captures.hidden(entity.uuid) && client.level?.getEntity(entity.id) === entity, entity.level() === client.level)
    }
    /** Loaded server entities only; observations do not depend on ESP switches or biome filtering. */
    fun observations(): SafariEspObservations {
        val client = Minecraft.getInstance()
        if (!SafariAssist.inSafari || client.level == null || client.level !== level) return SafariEspObservations(emptyList(), emptyList())
        targets = targets.filter { live(it, client) }
        val critters = targets.mapNotNull { target ->
            val e = target.entity
            val biome = SafariEspRules.biomeAt(e.x, e.z) ?: return@mapNotNull null
            if (SafariRoster.named(target.species)?.biome != biome) return@mapNotNull null
            EspCritterObservation(e.id, target.species, biome, e.x, e.y, e.z, target.species == "Rockmite" && e is Display.ItemDisplay)
        }
        return SafariEspObservations(critters, liveDrops(client), scanned)
    }
    private fun liveDrops(client: Minecraft): List<EspDrop> {
        val strings = stringDisplays.filter { drop ->
            val entity = client.level?.getEntity(drop.id) as? Display.ItemDisplay ?: return@filter false
            val p = entity.blockPosition()
            !entity.isRemoved && !entity.isInvisible && entity.itemStack.`is`(Items.STRING) &&
                p.x == drop.x && p.y == drop.y && p.z == drop.z
        }
        return SafariEspRules.floorDrops(strings)
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
        targets = targets.filter { live(it, client) }
        for (target in targets) {
            val e = target.entity
            val mob = SafariRoster.named(target.species)!!
            val group = SafariEspConfig.groups.getValue(mob.biome.name.lowercase())
            val mobBiome = SafariEspRules.biomeAt(e.x, e.z)
            if (SafariEspRules.neededForRun(mob.name, e is Display.ItemDisplay, SafariTracking.ledger.current, ConfigManager.fullClearMode) &&
                SafariEspConfig.entityEnabled(mob.name, e is Display.ItemDisplay) && mobBiome == mob.biome && SafariEspRules.visible(true, group.enabled, group.onlyInBiome, playerBiome, mobBiome))
                frame(bounds(target, delta), SafariEspConfig.rgb(SafariEspConfig.entityColor(mob.name, e is Display.ItemDisplay)))
        }
        val floor = SafariEspConfig.groups.getValue("floor")
        val tiles = liveDrops(client).filter { SafariEspRules.visible(true, floor.enabled, floor.onlyInBiome, playerBiome,
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
    fun reset() { targets = emptyList(); drops = emptyList(); level = null; ticks = 0; stringDisplays = emptyList(); scanned = false; clearCaptured() }
}
