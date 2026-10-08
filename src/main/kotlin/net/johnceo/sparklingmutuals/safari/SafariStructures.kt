package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Entity

/** Loaded-world observations adapted from CritterMod 0.9.0; never request chunks. */
object SafariStructures {
    val snooperPositions = listOf(BlockPos(-126, 39, 74), BlockPos(-114, 39, 87), BlockPos(-70, 39, 68),
        BlockPos(-96, 40, 17), BlockPos(-95, 40, 42))
    private var states = snooperPositions.associateWith { WallState.UNKNOWN }
    val walls get() = WallSummary(states.values.toList())
    val intactWalls get() = states.filterValues { it == WallState.INTACT }.keys
    var nearbyMounds = 0
        private set
    private var ticks = 0
    private val scaffolding = setOf(EntityType.ARMOR_STAND, EntityType.ITEM_DISPLAY, EntityType.BLOCK_DISPLAY,
        EntityType.TEXT_DISPLAY, EntityType.PLAYER, EntityType.ITEM, EntityType.INTERACTION)
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
        if (SafariAssist.biome != SafariBiome.CAVERN) { nearbyMounds = 0; ticks = 0; return }
        val level = client.level ?: return
        val player = client.player ?: return
        if (++ticks < 10) return
        ticks = 0
        states = snooperPositions.associateWith {
            when { !level.isLoaded(it) -> states.getValue(it).takeIf { state -> state == WallState.BROKEN } ?: WallState.UNKNOWN
                level.getBlockState(it).isAir -> WallState.BROKEN; else -> WallState.INTACT }
        }
        val run = SafariTracking.ledger.current ?: return
        level.entitiesForRendering().filterNot { it.isRemoved }.forEach {
            if (SafariEspRules.biomeAt(it.x, it.z) == SafariBiome.CAVERN)
                run.observe(EspEntity(it.type.toShortString(), invisible = it.isInvisible, passengers = it.passengers.isNotEmpty()))
        }
        val entities = level.entitiesForRendering().filter { !it.isRemoved && player.distanceToSqr(it) <= 4096 }.toList()
        val creatures = entities.filter { it.type !in scaffolding }
        val mounds = entities.filter { it.type == EntityType.INTERACTION }.filter { box ->
            SafariMounds.detected(MoundShape(box.x, box.y, box.z, box.boundingBox.xsize, box.boundingBox.ysize,
                player.distanceToSqr(box), creatures.any { kotlin.math.abs(it.x - box.x) <= .35 &&
                    kotlin.math.abs(it.z - box.z) <= .35 && kotlin.math.abs(it.y - box.y) <= 1.0 }))
        }.map { it.blockPosition() }.toSet()
        nearbyMounds = mounds.size
        run.moundSurvey.scan(mounds.map { Triple(it.x, it.y, it.z) }.toSet()) { (x, y, z) ->
            level.isLoaded(BlockPos(x, y, z)) && player.distanceToSqr(x + .5, y + .5, z + .5) <= 56.0 * 56
        }
    }
    fun reset() { states = snooperPositions.associateWith { WallState.UNKNOWN }; nearbyMounds = 0; ticks = 0 }
}
