package net.johnceo.sparklingmutuals.contest

import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.SparklingMutuals
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.EspBoxLines
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.core.BlockPos
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.entity.Display
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.Properties

/** Personal honeyhive beacons, verified against loaded server labels and honey levels. */
object HoneyHives {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/honeyhives")
    private const val REMOVED_HIVE = HoneyHiveRules.REMOVED_HIVE
    private const val RANGE = 256.0
    private const val SCAN_RADIUS = 96
    private const val FULL = 0xFF55FF55.toInt()
    private val hives = linkedMapOf<String, Hive>()
    /** Scope -> hive key -> when it refills: the one thing that decides whether a hive is full. */
    private val refillAt = mutableMapOf<String, MutableMap<String, Long>>()
    private val queenFull = mutableMapOf<String, MutableSet<String>>()
    /** The last scope seen, saved, so loots apply straight away after a restart or a server switch. */
    private var lastScope: String? = null
    private var profile: String? = null
    private var scope: String? = null
    private var tracker: HoneyHiveTracker? = null
    private var visible = emptyList<Pair<Hive, Int>>()
    private var ticks = 0
    private var level: Any? = null
    private var onTorrhus = false
    private var beamsBroken = false
    private val file get() = FabricLoader.getInstance().configDir.resolve("sparkling-mutuals-honeyhives.properties")

    private val lines = RenderType.create("sparkling-mutuals:honeyhive_lines",
        RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SparklingMutuals.id("pipeline/honeyhive_lines")).withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).build()))
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup())

    fun register() {
        load()
        // Watching clicks only: neither callback changes or consumes the interaction.
        UseBlockCallback.EVENT.register { player, world, _, hit -> clicked(player, world, hit.blockPos); InteractionResult.PASS }
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ -> clicked(player, world, pos); InteractionResult.PASS }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(SafariRules.strip(message.string)) }
        }
        LevelRenderEvents.COLLECT_SUBMITS.register(::beams)
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(::outlines)
    }

    private fun chat(text: String) {
        HoneyHiveRules.profileId(text)?.let { profile = it; updateScope(Minecraft.getInstance()); return }
        val client = Minecraft.getInstance()
        if (!ConfigManager.honeyHives || !onTorrhus || !SkyblockSidebar.inSkyblock(client)) return
        updateScope(client)
        if (tracker?.chat(text.trim(), System.currentTimeMillis()) == true) save()
    }

    private fun clicked(player: Player, world: Level, pos: BlockPos) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.honeyHives || !onTorrhus || !world.isClientSide || player !== client.player ||
            !world.getBlockState(pos).`is`(Blocks.BEE_NEST)) return
        updateScope(client)
        val key = "${pos.x},${pos.y},${pos.z}"
        if (key == REMOVED_HIVE) return
        if (hives.putIfAbsent(key, Hive(pos.x, pos.y, pos.z)) == null) save()
        tracker?.click(key, System.currentTimeMillis())
    }

    /** Until the join's profile line arrives, track fresh loots under this player only. */
    private fun updateScope(client: Minecraft) {
        val player = client.player?.uuid?.toString()?.replace("-", "")
        val selected = HoneyHiveRules.scope(player, profile)
        val next = selected ?: player?.let { "$it.unknown" }
        ContestTracker.selectScope(selected)
        if (next == scope) return
        val promote = selected != null && scope == "$player.unknown" && tracker != null
        if (!promote) tracker?.resetSession()
        scope = next
        tracker = next?.let { owner ->
            val times = refillAt.getOrPut(owner) { mutableMapOf() }
            val full = queenFull.getOrPut(owner) { mutableSetOf() }
            if (promote) {
                tracker!!.apply { promote(times, full) }.also {
                    refillAt.remove("$player.unknown"); queenFull.remove("$player.unknown")
                }
            } else HoneyHiveTracker(times, full)
        }
        if (selected != null && selected != lastScope) { lastScope = selected; save() }
    }

    fun tick(client: Minecraft) {
        // Only a disconnect forgets the profile: the player is briefly missing on every server switch too.
        if (client.connection == null) { profile = null; tracker?.resetSession(); scope = null; tracker = null }
        if (client.level !== level) { level = client.level; onTorrhus = false; visible = emptyList(); tracker?.resetSession() }
        updateScope(client)
        if (++ticks % 20 != 0) return
        val world = client.level ?: return
        val player = client.player ?: return
        if (!ConfigManager.honeyHives || !SkyblockSidebar.inSkyblock(client)) { visible = emptyList(); return }
        // The positions only mean anything on Torrhus Canyon: confirmed by finding a known hive where it should be.
        if (!onTorrhus) onTorrhus = hives.values.any { val pos = BlockPos(it.x, it.y, it.z)
            world.hasChunkAt(pos) && world.getBlockState(pos).`is`(Blocks.BEE_NEST) }
        if (!onTorrhus || ContestTracker.isComplete()) {
            visible = emptyList(); return
        }
        if (ticks % 40 == 0) survey(client)
        val now = System.currentTimeMillis()
        verifyLoaded(client, now)
        refillAt.values.forEach { it.entries.removeIf { e -> now >= e.value } }
        val refills = scope?.let { refillAt[it] }.orEmpty()
        val shown = mutableListOf<Pair<Hive, Int>>()
        for (hive in hives.values) {
            if (player.distanceToSqr(hive.x + .5, hive.y + .5, hive.z + .5) > RANGE * RANGE) continue
            val refill = refills[hive.key]
            when (HoneyHiveRules.state(now, refill)) {
                HiveState.FULL -> shown += hive to FULL
                HiveState.EMPTY -> Unit
            }
        }
        visible = shown
    }

    /** Learns hives from the build around each one, using the palette to skip sections without a nest. */
    private fun survey(client: Minecraft) {
        val world = client.level ?: return
        val center = client.player?.blockPosition() ?: return
        var added = false
        for (cx in ((center.x - SCAN_RADIUS) shr 4)..((center.x + SCAN_RADIUS) shr 4))
            for (cz in ((center.z - SCAN_RADIUS) shr 4)..((center.z + SCAN_RADIUS) shr 4)) {
                if (!world.hasChunk(cx, cz)) continue
                val chunk = world.getChunk(cx, cz)
                chunk.sections.forEachIndexed { index, section ->
                    if (section.hasOnlyAir() || !section.maybeHas { it.`is`(Blocks.BEE_NEST) }) return@forEachIndexed
                    val bottom = chunk.minY + (index shl 4)
                    for (x in 0..15) for (y in 0..15) for (z in 0..15) {
                        if (!section.getBlockState(x, y, z).`is`(Blocks.BEE_NEST)) continue
                        val pos = BlockPos((cx shl 4) + x, bottom + y, (cz shl 4) + z)
                        if (world.getBlockState(pos.below()).`is`(Blocks.OAK_FENCE) && world.getBlockState(pos.above()).`is`(Blocks.OAK_TRAPDOOR)) {
                            val hive = Hive(pos.x, pos.y, pos.z)
                            if (hive.key == REMOVED_HIVE) continue
                            if (hives.putIfAbsent(hive.key, hive) == null) added = true
                        }
                    }
                }
            }
        if (added) save()
    }

    /** Only inspect loaded known hives; no API response can overwrite what the server shows. */
    private fun verifyLoaded(client: Minecraft, now: Long) {
        // Before profile discovery, retain only confirmed chat loots; provisional block scans must not
        // replace a known profile's saved deadline when its join line finally arrives.
        if (profile == null) return
        val world = client.level ?: return
        val current = tracker ?: return
        val labels = world.entitiesForRendering().mapNotNull { entity ->
            val text = when (entity) {
                is Display.TextDisplay -> entity.text.string
                else -> entity.customName?.string
            }?.let(SafariRules::strip)?.trim() ?: return@mapNotNull null
            if (text != "Click to loot!" && HoneyHiveTracker.countdownMillis(text) == null) return@mapNotNull null
            Triple(entity.x, entity.y, entity.z) to text
        }
        var changed = false
        for (hive in hives.values) {
            val pos = BlockPos(hive.x, hive.y, hive.z)
            if (!world.hasChunkAt(pos)) continue
            val state = world.getBlockState(pos)
            if (!state.`is`(Blocks.BEE_NEST)) continue
            // Associate each label to its closest known nest, preventing adjacent hives sharing a status.
            val label = labels.firstOrNull { (at, _) ->
                val (x, y, z) = at
                val nearest = HoneyHiveRules.nearest(hives.values, x, y - 1, z)
                nearest?.key == hive.key && kotlin.math.abs(x - hive.x - .5) <= 1.5 &&
                    kotlin.math.abs(z - hive.z - .5) <= 1.5 && y in hive.y.toDouble()..(hive.y + 4.0)
            }?.second
            if (current.observe(hive.key, label, state.getValue(BeehiveBlock.HONEY_LEVEL), now)) changed = true
        }
        if (changed) save()
    }

    // ------------------------------------------------------------------ drawing

    private fun beams(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        val world = client.level ?: return
        if (visible.isEmpty() || beamsBroken) return
        try {
            val camera = client.gameRenderer.mainCamera.position()
            val animation = Math.floorMod(world.gameTime, 40L) + client.deltaTracker.getGameTimeDeltaPartialTick(false)
            val poses = context.poseStack()
            for ((hive, color) in visible) {
                // Beams have no transparency, so an unknown hive's dimmer colour becomes a darker one.
                val rgb = if (color ushr 24 < 0xFF) (color and 0xFEFEFE) shr 1 else color and 0xFFFFFF
                poses.pushPose()
                poses.translate(hive.x - camera.x, hive.y - camera.y, hive.z - camera.z)
                BeaconRenderer.submitBeaconBeam(poses, context.submitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1f, animation,
                    0, BeaconRenderer.MAX_RENDER_Y, rgb, BeaconRenderer.SOLID_BEAM_RADIUS, BeaconRenderer.BEAM_GLOW_RADIUS)
                poses.popPose()
            }
        } catch (e: Throwable) {
            // If a future version moves the beacon renderer, fall back to drawn columns rather than nothing.
            beamsBroken = true
            logger.warn("Beacon beams unavailable, drawing columns instead", e)
        }
    }

    private fun outlines(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (client.level == null || client.options.hideGui || visible.isEmpty()) return
        val camera = client.gameRenderer.mainCamera.position()
        val buffers = context.bufferSource()
        val vertices = buffers.getBuffer(lines)
        val pose = context.poseStack().last()
        fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, color: Int) =
            EspBoxLines.edges((minX - camera.x).toFloat(), (minY - camera.y).toFloat(), (minZ - camera.z).toFloat(),
                (maxX - camera.x).toFloat(), (maxY - camera.y).toFloat(), (maxZ - camera.z).toFloat()) { x, y, z, ex, ey, ez, axis ->
                val nx = if (axis == 0) 1f else 0f; val ny = if (axis == 1) 1f else 0f; val nz = if (axis == 2) 1f else 0f
                vertices.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2f)
                vertices.addVertex(pose, ex, ey, ez).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2f)
            }
        for ((hive, color) in visible) {
            box(hive.x.toDouble(), hive.y.toDouble(), hive.z.toDouble(), hive.x + 1.0, hive.y + 1.0, hive.z + 1.0, color)
            if (beamsBroken) box(hive.x + .35, hive.y + 1.0, hive.z + .35, hive.x + .65, hive.y + 64.0, hive.z + .65, color)
        }
        buffers.endBatch(lines)
    }

    // ------------------------------------------------------------------ storage

    private fun load() {
        javaClass.getResourceAsStream("/assets/sparkling-mutuals/honeyhives.txt")?.bufferedReader()?.use { reader ->
            HoneyHiveRules.parse(reader.readText()).forEach { hives[it.key] = it }
        }
        if (!Files.exists(file)) return
        val saved = Properties().apply { Files.newInputStream(file).use(::load) }
        val now = System.currentTimeMillis()
        // One-time migration: earlier API-derived timers were incorrect. Start this revision with all hives full.
        HoneyHiveRules.savedRefills(saved, now).forEach { (owner, times) -> refillAt[owner] = times.toMutableMap() }
        HoneyHiveRules.savedQueenFull(saved).forEach { (owner, keys) -> queenFull[owner] = keys.toMutableSet() }
        // Keep surveyed positions, except the removed non-existent hive; ids no longer control readiness.
        saved.stringPropertyNames().filter { it.startsWith("hive.") }.forEach { name ->
            HoneyHiveRules.parse(name.removePrefix("hive.").replace(',', ' ') + " " + saved.getProperty(name))
                .firstOrNull()?.takeIf { it.key != REMOVED_HIVE }?.let { hives.putIfAbsent(it.key, it) }
        }
        lastScope = saved.getProperty("scope.last")
        save()
    }

    private fun save() {
        val out = Properties()
        out.setProperty("tracking.version", "2")
        hives.values.forEach { out.setProperty("hive.${it.key}", it.id ?: "-") }
        refillAt.forEach { (owner, times) -> times.forEach { (key, at) -> out.setProperty("refill.$owner.$key", at.toString()) } }
        queenFull.forEach { (owner, keys) -> keys.forEach { key -> out.setProperty("full.$owner.$key", "1") } }
        lastScope?.let { out.setProperty("scope.last", it) }
        runCatching { Files.createDirectories(file.parent); Files.newOutputStream(file).use { out.store(it, "Sparkling Mutuals honeyhives") } }
            .onFailure { logger.warn("Couldn't save honeyhives", it) }
    }
}
