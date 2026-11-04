package net.johnceo.sparklingmutuals.debug

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.*
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.structures.BeeNests
import net.johnceo.sparklingmutuals.safari.structures.SafariStructures
import net.johnceo.sparklingmutuals.sync.SafariPartySync
import net.johnceo.sparklingmutuals.sync.PartySyncSites
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.UUID

object SafariDeveloper {
    val enabled by lazy {
        FabricLoader.getInstance().getModContainer("sparkling-mutuals").orElse(null)?.metadata
            ?.getCustomValue("sparkling-mutuals:developer")?.asString == "true"
    }
    private data class Event(val time: String, val message: String)
    private val events = ArrayDeque<Event>()
    private val cyan = 0xFF65E2F5.toInt()
    private val logger = org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/testing")
    @Volatile private var session: Path? = null
    private var panelAt = 0L
    private var livePanel: HudPanel? = null

    @Synchronized fun event(message: String, showOnHud: Boolean = true) {
        if (!enabled) return
        if (!showOnHud && session == null) return
        val entry = Event(Instant.now().toString(), message)
        if (showOnHud) {
            if (events.size == 80) events.removeFirst()
            events.addLast(entry)
        }
        session?.let { file ->
            try {
                Files.writeString(file, "${entry.time}  ${entry.message}\n", StandardOpenOption.APPEND)
            } catch (error: java.io.IOException) {
                session = null
                logger.warn("Testing session log stopped after a write failure", error)
            }
        }
    }

    @Synchronized private fun history() = events.toList()

    fun toggleTesting(client: Minecraft): String {
        check(enabled)
        val active = session
        if (active != null) {
            val end = runCatching { snapshot(client) }.getOrNull()
            synchronized(this) {
                event("Testing stopped" + (end?.let { " · snapshot ${it.fileName}" } ?: " · snapshot unavailable"))
                session = null
            }
            return "[SM] Testing stopped. Session log: $active"
        }
        val start = snapshot(client)
        val log = start.resolveSibling("testing-${start.fileName.toString().removePrefix("safari-").removeSuffix(".json")}.log")
        Files.writeString(log, "${Instant.now()}  Testing started · snapshot ${start.fileName}\n", StandardOpenOption.CREATE_NEW)
        session = log
        return "[SM] Testing started. Session log: $log"
    }

    fun preview() = panel(
        listOf(HudRow("Lobby", "m147AH"), HudRow("Transport", "Connected"),
            HudRow("Room roster", "2 players"), HudRow("Assignments", "Backend"), HudRow("Sequence", "8"),
            HudRow("Feeder · local", "1 Wriggleworm"), HudRow("Feeder · received", "Empty"),
            HudRow("Mounds · loaded", "2"), HudRow("Mounds · resolved", "1 local / 2 received"),
            HudRow("Visible · Cavern", "Flitter 2, Scrappy 1"),
            HudRow("Loaded floor markers", "3"), HudRow("Session log", "Recording")),
        listOf("00:00:01  Connected to party room · 2 players", "00:00:03  Outgoing objectives · feeder Empty",
            "00:00:05  Local mound hit · slot 8 · -121,43,24", "00:00:06  Confirmed mound slot 8",
            "00:00:09  Outgoing objectives · mound slots +8")
    )

    private fun panel(rows: List<HudRow>, recent: List<String>) = HudPanel("SAFARI · DEVELOPER", cyan,
        rows + HudRow("Recent sync activity", color = cyan, icon = null) +
            recent.map { HudRow(it, icon = null) }, titleIcon = null)

    fun render(graphics: GuiGraphicsExtractor, client: Minecraft) {
        if (!enabled || client.player == null || !SafariIsland.onIsland ||
            client.options.smHideGui || client.smScreen is ContestGui) return
        val now = System.currentTimeMillis()
        if (now >= panelAt) {
            panelAt = now + 500
            val seen = SafariEsp.observations()
            val biome = SafariAssist.biome
            val nearby = seen.critters.filter { it.biome == biome && !it.mound }
                .groupingBy { it.species }.eachCount().entries.sortedBy { it.key }
            val species = nearby.take(4).joinToString(", ") { "${it.key} ${it.value}" } +
                if (nearby.size > 4) " +${nearby.size - 4} types" else ""
            val rows = SafariPartySync.developerRows() + listOf(
                HudRow("Visible · ${biome?.label ?: "Safari"}", species.ifEmpty { "No tracked critters" }, icon = null),
                HudRow("Loaded floor markers", seen.floorDrops.size.toString(), icon = null),
                HudRow("Session log", if (session != null) "Recording" else "/testing to record", icon = null)
            )
            livePanel = panel(rows, history().takeLast(5).map { "${it.time.substring(11, 19)}  ${it.message.take(105)}" })
        }
        livePanel?.draw(graphics, SafariHud.DEVELOPER)
    }

    fun snapshot(client: Minecraft): Path {
        check(enabled)
        val run = SafariTracking.ledger.current
        val mode = SafariFullClear.mode
        val party = SafariSparklingMode.state
        val data = linkedMapOf(
            "schema" to 1,
            "capturedAt" to Instant.now().toString(),
            "modVersion" to FabricLoader.getInstance().getModContainer("sparkling-mutuals").get().metadata.version.friendlyString,
            "minecraftVersion" to FabricLoader.getInstance().getModContainer("minecraft").get().metadata.version.friendlyString,
            "location" to SafariAssist.location.name,
            "biome" to SafariAssist.biome?.name,
            "mode" to mode.name,
            "playerPosition" to client.player?.let { listOf(it.x, it.y, it.z) },
            "partySync" to SafariPartySync.developerSnapshot(),
            "recentSyncEvents" to history(),
            "render" to SafariEsp.developerSnapshot(client),
            "objectiveSites" to client.level?.takeIf { SafariIsland.onIsland }?.let { level ->
                val sites = mapOf(
                    "nests" to PartySyncSites.nests,
                    "mounds" to PartySyncSites.mounds,
                    "snoozleWalls" to SafariStructures.snooperPositions.map { Triple(it.x, it.y, it.z) },
                    "troodonWalls" to PartySyncSites.troodon
                )
                sites.mapValues { (_, positions) ->
                    positions.mapIndexed { slot, (x, y, z) ->
                        val pos = BlockPos(x, y, z)
                        val loaded = level.isLoaded(pos)
                        mapOf("slot" to slot, "position" to listOf(x, y, z), "loaded" to loaded,
                            "block" to if (loaded) BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).block).toString() else null)
                    }
                }
            },
            "partyDiscovery" to party.debug { it },
            "calculations" to run?.let {
                mapOf(
                    "biomes" to SafariBiome.entries.associate { biome ->
                        biome.name to mapOf("complete" to it.biomeComplete(mode, biome, party),
                            "canReport" to it.canReportBiome(biome, party),
                            "captureEvidence" to it.captureDebug(biome))
                    },
                    "critters" to SafariRoster.all.associate { critter ->
                        critter.name to mapOf("total" to it.count(critter.name), "personal" to it.personalCount(critter.name),
                            "complete" to (it.completed(mode, listOf(critter), party) == 1),
                            "partyNeedsSparkling" to party.needs(critter.name),
                            "sparklingPending" to it.sparklePending(critter.name))
                    },
                    "resources" to FloorDropType.entries.associate { type ->
                        type.name to mapOf("held" to it.resources.heldCount(type), "discovered" to it.resources.discoveredCount(type))
                    },
                    "birds" to it.birds.debug(),
                    "birdFoodCheck" to it.birdFoodCheck.debug(),
                    "gimmiegold" to it.gimmiegoldCheck.debug(),
                    "gemzie" to mapOf("placed" to it.gems.placed, "opened" to it.gems.opened),
                    "nests" to mapOf("found" to BeeNests.nestCount, "remaining" to BeeNests.unpunchedCount,
                        "punched" to BeeNests.punchedCount, "checked" to BeeNests.allChecked),
                    "snoozleWalls" to SafariStructures.walls.states,
                    "moundsRemaining" to it.moundSurvey.remaining
                )
            }
        )
        val folder = FabricLoader.getInstance().gameDir.resolve("sparkling-mutuals-debug")
        Files.createDirectories(folder)
        val file = folder.resolve("safari-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}.json")
        Files.writeString(file, GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data))
        return file
    }
}
