package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.chunk.status.ChunkStatus

/** Clear evidence is collected even with every HUD and ESP switch disabled. */
object SafariBiomeClearTracking {
    private var ticks = 0
    fun tick(client: Minecraft) {
        if (++ticks < 10) return
        ticks = 0
        val run = SafariTracking.ledger.current ?: return
        val level = client.level ?: return
        val player = client.player ?: return
        val biome = SafariAssist.biome ?: return
        if (SafariAssist.location != SafariLocation.INSIDE) return
        val snapshot = SafariEsp.observations()
        if (!snapshot.scanned) return
        snapshot.critters.filterNot { it.mound }.forEach { run.observeCritter(it.id, it.species) }
        run.updateCaptureEvidence(biome, snapshot.critters.filter {
            it.biome == biome && player.distanceToSqr(it.x, it.y, it.z) <= 80.0 * 80
        }.map { if (it.mound) "Rockmite Mound" else it.species }.toSet(),
            SafariStructures.walls.states.size == 5 && SafariStructures.walls.allBroken)
        if (biome == SafariBiome.FOREST) run.floorSurvey.scan(snapshot.floorDrops.filter {
            SafariEspRules.biomeAt(it.x.toDouble(), it.z.toDouble()) == SafariBiome.FOREST
        }.map { Triple(it.x, it.y, it.z) }.toSet()) { (x, y, z) ->
            level.isLoaded(BlockPos(x, y, z)) && player.distanceToSqr(x + .5, y + .5, z + .5) <= 56.0 * 56
        }
        if (biome in run.biomeClears) return
        // Loading the local radius is a prerequisite, not proof that a biome is clear.
        val centre = player.blockPosition()
        val loaded = (((centre.x - 80) shr 4)..((centre.x + 80) shr 4)).all { x ->
            (((centre.z - 80) shr 4)..((centre.z + 80) shr 4)).all { z -> level.getChunk(x, z, ChunkStatus.FULL, false) != null }
        }
        val evidence = BiomeClearEvidence(loaded,
            snapshot.critters.count { it.biome == biome && !it.mound },
            run.brokenMounds >= SafariFullClear.MOUND_MINIMUM && run.moundSurvey.remaining == 0 &&
                SafariStructures.nearbyMounds == 0 && snapshot.critters.none { it.mound },
            SafariStructures.walls.allBroken, BeeNests.allChecked, run.floorSurvey.allCleared,
            snapshot.critters.count { it.biome == biome && !it.mound && it.species == "Macaw" })
        if (!SafariFullClear.eligible(run, biome, evidence)) return
        run.biomeClears.add(biome)
        ConfigManager.personalBests.recordBiome(biome, run, System.currentTimeMillis())?.let {
            ConfigManager.save(); player.sendSystemMessage(Component.literal(it))
        }
    }
}
