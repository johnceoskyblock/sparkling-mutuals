package net.johnceo.sparklingmutuals.safari.structures

//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes as EntityType
*///? } else {
import net.minecraft.world.entity.EntityType
//? }

import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.esp.EspEntity
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.sync.PartySyncSites
import net.johnceo.sparklingmutuals.safari.resources.SafariWaypointSites
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity

/** Loaded-world observations adapted from CritterMod 0.9.0; never request chunks. */
object SafariStructures {
    val snooperPositions = listOf(BlockPos(-126, 39, 74), BlockPos(-114, 39, 87), BlockPos(-70, 39, 68),
        BlockPos(-96, 40, 17), BlockPos(-95, 40, 42))
    private var states = snooperPositions.associateWith { WallState.UNKNOWN }
    private var sharedWalls=0
    val localWallStates get() = states.values.toList()
    fun shareWalls(mask: Int) { sharedWalls=sharedWalls or mask }
    val walls get() = WallSummary(snooperPositions.mapIndexed { i,pos -> if(sharedWalls and (1 shl i)!=0) WallState.BROKEN else states.getValue(pos) })
    val intactWalls get() = snooperPositions.filterIndexed { i,pos -> sharedWalls and (1 shl i)==0 && states[pos]==WallState.INTACT }.toSet()
    var nearbyMounds = 0
        private set
    private var ticks = 0
    private val absentSince = mutableMapOf<Triple<Int, Int, Int>, Long>()
    private val scaffolding = setOf(EntityType.ARMOR_STAND, EntityType.ITEM_DISPLAY, EntityType.BLOCK_DISPLAY,
        EntityType.TEXT_DISPLAY, EntityType.PLAYER, EntityType.ITEM, EntityType.INTERACTION)
    internal fun isMoundAttack(entity: Entity, site: Triple<Int, Int, Int>, client: Minecraft): Boolean =
        entity.type == EntityType.INTERACTION &&
            (SafariTracking.ledger.current?.moundSurvey?.seenUnopened(site) == true ||
                (catalogMound(entity, client) || isMound(entity, client)))

    private fun catalogMound(entity: Entity, client: Minecraft): Boolean {
        val pos = entity.blockPosition()
        val site = PartySyncSites.moundAt(Triple(pos.x, pos.y, pos.z)) ?: return false
        val level = client.level ?: return false
        fun dry(at: BlockPos) = level.getFluidState(at).isEmpty && level.getFluidState(at.below()).isEmpty
        return entity.y <= 65 && kotlin.math.abs(entity.x - (pos.x + .5)) < .05 &&
            kotlin.math.abs(entity.z - (pos.z + .5)) < .05 && dry(pos) &&
            dry(BlockPos(site.first, site.second, site.third))
    }
    fun isMound(entity: Entity, client: Minecraft): Boolean {
        val player = client.player ?: return false
        val level = client.level ?: return false
        val occupied = level.entitiesForRendering().any {
            !it.isRemoved && it.type !in scaffolding && kotlin.math.abs(it.x - entity.x) <= .35 &&
                kotlin.math.abs(it.z - entity.z) <= .35 && kotlin.math.abs(it.y - entity.y) <= 1.0
        }
        return entity.type == EntityType.INTERACTION && SafariMounds.detected(MoundShape(entity.x, entity.y, entity.z,
            entity.boundingBox.xsize, entity.boundingBox.ysize, player.distanceToSqr(entity), occupied))
    }
    fun tick(client: Minecraft) {
        if (!SafariAssist.inSafari) { nearbyMounds = 0; ticks = 0; return }
        val level = client.level ?: return
        val player = client.player ?: return
        if (++ticks < 10) return
        ticks = 0
        states = snooperPositions.associateWith {
            when { !level.isLoaded(it) -> states.getValue(it)
                level.getBlockState(it).isAir -> WallState.BROKEN; else -> WallState.INTACT }
        }
        val run = SafariTracking.ledger.current ?: return
        level.entitiesForRendering().filterNot { it.isRemoved }.forEach {
            if (SafariEspRules.biomeAt(it.x, it.z) == SafariBiome.CAVERN)
                run.observe(EspEntity(it.type.toShortString(), invisible = it.isInvisible, passengers = it.passengers.isNotEmpty()))
        }
        val entities = level.entitiesForRendering().filter { !it.isRemoved && level.getEntity(it.id) === it }.toList()
        val creatures = entities.filter { it.type !in scaffolding }
        val interactions = entities.filter { it.type == EntityType.INTERACTION &&
            SafariEspRules.biomeAt(it.x, it.z) == SafariBiome.CAVERN }
        fun site(entity: Entity): Triple<Int, Int, Int> {
            val pos = entity.blockPosition()
            val observed = Triple(pos.x, pos.y, pos.z)
            return PartySyncSites.moundAt(observed) ?: observed
        }
        val mounds = interactions.filter { box ->
            val pos = box.blockPosition()
            val occupied = creatures.any { kotlin.math.abs(it.x - box.x) <= .35 &&
                kotlin.math.abs(it.z - box.z) <= .35 && kotlin.math.abs(it.y - box.y) <= 1.0 }
            val exact = Triple(pos.x, pos.y, pos.z) in SafariWaypointSites.mounds
            catalogMound(box, client) && (exact || !occupied) ||
                level.getFluidState(pos).isEmpty && level.getFluidState(pos.below()).isEmpty &&
                SafariMounds.detected(MoundShape(box.x, box.y, box.z, box.boundingBox.xsize, box.boundingBox.ysize,
                    player.distanceToSqr(box), occupied), limitRange = false)
        }.map(::site).toSet() + SafariEsp.renderedMoundSites(client).filter { it in SafariWaypointSites.mounds }
        val unopened = mounds.filterNot(run.moundSurvey::confirmedBroken).toSet()
        nearbyMounds = unopened.size
        if (SafariAssist.biome == SafariBiome.CAVERN || mounds.isNotEmpty() || run.moundSurvey.hasSites) {
            val loadedInteractions = interactions.mapTo(mutableSetOf(), ::site)
            val now = System.currentTimeMillis()
            absentSince.keys.retainAll { pos ->
                pos !in loadedInteractions && SafariAssist.biome == SafariBiome.CAVERN &&
                    player.distanceToSqr(pos.first + .5, pos.second + .5, pos.third + .5) <= 48.0 * 48
            }
            run.moundSurvey.scan(unopened) { pos ->
                val (x, y, z) = pos
                SafariAssist.biome == SafariBiome.CAVERN && pos !in loadedInteractions &&
                    level.isLoaded(BlockPos(x, y, z)) &&
                    player.distanceToSqr(x + .5, y + .5, z + .5) <= 48.0 * 48 &&
                    now - absentSince.getOrPut(pos) { now } >= 250
            }
        }
        run.observeCavernSources(unopened.isNotEmpty(), walls.intact > 0, walls.allBroken,
            intactWalls.any { level.isLoaded(it) })
    }
    fun reset() { states = snooperPositions.associateWith { WallState.UNKNOWN }; nearbyMounds = 0; ticks = 0; sharedWalls=0; absentSince.clear() }
}
