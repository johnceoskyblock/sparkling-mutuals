package net.johnceo.sparklingmutuals.safari.completion

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariLocation
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.structures.BeeNests
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.johnceo.sparklingmutuals.safari.structures.WallState
import net.minecraft.client.Minecraft

/** Clear evidence is collected even with every HUD and ESP switch disabled. */
object SafariBiomeClearTracking {
    private var ticks = 0
    fun tick(client: Minecraft) {
        if (++ticks < 10) return
        ticks = 0
        val run = SafariTracking.ledger.current ?: return
        if (client.level == null) return
        val player = client.player ?: return
        val currentBiome = SafariAssist.biome
        if (SafariAssist.location != SafariLocation.INSIDE) return
        val snapshot = SafariEsp.observations()
        if (!snapshot.scanned) return
        val inventory = player.inventory
        run.inventoryEvidence((0 until inventory.containerSize).map { inventory.getItem(it) }
            .filterNot { it.isEmpty }.map { it.hoverName.string to it.count })
        run.localPlayer(player.name.string)
        // Forest food markers are visible Safari-wide, including the central boat and other biomes.
        run.scanBirdSources(
            snapshot.forestBirdFoodDropsRemaining,
            snapshot.critters.filter { it.biome == SafariBiome.FOREST && !it.mound }.distinctBy { it.uuid }.map { it.species },
            qualified = currentBiome == SafariBiome.FOREST && ForestCompletionZones.inBirdScanRadius(player.x, player.z))
        val biome = currentBiome ?: return
        run.observePopulation(snapshot.critters.filterNot { it.mound }.map { it.species to it.uuid })
        snapshot.critters.filterNot { it.mound }.forEach { run.observeCritter(it.id, it.species, it.uuid) }
        run.updateCaptureEvidence(biome, snapshot.critters.filter {
            it.biome == biome
        }.map { if (it.mound) "Rockmite Mound" else it.species }.toSet(),
            SafariStructures.walls.states.size == 5 && SafariStructures.walls.allBroken, BeeNests.allChecked,
            macawsInRange = snapshot.critters.count { it.biome == biome && it.species == "Macaw" },
            playerX = player.x, playerZ = player.z, nestsPunched = BeeNests.punchedCount, nestsFound = BeeNests.nestCount)
        run.liveHoneybugs(snapshot.critters.filter { it.species == "Honeybug" && !it.mound }.map { it.uuid }.distinct().size)
        if (biome == SafariBiome.HAUNTED) {
            run.scanGimmiegolds(snapshot.critters.filter { it.species == "Gimmiegold" && !it.mound }.map { it.uuid },
                (player.x + 4) * (player.x + 4) + (player.z + 64) * (player.z + 64) <= 225)
        }
        run.sparklingChecks.scan(biome, player.x, player.z,
            snapshot.critters.filterNot { it.mound }.map { it.species to it.uuid },
            nestsChecked = run.nestsResolved,
            moundsCleared = run.allMoundsBroken && SafariStructures.nearbyMounds == 0 && snapshot.critters.none { it.mound },
            wallsCleared = SafariStructures.walls.states.size == 5 && SafariStructures.walls.allBroken,
            birdsSpawned = run.birds.sparklingSpawned,
            hauntedDropsRemaining = snapshot.floorDrops.count { SafariEspRules.biomeAt(it.x.toDouble(), it.z.toDouble()) == SafariBiome.HAUNTED },
            hauntedCoinDropsRemaining = snapshot.hauntedCoinDropsRemaining)
        run.reconcileStructures(biome, SafariStructures.nearbyMounds > 0 || snapshot.critters.any { it.mound },
            SafariStructures.walls.states.any { it == WallState.INTACT })
        SafariCompletion.tick(client, run, biome)
        // A scan can prove the last Unique critter absent after its biome's final catch.
        SafariTracking.checkPartyUnique(client, biome)
        if (biome in run.biomeClears) return
        // A completed entity scan is sufficient; unloaded surrounding chunks must not block a PB.
        val evidence = BiomeClearEvidence(snapshot.scanned,
            snapshot.critters.count { it.biome == biome && !it.mound },
            run.moundsComplete() &&
                SafariStructures.nearbyMounds == 0 && snapshot.critters.none { it.mound },
            SafariStructures.walls.states.size == 5 && SafariStructures.walls.allBroken, BeeNests.allChecked,
            nearbyMacaws = snapshot.critters.count { it.biome == biome && !it.mound && it.species == "Macaw" },
            nestsFound = BeeNests.nestCount)
        SafariFullClear.recordClear(run, biome, evidence, ConfigManager.personalBests, System.currentTimeMillis())?.let {
            ConfigManager.save(); player.sendSystemMessage(LocalChat.component(it))
        }
    }
}
