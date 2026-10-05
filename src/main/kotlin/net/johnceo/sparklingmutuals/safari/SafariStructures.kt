package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EntityType

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
    fun tick(client: Minecraft) {
        if (SafariAssist.biome != SafariBiome.CAVERN) { reset(); return }
        val level = client.level ?: return
        val player = client.player ?: return
        if (++ticks < 10) return
        ticks = 0
        if (ConfigManager.highlightSnooperWalls || ConfigManager.showSnooperWalls) states = snooperPositions.associateWith {
            when { !level.isLoaded(it) -> WallState.UNKNOWN; level.getBlockState(it).isAir -> WallState.BROKEN; else -> WallState.INTACT }
        }
        if (!ConfigManager.showMoundCount) { nearbyMounds = 0; return }
        val entities = level.entitiesForRendering().filter { !it.isRemoved && player.distanceToSqr(it) <= 4096 }.toList()
        val creatures = entities.filter { it.type !in scaffolding }
        nearbyMounds = entities.filter { it.type == EntityType.INTERACTION }.filter { box ->
            SafariMounds.detected(MoundShape(box.x, box.y, box.z, box.boundingBox.xsize, box.boundingBox.ysize,
                player.distanceToSqr(box), creatures.any { kotlin.math.abs(it.x - box.x) <= .35 &&
                    kotlin.math.abs(it.z - box.z) <= .35 && kotlin.math.abs(it.y - box.y) <= 1.0 }))
        }.map { it.blockPosition() }.distinct().size
    }
    fun reset() { states = snooperPositions.associateWith { WallState.UNKNOWN }; nearbyMounds = 0; ticks = 0 }
}
