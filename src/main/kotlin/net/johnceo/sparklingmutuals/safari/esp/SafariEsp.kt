package net.johnceo.sparklingmutuals.safari.esp

//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes as EntityType
*///? } else {
import net.minecraft.world.entity.EntityType
//? }

import net.johnceo.sparklingmutuals.compat.*
import com.mojang.authlib.GameProfile
import java.util.UUID
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.resources.FloorDisplay
import net.johnceo.sparklingmutuals.safari.resources.FloorDropPredictor
import net.johnceo.sparklingmutuals.safari.resources.FloorPickup
import net.johnceo.sparklingmutuals.safari.resources.HauntedCoinDrops
import net.johnceo.sparklingmutuals.safari.resources.ForestBirdFoodDrops
import net.johnceo.sparklingmutuals.safari.resources.PredictedFloorDrop
import net.johnceo.sparklingmutuals.safari.resources.SafariFloorDrops
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionResult
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
import org.slf4j.LoggerFactory

data class EspCritterObservation(val id: Int, val species: String, val biome: SafariBiome, val x: Double, val y: Double, val z: Double, val mound: Boolean, val uuid: UUID)
data class SafariEspObservations(val critters: List<EspCritterObservation>, val floorDrops: List<EspDrop>,
    val scanned: Boolean = false, val hauntedCoinDropsRemaining: Int = -1, val forestBirdFoodDropsRemaining: Int = -1)

/** Nebulune SafariESP identification and box offsets, adapted to our Fabric renderer without Athen. */
object SafariEsp {
    /** Invalidates per-tick contact validation when capture/model eligibility changes. */
    internal var contactRevision=0L
        private set
    private data class Target(val entity: Entity, val species: String, val shulker: Boolean)
    private var targets = emptyList<Target>()
    private var drops = emptyList<EspDrop>()
    private val dropPredictor = FloorDropPredictor()
    private var predictedDrops = emptyList<PredictedFloorDrop>()
    private val coinFloorIds = mutableSetOf<Int>()
    fun resetDrops() { dropPredictor.reset(); predictedDrops = emptyList(); coinFloorIds.clear() }
    fun floorPickup(text: String) {
        val reward = FloorPickup.parse(text) ?: return
        val player = Minecraft.getInstance().player ?: return
        val group = dropPredictor.pickup(reward, player.x, player.y, player.z, System.currentTimeMillis()) ?: return
        logger.info("Floor pickup correlation (nearest-group heuristic): group={} predicted={} actual={} quantity={} match={}",
            group.id, group.type?.label, reward.type.label, reward.quantity, group.type == reward.type)
    }
    private var level: Any? = null
    private var ticks = 0
    private var scanned = false
    private var stringDisplays = emptyList<EspDrop>()
    private val captures = EspCaptureMemory()
    private val modelLabels = EspModelLabels()
    private val motion = EspMotion()
    private val litterbugs = LitterbugProjection()
    private val shyworms = ShywormProjection()
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/esp")
    private val lines = WorldRenderCompat.renderType("safari_esp")
    private val fill = WorldRenderCompat.renderType("safari_drops", filled = true)

    fun register() {
        WorldRenderCompat.register(::render)
        UseItemCallback.EVENT.register { player, world, hand ->
            val client = Minecraft.getInstance()
            val id = player.getItemInHand(hand).get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id", "")
            if (world === client.level && player === client.player && SafariAssist.inSafari &&
                id in setOf("CRITTER_CAPSULE", "MASTERFUL_CRITTER_CAPSULE")) {
                rememberAim(client)
                captures.throwing(System.currentTimeMillis());contactRevision++
            }
            InteractionResult.PASS
        }
    }
    internal fun texture(profile: GameProfile?) = SafariEspRules.textureHash(profile?.properties()?.get("textures")?.firstOrNull()?.value())
    private fun texture(stack: ItemStack) = texture(stack.get(DataComponents.PROFILE)?.partialProfile())
    internal fun describe(entity: Entity): EspEntity {
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
    fun isRockmiteMound(entity: Entity, client: Minecraft): Boolean {
        if (entity.isRemoved || client.level?.getEntity(entity.id) !== entity || !SafariAssist.inSafari) return false
        return entity is Display.ItemDisplay && SafariEspRules.identify(describe(entity))?.name == "Rockmite" ||
            entity.type == EntityType.INTERACTION && SafariStructures.isMound(entity, client)
    }
    private fun modelVisible(entity: Entity): Boolean {
        if (entity !is Display) return true
        val scale = entity.renderState()?.transformation()?.get(1f)?.scale() ?: return false
        return entity.shouldRenderAtSqrDistance(0.0) && SafariEspRules.modelVisible(scale.x(), scale.y(), scale.z())
    }
    /** Keep per-species aim; server chat names the target after the original model can disappear. */
    private fun rememberAim(client: Minecraft) {
        val hit = (client.hitResult as? net.minecraft.world.phys.EntityHitResult)?.entity ?: return
        val target = targets.singleOrNull { it.entity === hit && live(it, client) } ?: return
        if (target.species == "Rockmite" && target.entity is Display.ItemDisplay) return
        captures.sighted(target.entity.uuid, target.species, System.currentTimeMillis())
    }
    fun threw(species: String) { captures.threw(species, System.currentTimeMillis());contactRevision++ }
    fun shywormMessage(text: String, now: Long) {
        val player = Minecraft.getInstance().player ?: return
        shyworms.chat(text, now, player.x, player.z)
    }
    fun escaped(species: String) { captures.escaped(species, System.currentTimeMillis())?.let(modelLabels::release);contactRevision++ }
    fun caught(species: String) {
        contactRevision++
        val id = captures.caught(species, System.currentTimeMillis())
        if (id == null) {
            if (targets.any { it.species == species && trackedModel(it) })
                logger.info("Capture of {} had no throw/model association; checking name-tag lifecycle instead.", species)
            return
        }
        logger.info("Retired captured {} model {}.", species, id)
        targets = targets.filterNot { it.entity.uuid == id }
    }
    fun clearCaptured() { captures.reset(); modelLabels.reset(); motion.reset(); litterbugs.reset(); shyworms.reset();contactRevision++ }
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        SafariFloorDrops.state.visit(SafariAssist.biome)
        if (!SafariAssist.inSafari || client.player == null || client.level == null) {
            targets = emptyList(); drops = emptyList(); stringDisplays = emptyList(); ticks = 0; scanned = false; return
        }
        rememberAim(client)
        if (++ticks < 5) return
        ticks = 0
        contactRevision++
        val strings = mutableListOf<EspDrop>()
        val entities = client.level!!.entitiesForRendering().filter { !it.isRemoved && client.level!!.getEntity(it.id) === it }.toList()
        predictedDrops = dropPredictor.scan(entities.filterIsInstance<Display.ItemDisplay>().map {
            FloorDisplay(it.id, it.x, it.y, it.z,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.itemStack.item).toString() + ":" + SafariRules.strip(it.itemStack.hoverName.string))
        })
        val labels = entities.mapNotNull { entity ->
            entity.customName?.string?.let(SafariEspRules::labelSpecies)?.let { name -> entity to name }
        }.groupBy({ it.second }, { it.first })
        targets = entities.filterNot { captures.hidden(it.uuid) }.mapNotNull { entity ->
            if (entity is Display.ItemDisplay && !entity.isInvisible && entity.itemStack.`is`(Items.STRING)) {
                val pos = entity.blockPosition()
                strings.add(EspDrop(entity.id, pos.x, pos.y, pos.z))
                return@mapNotNull null
            }
            val mob = SafariEspRules.identify(describe(entity)) ?: return@mapNotNull null
            val descriptor = mob.identifiers.any { it.shulker != null }
            Target(entity, mob.name, descriptor)
        }
        val now = System.currentTimeMillis()
        motion.retain(targets.map { it.entity.uuid }.toSet())
        targets.forEach { target ->
            val e = target.entity
            motion.observe(e.uuid, target.species, e.x, e.y, e.z, client.player!!.distanceToSqr(e), now)
            if (target.species == "Litterbug") litterbugs.observe(e.uuid, e.y, now)
        }
        targets.filter(::trackedModel).forEach { target ->
            val nearby = labels[target.species].orEmpty().filter { it.distanceToSqr(target.entity) <= 9 }
            val separate = nearby.filter { it !== target.entity }
            modelLabels.observe(target.entity.uuid, target.species, (separate.ifEmpty { nearby }).map {
                EspModelLabel(it.uuid, target.species, it.distanceToSqr(target.entity), it.isCustomNameVisible)
            }, now)
        }
        val worms = targets.filter { it.species == "Shyworm" }
        shyworms.retain(worms.map { it.entity.uuid }.toSet())
        worms.forEach {
            val e = it.entity
            shyworms.observe(e.uuid, e.x, e.y, e.z,
                modelLabels.label(e.uuid) != null && modelLabels.current(e.uuid, now), now)
        }
        shyworms.reconcile(now)
        targets = targets.filter { live(it, client) }
        SafariTracking.ledger.current?.let { run ->
            run.localPlayer(client.player!!.name.string)
            run.floorCatalog.observe(dropPredictor.knownDrops, entities.filter(::activeFloorDisplay).associate { it.id to it.uuid })
            run.resourceCoverage(run.floorCatalog.complete)
            dropPredictor.knownDrops.filter { it.id in run.floorCatalog.coinIds }.forEach { drop ->
                val members = (0..2).map { client.level!!.getEntity(drop.id + it) }
                val active = members.count(::activeFloorDisplay)
                // Only real STRING reward triples establish slots, not decorative/model triples.
                if (active == 3 || drop.actual?.type == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN)
                    coinFloorIds.add(drop.id)
                if (drop.id in coinFloorIds) {
                    run.gimmiegoldCheck.floor(drop.id,
                        if (active > 0) true else null,
                        collected = drop.actual?.type == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN)
                }
            }
            val groups = predictedDrops.filter { drop -> (0..2).all { activeFloorDisplay(client.level!!.getEntity(drop.id + it)) } }
            val coinSourcesObservable = dropPredictor.knownDrops.filter { it.id in run.floorCatalog.coinIds }.all { drop ->
                drop.actual?.type == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN ||
                    client.level!!.hasChunkAt(net.minecraft.core.BlockPos.containing(drop.x, drop.y, drop.z))
            }
            run.gimmiegoldCheck.sourceEvidence(run.floorCatalog.complete,
                if (coinSourcesObservable) HauntedCoinDrops.remaining(strings, groups) else null)
        }
        stringDisplays = strings.toList()
        drops = SafariEspRules.floorDrops(strings)
        scanned = true
        rememberAim(client)
    }
    private fun trackedModel(target: Target) = SafariEspRules.requiresModelLabel(target.species, target.entity.type.toShortString())
    /** Read-only model/capture validity, independent of ESP mode and visibility switches. */
    internal fun contactCurrent(entity: Entity, species: String, client: Minecraft): Boolean =
        entity !is Player && live(Target(entity,species,false),client)
    private fun live(target: Target, client: Minecraft, forEsp: Boolean = false): Boolean {
        val entity = target.entity
        if (SafariTracking.ledger.current?.sparklingCaptures?.resolved(entity.uuid) == true) return false
        val current = SafariEspRules.targetCurrent(target.species, describe(entity), entity.isRemoved,
            !captures.hidden(entity.uuid) && client.level?.getEntity(entity.id) === entity, entity.level() === client.level)
        val now = System.currentTimeMillis()
        val moving = motion.current(entity.uuid, now)
        val labeled = !trackedModel(target) || modelLabels.current(entity.uuid, now)
        return if (forEsp) SafariEspRules.renderCurrent(target.species, ConfigManager.sparklingMode, current, moving, labeled,
            if (ConfigManager.sparklingMode) ConfigManager.sparklingProfitableShardEsp
            else !ConfigManager.fullClearMode && ConfigManager.profitableShardEsp)
            else current && labeled
    }
    fun debug(client: Minecraft) {
        val player = client.player ?: return
        val entities = client.level?.entitiesForRendering()?.filter { !it.isRemoved && player.distanceToSqr(it) <= 6400 }?.toList() ?: return
        ShywormBodyDiagnostics.capture(client,entities).forEach { logger.info("SM_SHYWORM_BODY {}",it) }
        SafariAssist.diagnostics().forEach { logger.info("{}", it) }
        logger.info("Safari ESP diagnostics: biome={}, fullClear={}, collected={}", SafariAssist.biome,
            ConfigManager.fullClearMode, SafariRoster.all.associate { it.name to (SafariTracking.run?.count(it.name) ?: 0) })
        SafariTracking.run?.let { logger.info("Gimmiegold completion: {} coinDropMarkers={}", it.sparklingChecks.gimmiegoldDebug(), observations().hauntedCoinDropsRemaining) }
        SafariTracking.run?.let { logger.info("Gimmiegold Sparkling check: {}", it.gimmiegoldCheck.debug()) }
        SafariTracking.run?.let { logger.info("Forest birds: {} canReportFd={}", it.birds.debug(), it.canReportBiome(SafariBiome.FOREST)) }
        SafariTracking.run?.let { logger.info("Forest report: {}", it.forestReportDebug(
            net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode, SafariSparklingMode.state)) }
        val incenseIds = SafariFloorDrops.state.incenseDropIds(dropPredictor.knownDrops)
        predictedDrops.forEach { logger.info("Floor prediction: id={} biome={} pos={},{},{} predicted={} actual={} selected={}",
            it.id, it.biome, it.x, it.y, it.z, it.type?.label, it.actual, SafariFloorDrops.selected(it, incenseIds)) }
        entities.mapNotNull { entity -> SafariEspRules.identify(describe(entity))?.let { entity to it.name } }.forEach { (entity, species) ->
            val tag = modelLabels.label(entity.uuid)
            val label = entities.firstOrNull { it.uuid == tag }
            logger.info("ESP model species={} id={} uuid={} type={} pos={} retired={} label={} labelName={} labelVisible={} labelDistance={} labelCurrent={} descriptor={} vehicle={} passengers={} modelName={} nearbyLabels={}",
                species, entity.id, entity.uuid, entity.type.toShortString(), entity.position(), captures.hidden(entity.uuid), tag,
                label?.customName?.string, label?.isCustomNameVisible, label?.distanceToSqr(entity),
                modelLabels.current(entity.uuid, System.currentTimeMillis()), describe(entity), entity.vehicle?.id, entity.passengers.map { it.id }, entity.customName?.string,
                entities.filter { it.customName != null && it.distanceToSqr(entity) <= 9 }.map { "${it.id}:${it.customName?.string}:${it.isCustomNameVisible}" })
        }
    }
    /** Loaded server entities only; observations do not depend on ESP switches or biome filtering. */
    fun observations(): SafariEspObservations {
        val client = Minecraft.getInstance()
        if (!SafariAssist.inSafari || client.level == null || client.level !== level) return SafariEspObservations(emptyList(), emptyList())
        val critters = targets.filter { live(it, client) }.mapNotNull { target ->
            val e = target.entity
            val biome = SafariEspRules.biomeAt(e.x, e.z) ?: return@mapNotNull null
            if (SafariRoster.named(target.species)?.biome != biome) return@mapNotNull null
            EspCritterObservation(e.id, target.species, biome, e.x, e.y, e.z, target.species == "Rockmite" && e is Display.ItemDisplay, e.uuid)
        }
        val rawDrops = liveDrops(client)
        val completeGroups = predictedDrops.filter { drop ->
            (0..2).all { activeFloorDisplay(client.level?.getEntity(drop.id + it)) }
        }
        return SafariEspObservations(critters, rawDrops, scanned,
            HauntedCoinDrops.remaining(rawDrops, completeGroups), ForestBirdFoodDrops.remaining(rawDrops, completeGroups))
    }
    private fun activeFloorDisplay(entity: Entity?) = entity is Display.ItemDisplay && !entity.isRemoved &&
        !entity.isInvisible && entity.itemStack.`is`(Items.STRING)
    private fun liveDrops(client: Minecraft): List<EspDrop> {
        val strings = stringDisplays.filter { drop ->
            val entity = client.level?.getEntity(drop.id) as? Display.ItemDisplay ?: return@filter false
            val p = entity.blockPosition()
            !entity.isRemoved && !entity.isInvisible && entity.itemStack.`is`(Items.STRING) &&
                p.x == drop.x && p.y == drop.y && p.z == drop.z
        }
        // Keep unselected, generic and partial markers as positive resource presence evidence.
        // A type filter or an incomplete triple must never manufacture Haunted absence.
        return (strings + predictedDrops.filter { drop ->
            // A surviving decorative/empty/invisible display is not a remaining floor reward.
            (0..2).any { activeFloorDisplay(client.level?.getEntity(drop.id + it)) }
        }.map { it.marker() }).distinctBy { Triple(it.x, it.y, it.z) }
    }
    private fun bounds(target: Target, delta: Float): AABB {
        val e = target.entity
        val visual = EspBodyBounds.visual(net.johnceo.sparklingmutuals.safari.shots.ShotVector(e.x,e.y,e.z),
            if(e is Display) "display" else if(e is ArmorStand) "armor_stand" else "native",target.shulker)
        val box = visual?.let { AABB(it.min.x,it.min.y,it.min.z,it.max.x,it.max.y,it.max.z) } ?: e.boundingBox
        return EspBoxSize.padded(box.move(e.getPosition(delta).subtract(e.position())), target.species)
    }
    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (!SafariAssist.inSafari || client.player == null || client.level == null || client.options.smHideGui) return
        val camera = client.gameRenderer.smCamera.position()
        val playerBiome = SafariAssist.biome
        fun frame(box: AABB, color: Int) = WorldRenderCompat.box(context, lines, box, color)
        val delta = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val now = System.currentTimeMillis()
        val timers = mutableListOf<Triple<String, Int, AABB>>()
        targets = targets.filter { live(it, client) }
        for (target in targets) {
            val e = target.entity
            val mob = SafariRoster.named(target.species)!!
            val group = SafariEspConfig.groups.getValue(mob.biome.name.lowercase())
            val mobBiome = SafariEspRules.biomeAt(e.x, e.z)
            val forceIcy = mob.biome == SafariBiome.ICY && SafariSparklingMode.state.needs("Wumpa") &&
                (SafariTracking.ledger.current?.count(mob.name) ?: 0) == 0
            if (!forceIcy && !live(target, client, forEsp = true)) continue
            val needed = if (forceIcy) true else if (ConfigManager.sparklingMode)
                SafariEspRules.neededForSparkling(mob.name, mob.name == "Rockmite" && e is Display.ItemDisplay,
                    SafariSparklingMode.state, ConfigManager.sparklingProfitableShardEsp, SafariTracking.ledger.current, e.uuid, now)
            else SafariEspRules.neededForRun(mob.name, e is Display.ItemDisplay, SafariTracking.ledger.current, ConfigManager.fullClearMode,
                ConfigManager.profitableShardEsp, SafariSparklingMode.state)
            if (needed &&
                SafariEspConfig.entityEnabled(mob.name, e is Display.ItemDisplay) && mobBiome == mob.biome && SafariEspRules.visible(true, group.enabled, group.onlyInBiome, playerBiome, mobBiome)) {
                val box = bounds(target, delta)
                val height = when (mob.name) {
                    "Litterbug" -> litterbugs.hiddenHeight(e.uuid, e.x, e.y, e.z)
                    "Shyworm" -> shyworms.hiddenHeight(e.uuid)
                    else -> null
                }
                val projected = if (height == null) box else box.move(0.0, height - e.getPosition(delta).y, 0.0)
                frame(projected, SafariEspConfig.rgb(SafariEspConfig.entityColor(mob.name, e is Display.ItemDisplay)))
                if (mob.name == "Litterbug" && height != null) litterbugs.hiddenMarker(e.uuid, e.x, e.y, e.z, now)?.let {
                    timers.add(Triple(it.text, it.color, projected))
                }
                if (mob.name == "Shyworm" && !ConfigManager.sparklingMode) {
                    shyworms.timer(e.uuid, now)?.let { timers.add(Triple(it.text, it.color, projected)) }
                    shyworms.path(e.uuid, now)?.let {
                        val y = it.height + .02
                        frame(AABB(it.minX, y, it.minZ, it.maxX, y, it.maxZ), 0xFFFF0000.toInt())
                    }
                }
            }
        }
        val floor = SafariEspConfig.groups.getValue("floor")
        val incenseIds = SafariFloorDrops.state.incenseDropIds(dropPredictor.knownDrops)
        val selectedDrops = predictedDrops.filter { drop ->
            drop.actual == null && (0..2).all { client.level?.getEntity(drop.id + it)?.isRemoved == false } &&
                SafariEspRules.visible(true, SafariFloorDrops.selected(drop, incenseIds), floor.onlyInBiome, playerBiome, drop.biome)
        }
        val tiles = selectedDrops.map { it.marker() to SafariEspConfig.rgb(SafariEspConfig.dropColor(it.type!!)) }
        selectedDrops.forEach { timers.add(Triple(it.type!!.label, SafariEspConfig.rgb(SafariEspConfig.dropColor(it.type!!)),
            AABB(it.x - .5, it.y, it.z - .5, it.x + .5, it.y + 1, it.z + .5))) }
        tiles.forEach { (tile, color) -> frame(AABB(tile.x.toDouble(), tile.y + 1.0, tile.z.toDouble(), tile.x + 1.0, tile.y + 1.0, tile.z + 1.0), color) }
        WorldRenderCompat.flush(context, lines)
        for ((tile, color) in tiles) {
            WorldRenderCompat.tile(context, fill, tile.x.toDouble(), tile.y + 1.0, tile.z.toDouble(),
                color and 0xFFFFFF or ((color ushr 25) shl 24))
        }
        WorldRenderCompat.flush(context, fill)
        for ((text, color, box) in timers) {
            WorldRenderCompat.label(context, text, color, box.center.x, box.maxY + .2, box.center.z, SafariEspConfig.labelWorldScale)
        }
    }
    fun reset() { resetDrops(); targets = emptyList(); drops = emptyList(); level = null; ticks = 0; stringDisplays = emptyList(); scanned = false; clearCaptured() }
}
