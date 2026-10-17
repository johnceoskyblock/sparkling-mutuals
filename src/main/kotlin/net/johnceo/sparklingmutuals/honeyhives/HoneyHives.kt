package net.johnceo.sparklingmutuals.honeyhives

import net.johnceo.sparklingmutuals.compat.*
import java.nio.file.Files
import java.util.Properties
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestTracker
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import org.slf4j.LoggerFactory

/** Personal honeyhive beacons, driven by confirmed loot/refill chat and loaded server holograms. */
object HoneyHives {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/honeyhives")
    private const val REMOVED_HIVE = HoneyHiveRules.REMOVED_HIVE
    private const val RANGE = 256.0
    private const val SCAN_RADIUS = 96
    private const val FULL = 0xFF55FF55.toInt()
    private val hives = linkedMapOf<String, Hive>()
    /** Scope -> hive key -> when it refills: the one thing that decides whether a hive is full. */
    private val refillAt = mutableMapOf<String, MutableMap<String, Long>>()
    private val lootedAt = mutableMapOf<String, MutableMap<String, Long>>()
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

    private val lines = WorldRenderCompat.renderType("honeyhive_lines")

    fun register() {
        load()
        // Watching clicks only: neither callback changes or consumes the interaction.
        UseBlockCallback.EVENT.register { player, world, _, hit -> clicked(player, world, hit.blockPos); InteractionResult.PASS }
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ -> clicked(player, world, pos); InteractionResult.PASS }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(SafariRules.strip(message.string)) }
        }
        LevelRenderEvents.COLLECT_SUBMITS.register(::beams)
        WorldRenderCompat.register(::outlines)
    }

    private fun chat(text: String) {
        HoneyHiveRules.profileId(text)?.let { profile = it; updateScope(Minecraft.getInstance()); return }
        val client = Minecraft.getInstance()
        if (!ConfigManager.honeyHives || !onTorrhus || !SkyblockSidebar.inSkyblock(client)) return
        updateScope(client)
        val now = System.currentTimeMillis()
        if (tracker?.chat(text.trim(), now) == true) {
            save()
            refreshVisible(client, now)
        } else if (text.startsWith("QUEEN BEE!")) logger.info("Unmatched Queen Bee refill: scope={} text={}", scope, text)
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
        val selected = HoneyHiveRules.resumeScope(player, profile, lastScope)
        val next = selected ?: player?.let { "$it.unknown" }
        ContestTracker.selectScope(selected)
        if (next == scope) return
        val promote = selected != null && scope == "$player.unknown" && tracker != null
        if (!promote) tracker?.resetSession()
        scope = next
        tracker = next?.let { owner ->
            val times = refillAt.getOrPut(owner) { mutableMapOf() }
            val full = queenFull.getOrPut(owner) { mutableSetOf() }
            val loots = lootedAt.getOrPut(owner) { mutableMapOf() }
            if (promote) {
                tracker!!.apply { promote(times, full, loots) }.also {
                    refillAt.remove("$player.unknown"); queenFull.remove("$player.unknown"); lootedAt.remove("$player.unknown")
                }
            } else HoneyHiveTracker(times, full, provisional = selected == null, lootedAt = loots)
        }
        if (selected != null && selected != lastScope) { lastScope = selected; save() }
    }

    fun tick(client: Minecraft) {
        // Only a disconnect forgets the profile: the player is briefly missing on every server switch too.
        if (client.connection == null) { profile = null; tracker?.resetSession(); scope = null; tracker = null }
        if (client.level !== level) { level = client.level; onTorrhus = false; visible = emptyList(); tracker?.resetSession() }
        updateScope(client)
        if (++ticks % 20 != 0) return
        val now = System.currentTimeMillis()
        var expired = false
        refillAt.values.forEach { times -> if (times.entries.removeIf { now >= it.value }) expired = true }
        if (expired) save()
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
        verifyLoaded(client, now)
        refreshVisible(client, now)
    }

    private fun refreshVisible(client: Minecraft, now: Long) {
        val player = client.player ?: return
        if (!ConfigManager.honeyHives || !onTorrhus || ContestTracker.isComplete()) { visible = emptyList(); return }
        val refills = scope?.let { refillAt[it] }.orEmpty()
        val shown = mutableListOf<Pair<Hive, Int>>()
        for (hive in hives.values) {
            if (player.distanceToSqr(hive.x + .5, hive.y + .5, hive.z + .5) > RANGE * RANGE) continue
            val refill = refills[hive.key]
            when (tracker?.state(hive.key, now) ?: HoneyHiveRules.state(now, refill)) {
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
        val world = client.level ?: return
        val current = tracker ?: return
        val labels = world.entitiesForRendering().filter { !it.isRemoved && world.getEntity(it.id) === it }.flatMap { entity ->
            val text = if (entity is Display.TextDisplay) entity.text.string else entity.customName?.string
            text?.let { HoneyHiveLabels.statusLines(it).map { line -> HiveLabel(entity.x, entity.y, entity.z, line) } }.orEmpty()
        }
        var changed = false
        for (hive in hives.values) {
            val pos = BlockPos(hive.x, hive.y, hive.z)
            if (!world.hasChunkAt(pos)) continue
            val state = world.getBlockState(pos)
            if (!state.`is`(Blocks.BEE_NEST)) continue
            val label = HoneyHiveLabels.forHive(hive, hives.values, labels)
            if (current.observe(hive.key, label, state.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL), now)) changed = true
        }
        if (changed) save()
    }

    /** Log raw label carriers as well as interpreted state to diagnose server data changes. */
    fun debug(client: Minecraft) {
        val world = client.level ?: return
        val player = client.player ?: return
        logger.info("Hive diagnostics: enabled={} onTorrhus={} profileKnown={} provisional={} visible={}",
            ConfigManager.honeyHives, onTorrhus, profile != null, profile == null, visible.size)
        hives.values.filter { player.distanceToSqr(it.x + .5, it.y + .5, it.z + .5) <= 96.0 * 96.0 }.forEach { hive ->
            val pos = BlockPos(hive.x, hive.y, hive.z)
            logger.info("Hive pos={} loaded={} block={} refillAt={} queenFull={}", hive.key, world.hasChunkAt(pos),
                if (world.hasChunkAt(pos)) world.getBlockState(pos) else null,
                scope?.let { refillAt[it]?.get(hive.key) }, scope?.let { queenFull[it]?.contains(hive.key) })
        }
        world.entitiesForRendering().filter { !it.isRemoved && world.getEntity(it.id) === it && player.distanceToSqr(it) <= 96.0 * 96.0 }.forEach { entity ->
            val raw = if (entity is Display.TextDisplay) entity.text.string else entity.customName?.string
            if (raw != null && (raw.contains("hive", true) || raw.contains("refill", true) || raw.contains("loot", true)))
                logger.info("Hive label carrier: id={} type={} pos={},{},{} raw={} status={}",
                    entity.id, entity.type.toShortString(), entity.x, entity.y, entity.z, raw, HoneyHiveLabels.statusLines(raw))
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun beams(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        val world = client.level ?: return
        if (visible.isEmpty() || beamsBroken) return
        try {
            val camera = client.gameRenderer.smCamera.position()
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
        if (client.level == null || client.options.smHideGui || visible.isEmpty()) return
        fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, color: Int) =
            WorldRenderCompat.box(context, lines, net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ), color)
        for ((hive, color) in visible) {
            box(hive.x.toDouble(), hive.y.toDouble(), hive.z.toDouble(), hive.x + 1.0, hive.y + 1.0, hive.z + 1.0, color)
            if (beamsBroken) box(hive.x + .35, hive.y + 1.0, hive.z + .35, hive.x + .65, hive.y + 64.0, hive.z + .65, color)
        }
        WorldRenderCompat.flush(context, lines)
    }

    // ------------------------------------------------------------------ storage

    private fun load() {
        javaClass.getResourceAsStream("/assets/sparkling-mutuals/honeyhives.txt")?.bufferedReader()?.use { reader ->
            HoneyHiveRules.parse(reader.readText()).forEach { hives[it.key] = it }
        }
        if (!Files.exists(file)) return
        val saved = Properties().apply { Files.newInputStream(file).use(::load) }
        val now = System.currentTimeMillis()
        // Preserve version-2 personal deadlines across upgrades and relogs.
        HoneyHiveRules.savedRefills(saved, now).forEach { (owner, times) -> refillAt[owner] = times.toMutableMap() }
        HoneyHiveRules.savedQueenFull(saved).forEach { (owner, keys) -> queenFull[owner] = keys.toMutableSet() }
        // Keep surveyed positions, except the removed non-existent hive; ids no longer control readiness.
        saved.stringPropertyNames().filter { it.startsWith("hive.") }.forEach { name ->
            HoneyHiveRules.parse(name.removePrefix("hive.").replace(',', ' ') + " " + saved.getProperty(name))
                .firstOrNull()?.takeIf { it.key != REMOVED_HIVE }?.let { hives.putIfAbsent(it.key, it) }
        }
        saved.stringPropertyNames().forEach { name ->
            HoneyHiveRules.parseLoot(name, saved.getProperty(name), "looted")?.let { (owner, key, at) ->
                lootedAt.getOrPut(owner) { mutableMapOf() }[key] = at
            }
        }
        lastScope = saved.getProperty("scope.last")
        save()
    }

    private fun save() {
        val out = Properties()
        out.setProperty("tracking.version", "2")
        hives.values.forEach { out.setProperty("hive.${it.key}", it.id ?: "-") }
        refillAt.forEach { (owner, times) -> times.forEach { (key, at) -> out.setProperty("refill.$owner.$key", at.toString()) } }
        val now = System.currentTimeMillis()
        lootedAt.forEach { (owner, times) -> times.forEach { (key, at) ->
            out.setProperty("looted.$owner.$key", at.toString())
            val current = (if (owner == scope) tracker?.state(key, now) else null)
                ?: HoneyHiveRules.state(now, refillAt[owner]?.get(key))
            out.setProperty("state.$owner.$key", current.name.lowercase())
        } }
        queenFull.forEach { (owner, keys) -> keys.forEach { key -> out.setProperty("full.$owner.$key", "1") } }
        lastScope?.let { out.setProperty("scope.last", it) }
        runCatching { Files.createDirectories(file.parent); Files.newOutputStream(file).use { out.store(it, "Sparkling Mutuals honeyhives") } }
            .onFailure { logger.warn("Couldn't save honeyhives", it) }
    }
}
