package net.johnceo.sparklingmutuals.safari

import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.johnceo.sparklingmutuals.SparklingMutuals
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.core.BlockPos
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.Level
import net.minecraft.core.registries.BuiltInRegistries
import org.slf4j.LoggerFactory
import org.joml.Matrix4f

/** Forest's fixed nest survey and waypoints; observe loaded blocks without requesting chunks. */
object BeeNests {
    private val survey = BeeNestSurvey()
    private val positions = BeeNestSurvey.positions.associateWith { (x, y, z) -> BlockPos(x, y, z) }
    private var ticks = 0
    private var renderCallbacks = 0L
    private var lastRenderedMarkers = 0
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/nests")
    private val handledByParty get() = SafariTracking.ledger.current?.partyNestsHandled == true
    val unpunchedCount get() = if (handledByParty && SafariFullClear.mode == SafariMode.SPARKLING) 0 else survey.remaining.size
    val punchedCount get() = survey.punchedCount
    val nestCount get() = survey.nestCount
    val allChecked get() = survey.allChecked
    private val lines = RenderType.create("sparkling-mutuals:nests", RenderSetup.builder(RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET).withLocation(SparklingMutuals.id("pipeline/nests"))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).build()))
        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET).createRenderSetup())
    private val corners = List(8) { i -> floatArrayOf(if (i and 1 == 0) -.005f else 1.005f,
        if (i and 2 == 0) -.005f else 1.005f, if (i and 4 == 0) -.005f else 1.005f) }

    fun register() {
        AttackBlockCallback.EVENT.register { _, level, _, pos, _ ->
            clicked(level, pos)
            InteractionResult.PASS
        }
        UseBlockCallback.EVENT.register { _, level, _, hit ->
            clicked(level, hit.blockPos)
            InteractionResult.PASS
        }
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(::render)
    }
    private fun clicked(level: Level, pos: BlockPos) {
        if (!level.isClientSide || !SafariAssist.inSafari || SafariTracking.ledger.current == null ||
            !level.isLoaded(pos) || !level.getBlockState(pos).`is`(Blocks.BEE_NEST)) return
        val site = Triple(pos.x, pos.y, pos.z)
        survey.observe(site, NestBlock.NEST)
        survey.punch(site)
        syncRun()
    }
    private fun syncRun() {
        SafariTracking.ledger.current?.updateNestEvidence(survey.allChecked, survey.punchedCount,
            survey.nestCount, survey.unverified.size)
    }
    fun tick(client: Minecraft) {
        val level = client.level ?: return
        if (!SafariAssist.inSafari || SafariTracking.ledger.current == null) return
        // The server may send Forest chunks before the player's Forest area is mapped.
        // Inspect their actual data immediately on arrival, then every half second.
        if (ticks++ % 10 != 0) return
        survey.scan { site ->
            val pos = positions.getValue(site)
            if (!level.isLoaded(pos)) return@scan null
            val block = level.getBlockState(pos)
            when {
                block.`is`(Blocks.BEE_NEST) -> NestBlock.NEST
                block.`is`(Blocks.BEEHIVE) -> NestBlock.HIVE
                else -> NestBlock.UNKNOWN
            }
        }
        syncRun()
    }
    private fun render(context: LevelRenderContext) {
        renderCallbacks++
        lastRenderedMarkers = 0
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        if (client.player == null || client.options.hideGui || !SafariAssist.inSafari) return
        val camera = client.gameRenderer.mainCamera.position()
        val poses = context.poseStack()
        val buffers = context.bufferSource()
        val vertices = buffers.getBuffer(lines)
        val markers = when {
            SafariAssist.biome == SafariBiome.FOREST &&
                SafariHelperRules.showNests(SafariFullClear.mode, SafariTracking.ledger.current, SafariSparklingMode.state) ->
                survey.waypoints(SafariHelperRules.showNests(SafariFullClear.mode,
                    SafariTracking.ledger.current, SafariSparklingMode.state)).mapNotNull { waypoint ->
                    val pos = positions.getValue(waypoint.position)
                    val loaded = level.isLoaded(pos)
                    if (waypoint.nest && loaded && !level.getBlockState(pos).`is`(Blocks.BEE_NEST)) null
                    else Triple(pos, when {
                        !waypoint.nest -> "Survey site ${pos.x} ${pos.y} ${pos.z}"
                        !loaded -> "Nest (load area)"
                        else -> "Nest"
                    }, if (waypoint.nest) SafariEspConfig.rgb(AppearanceConfig.nestColor) else 0xFFAAAAAA.toInt())
                }
            SafariAssist.biome == SafariBiome.CAVERN &&
                SafariHelperRules.needed("Snoozle", SafariFullClear.mode, SafariTracking.ledger.current, SafariSparklingMode.state) ->
                SafariStructures.intactWalls.filter { level.isLoaded(it) && !level.getBlockState(it).isAir &&
                    it.distToCenterSqr(camera) < 40000 }.map { Triple(it, "Snooper wall", SafariEspConfig.rgb(AppearanceConfig.snooperColor)) }
            else -> emptyList()
        }
        lastRenderedMarkers = markers.size
        for ((pos, _, color) in markers) {
            poses.pushPose(); poses.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
            val pose = poses.last()
            corners.forEachIndexed { i, a ->
                for (bit in listOf(1, 2, 4)) if (i and bit == 0) {
                    val b = corners[i or bit]
                    val axis = if (bit == 1) 0 else if (bit == 2) 1 else 2
                    repeat(2) { endpoint ->
                        val point = if (endpoint == 0) a else b
                        vertices.addVertex(pose, point[0], point[1], point[2]).setColor(color)
                            .setNormal(pose, if (axis == 0) 1f else 0f, if (axis == 1) 1f else 0f, if (axis == 2) 1f else 0f).setLineWidth(3f)
                    }
                }
            }
            poses.popPose()
        }
        buffers.endBatch(lines)
        for ((pos, label, color) in markers) {
            val text = "$label · ${kotlin.math.sqrt(pos.distToCenterSqr(camera)).toInt()}m"
            poses.pushPose(); poses.translate(pos.x + .5 - camera.x, pos.y + 1.4 - camera.y, pos.z + .5 - camera.z)
            poses.mulPose(client.gameRenderer.mainCamera.rotation()); poses.scale(.025f, -.025f, .025f)
            client.font.drawInBatch(text, -client.font.width(text) / 2f, 0f, color, false,
                Matrix4f(poses.last().pose()), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, LightCoordsUtil.FULL_BRIGHT)
            poses.popPose()
        }
    }
    /** Explain the block-data -> survey -> helper -> render boundaries without assuming a GPU failure. */
    fun debug(client: Minecraft) {
        val level = client.level ?: return
        val run = SafariTracking.ledger.current
        logger.info("Nest diagnostics: safari={} biome={} activeRun={} mode={} partyReady={} partyNeedsHoneybug={} helper={} hideGui={} callbacks={} lastMarkers={} knownNests={} unpunched={} unknown={} checked={} captures={} personal={} target={}",
            SafariAssist.inSafari, SafariAssist.biome, run != null, SafariFullClear.mode, SafariSparklingMode.state.ready,
            SafariSparklingMode.state.needs("Honeybug"), SafariHelperRules.showNests(SafariFullClear.mode, run, SafariSparklingMode.state),
            client.options.hideGui, renderCallbacks, lastRenderedMarkers, survey.nestCount, survey.remaining.size,
            survey.unverified.size, survey.allChecked, run?.count("Honeybug"), run?.personalCount("Honeybug"), run?.honeybugCaptureTarget)
        positions.forEach { (site, pos) ->
            val loaded = level.isLoaded(pos)
            logger.info("Nest site {} loaded={} block={} classification={} pending={}", site, loaded,
                if (loaded) BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).block) else "unavailable",
                survey.classification(site), site in survey.remaining)
        }
    }
    fun reset() { survey.reset(); ticks = 0; renderCallbacks = 0; lastRenderedMarkers = 0 }
}
