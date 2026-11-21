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
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.CritterNameplates
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
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
import net.minecraft.world.entity.animal.armadillo.Armadillo
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
    val scanned: Boolean = false, val hauntedCoinDropsRemaining: Int = -1, val forestBirdFoodDropsRemaining: Int = -1,
    val forestBirdFoodByType: Map<String,Int>?=null)

/** Nebulune SafariESP identification and box offsets, adapted to our Fabric renderer without Athen. */
object SafariEsp {
    /** Invalidates per-tick contact validation when capture/model eligibility changes. */
    internal var contactRevision=0L
        private set
    private data class Target(val entity: Entity, val species: String, val shulker: Boolean)
    private var targets = emptyList<Target>()
    private var captureTargets = emptyList<Target>()
    private var captureMarkers = emptyList<Entity>()
    private val dropPredictor = FloorDropPredictor()
    private var predictedDrops = emptyList<PredictedFloorDrop>()
    private var predictedIncenseIds = emptySet<Int>()
    private val bannerCollected = mutableSetOf<Int>()
    private val bannerMissing = mutableMapOf<Int,Int>()
    private val coinFloorIds = mutableSetOf<Int>()
    fun resetDrops() {
        dropPredictor.reset()
        predictedDrops = emptyList()
        predictedIncenseIds = emptySet()
        coinFloorIds.clear()
        bannerCollected.clear()
        bannerMissing.clear()
    }
    /** Presentation-only proof. Remote unloads never count as completed floor drops. */
    fun floorBannerReady(biome: SafariBiome): Boolean {
        val client=Minecraft.getInstance();val player=client.player ?: return false
        if(!scanned || client.level!==level) return false
        val known=predictedDrops.filter { it.biome==biome }
        if(known.isEmpty()) return false
        known.forEach { drop ->
            if(drop.actual!=null) bannerCollected.add(drop.id)
            else if((0..2).any { activeFloorDisplay(client.level?.getEntity(drop.id+it)) }) {
                bannerCollected.remove(drop.id);bannerMissing.remove(drop.id)
            } else if(player.distanceToSqr(drop.x,drop.y,drop.z)<=24*24 &&
                client.level?.hasChunkAt(net.minecraft.core.BlockPos.containing(drop.x,drop.y,drop.z))==true) {
                val samples=(bannerMissing[drop.id] ?: 0)+1;bannerMissing[drop.id]=samples
                if(samples>=2) bannerCollected.add(drop.id)
            }
        }
        return known.all { it.id in bannerCollected }
    }
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
    private val sleepingWumpa = Regex("(?i)^Wumpa\\s+zZz$")
    private val cavernDiagnostics = mutableSetOf<String>()
    private val motion = EspMotion()
    private val litterbugs = LitterbugProjection()
    private val shyworms = ShywormProjection()
    private val scrappyFoods = ScrappyFoods()
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/esp")
    private val lines = WorldRenderCompat.renderType("safari_esp")
    private val fill = WorldRenderCompat.renderType("safari_drops", filled = true)
    private var lastCapsuleInputTick: Int? = null

    fun register() {
        WorldRenderCompat.register(::render)
        AttackEntityCallback.EVENT.register { player, world, _, entity, _ ->
            val client = Minecraft.getInstance()
            if (world === client.level && player === client.player && !player.isSpectator)
                SafariStructures.moundAttack(entity, client)
            InteractionResult.PASS
        }
        UseItemCallback.EVENT.register { player, world, hand ->
            val client = Minecraft.getInstance()
            if (world === client.level && player === client.player)
                rememberCapsuleInput(client, player, player.getItemInHand(hand))
            InteractionResult.PASS
        }
        UseBlockCallback.EVENT.register { player, world, hand, _ ->
            val client = Minecraft.getInstance()
            if (world === client.level && player === client.player)
                rememberCapsuleInput(client, player, player.getItemInHand(hand))
            InteractionResult.PASS
        }
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            val client = Minecraft.getInstance()
            if (world === client.level && player === client.player)
                rememberCapsuleInput(client, player, player.getItemInHand(hand), entity)
            InteractionResult.PASS
        }
    }

    private fun rememberCapsuleInput(client: Minecraft, player: Player, stack: ItemStack, clicked: Entity? = null) {
        if (!SafariAssist.inSafari || player.isSpectator || lastCapsuleInputTick == player.tickCount) return
        val id = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id", "")
        if (id != "CRITTER_CAPSULE" && id != "MASTERFUL_CRITTER_CAPSULE" &&
            stack.hoverName.string !in setOf("Critter Capsule", "Masterful Critter Capsule")) return
        val now = System.currentTimeMillis()
        val direct = clicked ?: (client.hitResult as? net.minecraft.world.phys.EntityHitResult)?.entity
        val target = direct?.let { entity ->
            captureTargets.firstOrNull { it.entity === entity && capsuleCandidate(it, client) }
                ?: captureTargets.singleOrNull { modelLabels.label(it.entity.uuid) == entity.uuid && capsuleCandidate(it, client) }
        }
        if (target != null) {
            captures.throwingAt(now, target.entity.uuid, target.species)
        } else {
            val aim = capsuleRayAim(client, player)
            if (aim.isNotEmpty()) captures.throwingAt(now, aim)
            else {
                rememberAim(client, includeWormParts = true)
                captures.throwing(now)
            }
        }
        lastCapsuleInputTick = player.tickCount
        contactRevision++
    }

    private fun capsuleCandidate(target: Target, client: Minecraft) =
        target.species != "Hideyho" && !(target.species == "Rockmite" && target.entity is Display.ItemDisplay) &&
            live(target, client)

    /** Capsule range can exceed vanilla's entity interaction range. Freeze aim at launch. */
    private fun capsuleRayAim(client: Minecraft, player: Player, species: String? = null): Map<String, UUID> {
        val level = client.level ?: return emptyMap()
        val eye = player.eyePosition
        val end = eye.add(player.lookAngle.scale(24.0))
        fun terrain(to: net.minecraft.world.phys.Vec3) = level.clip(net.minecraft.world.level.ClipContext(eye, to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player))
        val candidates = captureTargets.mapNotNull { target ->
            if (species != null && target.species != species ||
                eye.distanceToSqr(target.entity.position()) > 26.0 * 26 || !capsuleCandidate(target, client)) null
            else EspCapsuleTarget(target.entity.uuid, target.species, bounds(target, 1f), target.entity is Display)
        }
        return SafariEspRules.capsuleAim(eye, terrain(end).location, candidates, confirmedSpecies = species != null) {
            terrain(it).type == net.minecraft.world.phys.HitResult.Type.MISS
        }
    }
    internal fun texture(profile: GameProfile?) = SafariEspRules.textureHash(profile?.properties()?.get("textures")?.firstOrNull()?.value())
    private fun texture(stack: ItemStack) = texture(stack.get(DataComponents.PROFILE)?.partialProfile())
    internal fun describe(entity: Entity): EspEntity = describe(entity, entity.type.toShortString())

    private fun describe(entity: Entity, type: String): EspEntity {
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
        return EspEntity(type, texture, shulker?.name,
            (entity as? TropicalFish)?.let { "${it.pattern.name}/${it.baseColor.name}/${it.patternColor.name}" },
            (entity as? Parrot)?.variant?.name, entity.isInvisible || !modelVisible(entity), entity.passengers.isNotEmpty())
    }
    private fun identify(entity: Entity, type: String = entity.type.toShortString()): EspMob? {
        if (!SafariEspRules.canIdentify(type)) return null
        return SafariEspRules.identify(describe(entity, type))
    }
    fun isRockmiteMound(entity: Entity, client: Minecraft): Boolean {
        if (entity.isRemoved || client.level?.getEntity(entity.id) !== entity || !SafariAssist.inSafari) return false
        return entity is Display.ItemDisplay && SafariEspRules.identify(describe(entity))?.name == "Rockmite" ||
            SafariStructures.isMoundAttack(entity, SafariStructures.moundSite(entity.blockPosition()), client)
    }
    private fun modelVisible(entity: Entity): Boolean {
        if (entity !is Display) return true
        val scale = entity.renderState()?.transformation()?.get(1f)?.scale() ?: return false
        return entity.shouldRenderAtSqrDistance(0.0) && SafariEspRules.modelVisible(scale.x(), scale.y(), scale.z())
    }
    /** Keep per-species aim; server chat names the target after the original model can disappear. */
    private fun rememberAim(client: Minecraft, includeWormParts: Boolean = false) {
        val hit = (client.hitResult as? net.minecraft.world.phys.EntityHitResult)?.entity ?: return
        val target = captureTargets.singleOrNull { it.entity === hit && live(it, client) }
        if (target == null) {
            if (!includeWormParts || hit !is ArmorStand || texture(hit.getItemBySlot(EquipmentSlot.HEAD)) !=
                net.johnceo.sparklingmutuals.safari.shots.preview.contacts.ShywormContactParts.BODY_TEXTURE) return
            val heads = captureTargets.filter { it.species == "Shyworm" && live(it, client) }.map { it.entity }
            val bodies = client.level?.entitiesForRendering()?.filterIsInstance<ArmorStand>()?.filter {
                it.distanceToSqr(hit) <= 14.0 * 14 && texture(it.getItemBySlot(EquipmentSlot.HEAD)) ==
                    net.johnceo.sparklingmutuals.safari.shots.preview.contacts.ShywormContactParts.BODY_TEXTURE
            }.orEmpty()
            val parts = heads.map { ShywormPart(it.uuid,it.x,it.y,it.z,true) } + bodies.map { ShywormPart(it.uuid,it.x,it.y,it.z,false) }
            ShywormProjection.headForPart(hit.uuid, parts)?.let { captures.sighted(it,"Shyworm",System.currentTimeMillis()) }
            return
        }
        if (target.species == "Rockmite" && target.entity is Display.ItemDisplay) return
        captures.sighted(target.entity.uuid, target.species, System.currentTimeMillis())
    }
    fun threw(species: String) {
        val client = Minecraft.getInstance()
        captures.threw(species, System.currentTimeMillis()) {
            client.player?.let { capsuleRayAim(client, it, species)[species] }
        }
        contactRevision++
    }
    fun shywormMessage(text: String, now: Long) {
        val player = Minecraft.getInstance().player ?: return
        shyworms.chat(text, now, player.x, player.z)
    }
    fun escaped(species: String, brokeOut: Boolean) {
        captures.escaped(species, System.currentTimeMillis(), brokeOut)?.let(modelLabels::release)
        contactRevision++
    }
    fun caught(species: String, personal: Boolean = true) {
        contactRevision++
        val now=System.currentTimeMillis()
        val id = (if (personal) captures.caught(species, now) else null) ?: modelLabels.captureCandidate(species,now)
        if (id == null) {
            if (species == "Shyworm") shyworms.unassociatedCapture(now)
            if (species == "Litterbug") litterbugs.unassociatedCapture(now)
            if (personal && targets.any { it.species == species && trackedModel(it) })
                logger.info("Capture of {} had no throw/model association; checking name-tag lifecycle instead.", species)
            return
        }
        retireCapture(species, id)
    }
    private fun retireCapture(species: String, id: UUID) {
        captures.retire(id)
        modelLabels.release(id)
        if (species == "Litterbug") litterbugs.caught(id)
        if (species == "Shyworm") shyworms.caught(id).forEach { captures.retire(it); modelLabels.release(it) }
        logger.info("Retired captured {} model {}.", species, id)
        targets = targets.filterNot { captures.hidden(it.entity.uuid) }
    }
    fun clearCaptured() { cavernDiagnostics.clear(); captures.reset(); lastCapsuleInputTick = null; captureMarkers = emptyList(); captureTargets = emptyList(); modelLabels.reset(); motion.reset(); litterbugs.reset(); shyworms.reset(); scrappyFoods.reset();contactRevision++ }
    fun tick(client: Minecraft) {
        if (level !== client.level) { reset(); level = client.level }
        SafariFloorDrops.state.visit(SafariAssist.biome)
        if (!SafariAssist.inSafari || client.player == null || client.level == null) {
            targets = emptyList(); captureTargets = emptyList(); captureMarkers = emptyList(); stringDisplays = emptyList(); ticks = 0; scanned = false; return
        }
        rememberAim(client)
        if (++ticks < 5) return
        ticks = 0
        contactRevision++
        val strings = mutableListOf<EspDrop>()
        val world = client.level!!
        val floorDisplays = mutableListOf<FloorDisplay>()
        val labels = mutableMapOf<String, MutableList<Entity>>()
        val entitiesByUuid = mutableMapOf<UUID, Entity>()
        val entityIds = mutableListOf<Int>()
        val armadillos = mutableListOf<Pair<UUID, Int>>()
        val scannedTargets = mutableListOf<Target>()
        val scannedCaptureMarkers = mutableListOf<Entity>()
        for (entity in world.entitiesForRendering()) {
            if (entity.isRemoved || world.getEntity(entity.id) !== entity) continue
            entitiesByUuid.putIfAbsent(entity.uuid, entity)
            entityIds.add(entity.id)
            val type = entity.type.toShortString()
            if (type == "armadillo") armadillos.add(entity.uuid to entity.id)
            val floorDisplay = activeFloorDisplay(entity)
            if (floorDisplay && entity is Display.ItemDisplay) {
                floorDisplays.add(FloorDisplay(entity.id, entity.x, entity.y, entity.z,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entity.itemStack.item).toString() +
                        ":" + SafariRules.strip(entity.itemStack.hoverName.string)))
            }
            val label = labelText(entity)?.let(SafariEspRules::labelSpecies)
            if (label != null) labels.getOrPut(label) { mutableListOf() }.add(entity)
            if (entity !is Player && isCaptureMarker(entity)) scannedCaptureMarkers.add(entity)
            if (captures.hidden(entity.uuid)) continue
            if (floorDisplay) {
                val pos = entity.blockPosition()
                strings.add(EspDrop(entity.id, pos.x, pos.y, pos.z))
                continue
            }
            val mob = identify(entity, type) ?: continue
            scannedTargets.add(Target(entity, mob.name, mob.identifiers.any { it.shulker != null }))
        }
        predictedDrops = dropPredictor.scan(floorDisplays)
        // Incense membership follows the scan's predicted types, independently of pickup evidence.
        val knownDrops = dropPredictor.knownDrops
        predictedIncenseIds = SafariFloorDrops.state.incenseDropIds(knownDrops)
        captureMarkers = scannedCaptureMarkers
        val lastCaptureTargets = captureTargets
        targets = scannedTargets
        captureTargets = scannedTargets
        scrappyFoods.observe(armadillos, entityIds)
        val now = System.currentTimeMillis()
        val liveBodyIds = targets.mapTo(hashSetOf()) { it.entity.uuid }
        motion.retain(liveBodyIds)
        targets.forEach { target ->
            val e = target.entity
            motion.observe(e.uuid, target.species, e.x, e.y, e.z, client.player!!.distanceToSqr(e), now)
            if (target.species == "Litterbug") litterbugs.observe(e.uuid, e.x, e.y, e.z, now)
        }
        val pityBodies = mutableListOf<EspPityBody>()
        for (target in targets) {
            if (!SafariEspRules.requiresCaptureLabel(target.species, target.entity.type.toShortString())) continue
            val candidates = mutableListOf<EspModelLabel>()
            val visibleRequired = target.species == "Driftling" || target.species == "Chuckwalla"
            var hasSeparateLabel = false
            var ownLabel: EspModelLabel? = null
            for (label in labels[target.species].orEmpty()) {
                val distance = label.distanceToSqr(target.entity)
                if (!(distance <= 9)) continue
                val separate = label !== target.entity
                if (separate) hasSeparateLabel = true
                val visible = label is Display.TextDisplay || label.isCustomNameVisible
                if (visibleRequired && !visible) continue
                val candidate = EspModelLabel(label.uuid, target.species, distance, visible)
                if (separate) candidates.add(candidate) else ownLabel = candidate
            }
            // A separate label takes precedence even when it fails the visibility requirement.
            if (!hasSeparateLabel && ownLabel != null) candidates.add(ownLabel)
            modelLabels.observe(target.entity.uuid, target.species, candidates, now)
            val labelId = modelLabels.label(target.entity.uuid)
            if (labelId != null) captures.bindLabel(target.entity.uuid, target.species, labelId, liveBodyIds)
            val label = labelId?.let(entitiesByUuid::get)
            val labelText = label?.let(::labelText)
            val named = label != null && (label is Display.TextDisplay || label.isCustomNameVisible) &&
                label.distanceToSqr(target.entity) <= 9.0 && labelText?.let(SafariEspRules::labelSpecies) == target.species
            val inCapsule = SafariEspRules.isCapturingLabel(labelText) || inCaptureAnimation(target.entity, client)
            pityBodies.add(EspPityBody(target.entity.uuid, target.species, target.entity.position(), named, inCapsule))
            modelLabels.captureState(target.entity.uuid, labelText, now)
            modelLabels.observeRange(target.entity.uuid, target.species, client.player!!.distanceToSqr(target.entity), now)
        }
        // A body can shrink out of the scan before the bound name switches to CAPTURING.
        for (old in lastCaptureTargets) {
            if (old.entity.uuid in liveBodyIds || captures.hidden(old.entity.uuid)) continue
            val label = modelLabels.label(old.entity.uuid)?.let(entitiesByUuid::get) ?: continue
            val text = labelText(label)
            if (!SafariEspRules.isCapturingLabel(text)) continue
            pityBodies.add(EspPityBody(old.entity.uuid, old.species, old.entity.position(), named = false, capturing = true))
            modelLabels.captureState(old.entity.uuid, text, now)
        }
        captures.observeBodies(pityBodies, now).forEach { (species, id) -> retireCapture(species, id) }
        SafariTracking.ledger.current?.takeIf { it.fullClearReportReceived(SafariBiome.CAVERN) }?.let { run ->
            targets.filter { it.species in setOf("Driftling", "Chuckwalla") &&
                run.count(it.species) >= net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.minimum(it.species) }
                .forEach { target ->
                    val e = target.entity
                    val active = live(target, client)
                    val moving = motion.recentMovement(e.uuid, now)
                    val key = "${e.uuid}:$active:$moving:${run.count(target.species)}"
                    if (cavernDiagnostics.size < 128 && cavernDiagnostics.add(key)) logger.info(
                        "Cavern reported-clear model species={} uuid={} captured={} active={} recentMovement={} label={} labelVisible={} labelCurrent={} pos={} descriptor={}",
                        target.species, e.uuid, run.count(target.species), active, moving, modelLabels.label(e.uuid),
                        modelLabels.visible(e.uuid), modelLabels.current(e.uuid, now), e.position(), describe(e))
                }
        }
        litterbugs.retain(targets.filter { it.species == "Litterbug" }.map { it.entity.uuid }.toSet(),now)
        val worms = targets.filter { it.species == "Shyworm" }
        shyworms.retain(worms.map { it.entity.uuid }.toSet())
        worms.forEach {
            val e = it.entity
            shyworms.observe(e.uuid, e.x, e.y, e.z,
                e.y > 2 && (modelLabels.confirmed(e.uuid,now) || motion.recentMovement(e.uuid,now) ||
                    modelLabels.label(e.uuid) != null && client.player!!.distanceToSqr(e) > 32.0*32), now)
        }
        shyworms.reconcile(now)
        shyworms.reconcileCapture(now, worms.filter { live(it, client) }.map { it.entity.uuid }.toSet())
        pruneDrawableTargets(client)
        SafariTracking.ledger.current?.let { run ->
            run.localPlayer(client.player!!.name.string)
            val coinDrops = knownDrops.filter { it.biome == SafariBiome.HAUNTED &&
                (it.actual?.type ?: it.type) == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN }
            // A later anchor or a confirmed pickup can change an earlier prediction.
            val reclassified = coinFloorIds - coinDrops.mapTo(hashSetOf()) { it.id }
            coinFloorIds.removeAll(reclassified)
            run.gimmiegoldCheck.discardFloors(reclassified)
            coinDrops.forEach { drop ->
                val active = (0..2).count { activeFloorDisplay(world.getEntity(drop.id + it)) }
                // Only real STRING reward triples establish slots, not decorative/model triples.
                if (active == 3 || drop.actual?.type == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN)
                    coinFloorIds.add(drop.id)
                if (drop.id in coinFloorIds) {
                    val observable = client.level!!.hasChunkAt(net.minecraft.core.BlockPos.containing(drop.x, drop.y, drop.z)) &&
                        client.player!!.distanceToSqr(drop.x, drop.y, drop.z) <= 80.0 * 80
                    run.gimmiegoldCheck.floor(drop.id,
                        if (active > 0) true else if (observable) false else null,
                        collected = drop.actual?.type == net.johnceo.sparklingmutuals.safari.resources.FloorDropType.COIN)
                }
            }
        }
        stringDisplays = strings
        scanned = true
        rememberAim(client)
    }
    private fun trackedModel(target: Target) = SafariEspRules.requiresModelLabel(target.species, target.entity.type.toShortString())
    /** Rendering may retain visual proof; remaining-capture observations always use strict live(). */
    private fun drawable(target: Target, client: Minecraft) = live(target, client, forEsp = true)
    private fun pruneDrawableTargets(client: Minecraft) {
        // Keep list identity when every target survives so outline lookup can reuse its index.
        var remaining: MutableList<Target>? = null
        for ((index, target) in targets.withIndex()) {
            if (drawable(target, client)) remaining?.add(target)
            else if (remaining == null) remaining = ArrayList<Target>(targets.size - 1).apply {
                addAll(targets.subList(0, index))
            }
        }
        if (remaining != null) targets = remaining
    }
    /** Read-only model/capture validity, independent of ESP mode and visibility switches. */
    internal fun contactCurrent(entity: Entity, species: String, client: Minecraft): Boolean =
        entity !is Player && live(Target(entity,species,false),client)
    private fun live(target: Target, client: Minecraft, forEsp: Boolean = false): Boolean {
        val entity = target.entity
        if (target.species == "Rockmite" && entity is Display.ItemDisplay) {
            val site = SafariStructures.moundSite(entity.blockPosition())
            if (SafariTracking.ledger.current?.moundSurvey?.confirmedBroken(site) == true) return false
        }
        // The quest NPC can stay loaded or change UUID after its successful reward.
        if (target.species == "Hideyho" && (SafariTracking.ledger.current?.count("Hideyho") ?: 0) > 0) return false
        if (SafariTracking.ledger.current?.sparklingCaptures?.resolved(entity.uuid) == true) return false
        if (entity.isRemoved || captures.hidden(entity.uuid) || client.level?.getEntity(entity.id) !== entity ||
            entity.level() !== client.level || identify(entity)?.name != target.species) return false
        val now = System.currentTimeMillis()
        if (!forEsp && SafariEspRules.requiresCaptureLabel(target.species, entity.type.toShortString()) &&
            !modelLabels.confirmed(entity.uuid, now)) return false
        val moving = motion.current(entity.uuid, now)
        val visualLabel = if (forEsp) modelLabels.visualCurrent(entity.uuid, target.species,
            client.player!!.distanceToSqr(entity), now, when(target.species) {
                "Shyworm" -> shyworms.hiddenHeight(entity.uuid, entity.y)
                "Litterbug" -> litterbugs.hiddenHeight(entity.uuid,entity.x,entity.y,entity.z)
                else -> null
            }) else false
        val labeled = !trackedModel(target) || if (forEsp) visualLabel else modelLabels.current(entity.uuid, now)
        if (forEsp && (target.species == "Shyworm" || target.species == "Litterbug") && !labeled) return false
        if (!forEsp && target.species == "Shyworm" && !SafariEspRules.shywormCurrent(
                modelLabels.label(entity.uuid) != null, labeled, entity.y)) return false
        if (!forEsp && target.species == "Flitter" && !SafariEspRules.flitterCurrent(
                modelLabels.label(entity.uuid) != null, labeled, modelLabels.visible(entity.uuid),
                motion.recentMovement(entity.uuid, now))) return false
        if (!forEsp && target.species == "Driftling" && !SafariEspRules.driftlingCurrent(
                modelLabels.label(entity.uuid) != null, labeled, modelLabels.visible(entity.uuid),
                motion.recentMovement(entity.uuid, now), motion.movementObserved(entity.uuid))) return false
        if (forEsp && (target.species == "Flitter" || target.species == "Driftling") &&
            !modelLabels.confirmed(entity.uuid,now) && !motion.recentMovement(entity.uuid,now) &&
            !(client.player!!.distanceToSqr(entity) > 32.0*32 && modelLabels.label(entity.uuid) != null && visualLabel)) return false
        if (target.species == "Chuckwalla" && !(if (forEsp) visualLabel else SafariEspRules.chuckwallaCurrent(
                modelLabels.label(entity.uuid) != null, labeled, modelLabels.visible(entity.uuid)))) return false
        return if (forEsp) SafariEspRules.renderCurrent(target.species, ConfigManager.sparklingMode, true, moving, labeled,
            if (ConfigManager.sparklingMode) ConfigManager.sparklingProfitableShardEsp
            else !ConfigManager.fullClearMode && ConfigManager.profitableShardEsp)
            else labeled
    }
    internal fun developerSnapshot(client: Minecraft): Map<String, Any?> {
        val entities = client.level?.entitiesForRendering()?.filterNot { it.isRemoved }?.toList().orEmpty()
        val incenseIds = predictedIncenseIds
        val playerBiome = SafariAssist.biome
        val targetsById = targets.associateBy { it.entity.id }
        return mapOf(
            "loadedEntityCount" to entities.size,
            "loadedEntityTypes" to entities.groupingBy { it.type.toShortString() }.eachCount(),
            "entities" to entities.mapNotNull { entity ->
                val descriptor = describe(entity)
                val species = SafariEspRules.identify(descriptor)
                val target = targetsById[entity.id]
                val label = entity.customName?.string?.let(SafariEspRules::labelSpecies)
                if (species == null && target == null && label == null &&
                    entity !is Display && descriptor.type != "interaction") return@mapNotNull null
                mapOf(
                    "id" to entity.id, "uuid" to entity.uuid, "descriptor" to descriptor,
                    "species" to (target?.species ?: species?.name), "labelSpecies" to label,
                    "position" to listOf(entity.x, entity.y, entity.z),
                    "name" to entity.customName?.string, "labelVisible" to entity.isCustomNameVisible,
                    "box" to entity.boundingBox.let { listOf(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ) },
                    "modelLabel" to modelLabels.label(entity.uuid), "retired" to captures.hidden(entity.uuid),
                    "liveForTracking" to target?.let { live(it, client) },
                    "liveForEsp" to target?.let { live(it, client, forEsp = true) },
                    "espDecision" to target?.let {
                        if (!drawable(it, client)) "Model no longer drawable"
                        else critterEspDecision(it, client, playerBiome, System.currentTimeMillis())
                    },
                    "floorMarker" to activeFloorDisplay(entity),
                    "displayItem" to (entity as? Display.ItemDisplay)?.itemStack?.let {
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.item).toString()
                    }
                )
            },
            "observations" to observations(),
            "knownFloorDrops" to dropPredictor.knownDrops,
            "floorEspCandidates" to predictedDrops.map {
                mapOf("id" to it.id, "selected" to SafariFloorDrops.selected(it, incenseIds),
                    "activeTriple" to (0..2).all { offset -> activeFloorDisplay(client.level?.getEntity(it.id + offset)) },
                    "renderEligible" to floorEspEligible(it, client, playerBiome, incenseIds))
            },
            "retainedShyworms" to shyworms.missingMarkers(),
            "retainedLitterbugs" to litterbugs.missingMarkers(System.currentTimeMillis()),
            "sparklingDetections" to SafariAssist.diagnostics(),
            "hideCritterEsp" to ConfigManager.hideCritterEsp(),
            "hideFloorDropEsp" to ConfigManager.hideFloorDropEsp()
        )
    }

    fun debug(client: Minecraft) {
        logger.info("Recent capsule attempts: {}", captures.recentAttempts())
        SafariTracking.ledger.current?.moundSurvey?.let { logger.info("Mound hit progress: {}", it.hitDiagnostics(System.currentTimeMillis())) }
        logger.info("Shared party sync: {}",net.johnceo.sparklingmutuals.sync.SafariPartySync.diagnostics())
        val player = client.player ?: return
        val entities = client.level?.entitiesForRendering()?.filter { !it.isRemoved && player.distanceToSqr(it) <= 6400 }?.toList() ?: return
        ShywormBodyDiagnostics.capture(client,entities).forEach { logger.info("SM_SHYWORM_BODY {}",it) }
        SafariAssist.diagnostics().forEach { logger.info("{}", it) }
        logger.info("Safari ESP diagnostics: biome={}, fullClear={}, collected={}", SafariAssist.biome,
            ConfigManager.fullClearMode, SafariRoster.all.associate { it.name to (SafariTracking.run?.count(it.name) ?: 0) })
        SafariAssist.biome?.let { biome -> SafariTracking.run?.captureDebug(biome)?.forEach { (name, evidence) ->
            logger.info("Full clear evidence: {} {}", name, evidence)
        } }
        SafariTracking.run?.let { run ->
            val mode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
            val party = SafariSparklingMode.state
            SafariBiome.entries.forEach { biome ->
                logger.info("Biome gate: biome={} mode={} current={} complete={} local={} evidence={} startingItems={} pendingSparkling={} checksLeft={}",
                    biome, mode, run.lastBiome, run.biomeComplete(mode, biome, party),
                    run.completion.locallyComplete(run, biome, mode, party), run.hasCaptureEvidence(biome),
                    run.hasUnusedStartingForBiome(biome), run.sparklingCaptures.pending(biome),
                    biome.critters.filter { party.needs(it.name) && !run.sparklingComplete(it.name, party, localOnly = true) }.map { it.name })
            }
        }
        logger.info("Party discovery cache: {}", SafariSparklingMode.state.debug { id ->
            client.connection?.onlinePlayers?.firstOrNull { it.profile.id.toString() == id }?.profile?.name
                ?: net.johnceo.sparklingmutuals.safari.automation.SafariPartyInput.nameOf(id) ?: id
        })
        SafariTracking.run?.let { logger.info("Gimmiegold completion: {} coinDropMarkers={}", it.sparklingChecks.gimmiegoldDebug(), observations().hauntedCoinDropsRemaining) }
        SafariTracking.run?.let { logger.info("Gimmiegold Sparkling check: {}", it.gimmiegoldCheck.debug()) }
        SafariTracking.run?.let { logger.info("Forest birds: {} canReportFd={}", it.birds.debug(), it.canReportBiome(SafariBiome.FOREST)) }
        SafariTracking.run?.let { logger.info("Forest report: {}", it.forestReportDebug(
            net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode, SafariSparklingMode.state)) }
        val incenseIds = predictedIncenseIds
        val run = SafariTracking.ledger.current
        logger.info("Gem placement evidence: {}", run?.gems?.debug())
        logger.info("Gem podium blocks: {}", run?.gems?.podiumDebug(client))
        logger.info("Gem ESP gates: mode={} playerBiome={} hiddenByMode={} cavernEnabled={} partyNeedsGemzie={} captured={} placed={} chamberOpened={} completed={} inventory={}",
            net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode, SafariAssist.biome, ConfigManager.hideFloorDropEsp(),
            SafariFloorDrops.enabled(SafariBiome.CAVERN), SafariSparklingMode.state.needs("Gemzie"), run?.count("Gemzie"),
            run?.gems?.placed, run?.gems?.opened, run?.completed(net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode,
                listOf(SafariRoster.named("Gemzie")!!), SafariSparklingMode.state), SafariFloorDrops.state.gemDebug())
        predictedDrops.forEach { drop -> logger.info("Floor prediction: id={} biome={} pos={},{},{} predicted={} actual={} selected={} completeLiveTriple={} onlyInBiome={}",
            drop.id, drop.biome, drop.x, drop.y, drop.z, drop.type?.label, drop.actual, SafariFloorDrops.selected(drop, incenseIds),
            (0..2).all { activeFloorDisplay(client.level?.getEntity(drop.id + it)) }, SafariEspConfig.dropOnlyInBiome(drop.type)) }
        entities.filterIsInstance<Display.ItemDisplay>().filter { SafariEspRules.biomeAt(it.x,it.z)==SafariBiome.CAVERN }
            .take(128).forEach { logger.info("Cavern floor candidate: id={} pos={} invisible={} item={} name={} activeMarker={}",
                it.id,it.position(),it.isInvisible,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.itemStack.item),
                it.itemStack.hoverName.string,activeFloorDisplay(it)) }
        entities.mapNotNull { entity -> SafariEspRules.identify(describe(entity))?.let { entity to it.name } }.forEach { (entity, species) ->
            val tag = modelLabels.label(entity.uuid)
            val label = entities.firstOrNull { it.uuid == tag }
            if (species in setOf("Driftling", "Chuckwalla")) logger.info("Cavern lifecycle species={} uuid={} movementObserved={} recentMovement={} labelVisible={} active={}",
                species, entity.uuid, motion.movementObserved(entity.uuid), motion.recentMovement(entity.uuid, System.currentTimeMillis()),
                modelLabels.visible(entity.uuid), contactCurrent(entity, species, client))
            logger.info("ESP model species={} id={} uuid={} type={} pos={} retired={} pity={} label={} labelName={} labelVisible={} labelDistance={} labelCurrent={} descriptor={} vehicle={} passengers={} modelName={} nearbyLabels={}",
                species, entity.id, entity.uuid, entity.type.toShortString(), entity.position(), captures.hidden(entity.uuid), captures.pityFor(entity.uuid), tag,
                label?.customName?.string, label?.isCustomNameVisible, label?.distanceToSqr(entity),
                modelLabels.current(entity.uuid, System.currentTimeMillis()), describe(entity), entity.vehicle?.id, entity.passengers.map { it.id }, entity.customName?.string,
                entities.filter { it.customName != null && it.distanceToSqr(entity) <= 9 }.map { "${it.id}:${it.customName?.string}:${it.isCustomNameVisible}" })
        }
    }
    /** Loaded server entities only; observations do not depend on ESP switches or biome filtering. */
    internal fun renderedMoundSites(client: Minecraft): Set<Triple<Int, Int, Int>> {
        if (level !== client.level || client.player == null) return emptySet()
        return targets.asSequence().filter { it.species == "Rockmite" && it.entity is Display.ItemDisplay && drawable(it, client) }
            .map { SafariStructures.moundSite(it.entity.blockPosition()) }
            .toSet()
    }

    internal fun hideyhoPosition(): net.minecraft.world.phys.Vec3? {
        val client = Minecraft.getInstance()
        if (!SafariAssist.visualsActive(client) || level !== client.level || client.player == null) return null
        return targets.firstOrNull { it.species == "Hideyho" && drawable(it, client) }?.entity?.position()
    }
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
            HauntedCoinDrops.remaining(rawDrops, completeGroups), ForestBirdFoodDrops.remaining(rawDrops, completeGroups),
            ForestBirdFoodDrops.remainingByType(rawDrops,completeGroups))
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
    private fun floorEspEligible(drop: net.johnceo.sparklingmutuals.safari.resources.PredictedFloorDrop,
        client: Minecraft, biome: SafariBiome?, incenseIds: Set<Int>): Boolean =
        !ConfigManager.hideFloorDropEsp() && drop.actual == null &&
            (0..2).all { client.level?.getEntity(drop.id + it)?.isRemoved == false } &&
            SafariEspRules.visible(true, SafariFloorDrops.selected(drop, incenseIds), SafariEspConfig.dropOnlyInBiome(drop.type), biome, drop.biome)

    private fun critterEspDecision(target: Target, client: Minecraft, playerBiome: SafariBiome?, now: Long): String {
        if (!SafariAssist.inSafari || client.options.smHideGui) return "HUD hidden or outside Safari"
        if (ConfigManager.hideCritterEsp()) return "Critter ESP hidden by mode"
        val entity = target.entity
        val mob = SafariRoster.named(target.species) ?: return "Unknown species"
        val group = SafariEspConfig.groups.getValue(mob.biome.name.lowercase())
        val actualBiome = SafariEspRules.biomeAt(entity.x, entity.z)
        val display = entity is Display.ItemDisplay
        val run = SafariTracking.ledger.current
        val party = SafariSparklingMode.state
        val mode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
        val forceIcy = mob.biome == SafariBiome.ICY && BiomeObjectiveConfig.needs("Wumpa", party) &&
            (run?.count("Wumpa") ?: 0) == 0 && (run?.count(mob.name) ?: 0) == 0
        if (!BiomeObjectiveConfig.allowsEsp(mob.name, mode, run) && !(mode == SafariMode.SPARKLING && forceIcy))
            return "Optional sparkling check disabled"
        if (!forceIcy && !live(target, client, forEsp = true)) return "Inactive model or label"
        val alwaysOn = SafariEspConfig.alwaysOn(mob.name)
        val needed = alwaysOn || forceIcy || if (ConfigManager.sparklingMode)
            SafariEspRules.neededForSparkling(mob.name, mob.name == "Rockmite" && display,
                party, ConfigManager.sparklingProfitableShardEsp, run, entity.uuid, now)
        else SafariEspRules.neededForRun(mob.name, display, run, ConfigManager.fullClearMode, ConfigManager.profitableShardEsp, party)
        if (!needed) return "Species not needed for current mode"
        if (!SafariEspConfig.entityEnabled(mob.name, display)) return "Species ESP disabled"
        if (actualBiome != mob.biome) return "Model outside species biome"
        if (!SafariEspRules.visible(true, group.enabled, group.onlyInBiome && !alwaysOn, playerBiome, actualBiome)) return "Biome visibility restricted"
        return "Eligible"
    }

    private var indexedTargets: List<Target>?=null
    private var targetIndex=emptyMap<Int,Target>()
    private fun indexedTarget(entity: Entity): Target? {
        if (indexedTargets !== targets) {
            indexedTargets = targets
            targetIndex = targets.associateBy { it.entity.id }
        }
        return targetIndex[entity.id]?.takeIf { it.entity === entity }
    }
    private fun targetColor(target: Target): Int {
        val mound=target.species=="Rockmite" && target.entity is Display.ItemDisplay
        val food=if(target.species=="Scrappy" && !SafariEspConfig.usesBiomeColor("Scrappy")) scrappyFoods.food(target.entity.uuid) else null
        return SafariEspConfig.rgb(food?.let {SafariEspConfig.scrappyFoodColors.getValue(it)} ?: SafariEspConfig.entityColor(target.species,mound))
    }
    fun outlineColor(entity: Entity): Int? {
        val client=Minecraft.getInstance()
        if(!SafariEspConfig.critterGlow || !SafariAssist.visualsActive(client) || client.player==null || client.options.smHideGui)return null
        val target = indexedTarget(entity) ?: return null
        if(target.species=="Rockmite" && entity is Display.ItemDisplay)return null
        if (target.species == "Shyworm" && (entity.y <= 2.0 || shyworms.hiddenHeight(entity.uuid, entity.y) != null)) return null
        if(!drawable(target,client) || critterEspDecision(target,client,SafariAssist.biome,System.currentTimeMillis())!="Eligible")return null
        return targetColor(target)
    }
    private fun nameplateShown(target: Target,client: Minecraft): Boolean {
        return CritterNameplates.settings.enabled && CritterNameplates.enabledFor(target.species) &&
            client.player!=null && !client.options.smHideGui && SafariAssist.visualsActive(client) &&
            !ConfigManager.hideCritterEsp() && drawable(target,client) &&
            !(target.species=="Rockmite" && target.entity is Display.ItemDisplay) &&
            critterEspDecision(target,client,SafariAssist.biome,System.currentTimeMillis())=="Eligible"
    }
    private fun labelText(entity: Entity): String? =
        if (entity is Display.TextDisplay) entity.text.string else entity.customName?.string

    private fun isCaptureMarker(entity: Entity) = SafariEspRules.isCapturingLabel(labelText(entity))

    private fun inCaptureAnimation(entity: Entity, client: Minecraft): Boolean {
        val level = client.level ?: return false
        if (isCaptureMarker(entity)) return true
        val vehicleItem = entity.vehicle?.let(CritterCapsuleHider::itemId)
        if (vehicleItem == "CRITTER_CAPSULE" || vehicleItem == "MASTERFUL_CRITTER_CAPSULE") return true
        return captureMarkers.any { marker ->
            !marker.isRemoved && level.getEntity(marker.id) === marker &&
                SafariEspRules.nearCaptureMarker(marker.x - entity.x, marker.y - entity.y, marker.z - entity.z) &&
                isCaptureMarker(marker)
        }
    }

    fun hideVanillaNameTag(entity: Entity): Boolean {
        if(CritterNameplates.showsVanilla())return false
        if (targets.isEmpty()) return false
        val client = Minecraft.getInstance()
        if (client.player == null || client.options.smHideGui || !SafariAssist.visualsActive(client) ||
            ConfigManager.hideCritterEsp()) return false
        val labelCarrier = entity is ArmorStand || entity is Display.TextDisplay
        val raw = if (labelCarrier) entity.customName?.string?.let(SafariRules::strip) else null
        val species=raw?.let(SafariEspRules::labelSpecies) ?: if(raw?.matches(sleepingWumpa)==true) "Wumpa" else null
        return targets.any { target ->
                (target.entity===entity || modelLabels.label(target.entity.uuid)==entity.uuid ||
                    labelCarrier && species==target.species && target.entity.distanceToSqr(entity)<=9.0) &&
                nameplateShown(target,client)
        }
    }

    fun sparklingDuplicoColor(entity: Entity): Int? {
        if (entity.type != EntityType.INTERACTION) return null
        val client = Minecraft.getInstance()
        if (!SafariAssist.visualsActive(client) || client.options.smHideGui) return null
        if (indexedTarget(entity)?.species != "Duplico") return null
        val sparkling = SafariAssist.sparklingDuplicoBodies()
        if (sparkling.isEmpty()) return null
        val blocks = targets.filter { it.species == "Duplico" && drawable(it,client) }
        if (blocks.none { it.entity === entity }) return null
        val matched = sparkling.any { body ->
            var nearest: Entity? = null
            var nearestDistance = 9.0
            for (block in blocks) {
                val distance = block.entity.distanceToSqr(body)
                if (distance <= 9.0 && (nearest == null || distance < nearestDistance)) {
                    nearest = block.entity
                    nearestDistance = distance
                }
            }
            nearest === entity
        }
        return if (matched) SafariEspConfig.rgb(net.johnceo.sparklingmutuals.config.AppearanceConfig.sparklingColor) else null
    }

    private fun render(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (!SafariAssist.visualsActive(client) || client.player == null || client.options.smHideGui) return
        val camera = client.gameRenderer.smCamera.position()
        val playerBiome = SafariAssist.biome
        fun frame(box: AABB, color: Int) = WorldRenderCompat.box(context, lines, box, color)
        val delta = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val now = System.currentTimeMillis()
        val run = SafariTracking.ledger.current
        val timers = mutableListOf<Triple<String, Int, AABB>>()
        pruneDrawableTargets(client)
        if (!ConfigManager.hideCritterEsp()) {
        for (target in targets) {
            val e = target.entity
            val mob = SafariRoster.named(target.species)!!
            if (critterEspDecision(target, client, playerBiome, now) == "Eligible") {
                val box = bounds(target, delta)
                val height = when (mob.name) {
                    "Litterbug" -> litterbugs.hiddenHeight(e.uuid, e.x, e.y, e.z)
                    "Shyworm" -> shyworms.hiddenHeight(e.uuid, e.y)
                    else -> null
                }
                val projected = when {
                    height == null -> box
                    mob.name == "Shyworm" -> ShywormProjection.projectHead(box, e.getPosition(delta), height)
                    else -> box.move(0.0, height - e.getPosition(delta).y, 0.0)
                }
                val food = if (mob.name == "Scrappy" && e is Armadillo && e.isScared) scrappyFoods.food(e.uuid) else null
                val color = targetColor(target)
                val mound = mob.name == "Rockmite" && e is Display.ItemDisplay
                val showPity = CritterNameplates.settings.pity && !inCaptureAnimation(e, client)
                if (mound && showPity) {
                    val key = SafariStructures.moundSite(e.blockPosition())
                    run?.moundSurvey?.hitLabel(key)?.let { label ->
                        WorldRenderCompat.markerLabel(context, label, color, projected.center.x,
                            projected.maxY + .4, projected.center.z, false)
                    }
                } else if (!mound && CritterNameplates.showsMarker() && CritterNameplates.enabledFor(mob.name)) {
                    val title = if (mob.name == "Hideyho") {
                        if (run == null) "Hideyho (START)" else run.hideyhoMarker.title(e.blockPosition())
                    } else food?.label ?: mob.name
                    if (title != null) {
                        val distance = client.player!!.position().distanceTo(projected.center)
                        val sparkling = SafariAssist.isHighlighted(e.id) || sparklingDuplicoColor(e) != null
                        val label = CritterNameplates.label(mob.name, captures.pityFor(e.uuid), distance, sparkling,
                            title = title, showPity = food == null && showPity)
                        val matchEsp = CritterNameplates.matchesEspColor(mob.name)
                        val espColor = if (sparkling) SafariEspConfig.rgb(AppearanceConfig.sparklingColor) else color
                        val nameColor = CritterNameplates.color(mob.name, (run?.captureProgress(mob.name) ?: 0) > 0, espColor)
                        WorldRenderCompat.markerLabel(context, label, nameColor, projected.center.x,
                            projected.maxY + .4, projected.center.z, sparkling && !matchEsp)
                    }
                }
                val retainedWormHead = mob.name == "Shyworm" && height != null
                if (SafariEspConfig.showCritterBox(mob.name) || mound ||
                    retainedWormHead && ShywormProjection.showRetainedHead(SafariEspConfig.critterBoxes, SafariEspConfig.critterGlow))
                    frame(projected, if (retainedWormHead && SafariAssist.isHighlighted(e.id))
                        SafariEspConfig.rgb(AppearanceConfig.sparklingColor) else color)
                if (mob.name == "Scrappy" && food != null &&
                    (!CritterNameplates.settings.enabled || !CritterNameplates.enabledFor(mob.name))) WorldRenderCompat.label(context, food.label, color,
                    projected.center.x, projected.maxY + .2, projected.center.z, SafariEspConfig.scrappyLabelWorldScale)
                if (mob.name == "Litterbug" && height != null) litterbugs.hiddenMarker(e.uuid, e.x, e.y, e.z, now)?.let {
                    timers.add(Triple(it.text, it.color, projected))
                }
                if (mob.name == "Shyworm" && !ConfigManager.sparklingMode) {
                    shyworms.timer(e.uuid, now)?.let { timers.add(Triple(it.text, it.color, projected)) }
                }
            }
        }
        val litterbugMob=SafariRoster.named("Litterbug")!!
        val litterbugGroup=SafariEspConfig.groups.getValue("haunted")
        val retainedBugNeeded=if(ConfigManager.sparklingMode)
            SafariEspRules.neededForSparkling("Litterbug",false,SafariSparklingMode.state,ConfigManager.sparklingProfitableShardEsp,SafariTracking.ledger.current)
        else SafariEspRules.neededForRun("Litterbug",false,SafariTracking.ledger.current,ConfigManager.fullClearMode,ConfigManager.profitableShardEsp,SafariSparklingMode.state)
        if((retainedBugNeeded || SafariEspConfig.alwaysOn("Litterbug")) && SafariTracking.ledger.current?.captureComplete("Litterbug") != true &&
            SafariEspConfig.entityEnabled("Litterbug",false) &&
            SafariEspRules.visible(true,litterbugGroup.enabled,litterbugGroup.onlyInBiome && !SafariEspConfig.alwaysOn("Litterbug"),playerBiome,litterbugMob.biome)) {
            for(location in litterbugs.missingMarkers(now)) {
                if(captures.hidden(location.id)) continue
                val box=AABB(location.x-.4,location.marker.height,location.z-.4,location.x+.4,location.marker.height+.8,location.z+.4)
                if(SafariEspConfig.critterBoxes) frame(box,SafariEspConfig.rgb(SafariEspConfig.entityColor("Litterbug",false)))
                timers.add(Triple(location.marker.text,location.marker.color,box))
            }
        }
        val wormMob = SafariRoster.named("Shyworm")!!
        val wormGroup = SafariEspConfig.groups.getValue(wormMob.biome.name.lowercase())
        val retainedWormNeeded = if (ConfigManager.sparklingMode)
            SafariEspRules.neededForSparkling("Shyworm", false, SafariSparklingMode.state,
                ConfigManager.sparklingProfitableShardEsp, SafariTracking.ledger.current)
        else SafariEspRules.neededForRun("Shyworm", false, SafariTracking.ledger.current,
            ConfigManager.fullClearMode, ConfigManager.profitableShardEsp, SafariSparklingMode.state)
        if ((retainedWormNeeded || SafariEspConfig.alwaysOn("Shyworm")) && SafariTracking.ledger.current?.captureComplete("Shyworm") != true &&
            SafariEspConfig.entityEnabled("Shyworm", false) &&
            SafariEspRules.visible(true, wormGroup.enabled, wormGroup.onlyInBiome && !SafariEspConfig.alwaysOn("Shyworm"), playerBiome, wormMob.biome)) {
            for (marker in shyworms.missingMarkers()) {
                if (captures.hidden(marker.id)) continue
                val box = AABB(marker.x - .3, marker.y + 1.35, marker.z - .3,
                    marker.x + .3, marker.y + 1.95, marker.z + .3)
                val color = SafariEspConfig.rgb(SafariEspConfig.entityColor("Shyworm", false))
                if (ShywormProjection.showRetainedHead(SafariEspConfig.critterBoxes, SafariEspConfig.critterGlow)) frame(box, color)
                timers.add(Triple("Shyworm (last seen)", color, box))
            }
        }
        }
        val selectedDrops = predictedDrops.filter { floorEspEligible(it, client, playerBiome, predictedIncenseIds) }
        val tiles = selectedDrops.map { it.marker() to SafariEspConfig.rgb(SafariEspConfig.dropColor(it.type!!)) }
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
        selectedDrops.forEach { drop ->
            val type = drop.type!!
            WorldRenderCompat.label(context, type.label, SafariEspConfig.rgb(SafariEspConfig.dropColor(type)),
                drop.x, drop.y + 1.2, drop.z, SafariEspConfig.dropLabelWorldScale(type))
        }
    }
    fun reset() { resetDrops(); targets = emptyList(); level = null; ticks = 0; stringDisplays = emptyList(); scanned = false; clearCaptured() }
}
