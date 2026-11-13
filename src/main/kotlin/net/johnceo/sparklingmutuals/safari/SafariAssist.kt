package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.compat.*
import kotlin.math.sqrt
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
import net.johnceo.sparklingmutuals.safari.esp.CritterCapsuleHider
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingDetector
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

/** Positive name/skin scan of loaded entities for run-owned alerts and detection presentation. */
object SafariAssist {
    private data class Shiny(val label: Entity, val body: Entity, val name: String, val biome: SafariBiome, val identity: java.util.UUID)
    private var shinies = emptyList<Shiny>()
    private var ticks = 0
    private var lastLevel: Any? = null
    var inSafari = false
        private set
    var inHaunted = false
        private set
    var atEntrance = false
        private set
    var biome: SafariBiome? = null
        private set
    private var entered = false
    var departing = false
        private set
    var location = SafariLocation.UNKNOWN
        private set
    fun markEntered() { entered = true; inSafari = true; location = SafariLocation.INSIDE }

    fun markDeparting() {
        departing = true
        shinies = emptyList()
        SparklingEncounters.leave()
    }

    fun observeParticle(packet: net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket) {
        if (!inSafari || departing || packet.particle.type != net.minecraft.core.particles.ParticleTypes.WAX_ON) return
        val run = SafariTracking.ledger.current ?: return
        run.sparklingParticles.observe(true, true, packet.x, packet.y, packet.z, System.currentTimeMillis())
        // Alert after the next name/skin scan so known critters take priority over packets.
    }

    fun onClientTick(client: Minecraft) {
        if (client.level !== lastLevel) { reset(); lastLevel = client.level }
        if (departing) return
        val player = client.player
        // Respawning may briefly replace the player while the Safari world is unchanged.
        // World changes and disconnects reset the session separately; a missing player must not.
        if (client.level == null || player == null) { shinies = emptyList(); return }
        if (ConfigManager.removeDarkness && visualsActive(client)) player.removeEffect(MobEffects.DARKNESS)
        if (++ticks < 5) return
        ticks = 0
        val lines = SkyblockSidebar.areaName(client)?.let { listOf("Area: $it") }.orEmpty() + SkyblockSidebar.lines(client)
        val area = SafariAreaMap.areaAt(player.x, player.y, player.z)
        val mappedBiome = area?.takeIf { it in 1..4 }
        location = SafariLocation.resolve(lines, area, entered)
        if (location != SafariLocation.UNKNOWN) entered = location == SafariLocation.INSIDE
        atEntrance = location == SafariLocation.ENTRANCE
        inSafari = location == SafariLocation.INSIDE || (location == SafariLocation.UNKNOWN && entered)
        biome = if (inSafari) SafariBiome.mapped(mappedBiome) else null
        inHaunted = inSafari && (SafariRules.isHaunted(lines) || biome == SafariBiome.HAUNTED)
        SafariTracking.ensureRun(client)
        if (!inSafari) {
            shinies = emptyList()
            if (!SafariIsland.onIsland) SparklingEncounters.leave()
            return
        }
        val entities = client.level!!.entitiesForRendering().filterNot { it.isRemoved }.toList()
        val realPlayers = client.connection?.listedOnlinePlayers?.map { it.profile.id }?.toSet().orEmpty() + player.uuid
        val partyMembers = PartyManager.getMembers().toSet()
        val capsules = entities.filter {
            CritterCapsuleHider.itemId(it) in setOf("CRITTER_CAPSULE", "MASTERFUL_CRITTER_CAPSULE")
        }.mapTo(mutableSetOf()) { it.id }
        val snapshots = entities.map { entity ->
            val realPlayer = entity is Player && (entity.uuid in realPlayers || entity.uuid.toString() in partyMembers)
            val carrier = entity is Player || entity is ArmorStand || entity is net.minecraft.world.entity.Display.ItemDisplay
            SparklingEntity(entity.id, entity.uuid, entity.x, entity.y, entity.z, entity.customName?.string,
                if (entity is Player && !realPlayer)
                    SafariEsp.texture(client.connection?.getPlayerInfo(entity.uuid)?.profile ?: entity.gameProfile)
                else if (carrier && !realPlayer) SafariEsp.describe(entity).texture else null, realPlayer, carrier,
                entity is LivingEntity && entity !is ArmorStand && !realPlayer,
                entity.id in capsules || entity.vehicle?.id in capsules)
        }
        val byId = entities.associateBy { it.id }
        val captureMarkers = SparklingDetector.captureMarkers(snapshots)
        shinies = SparklingDetector.detect(snapshots).mapNotNull { match ->
            val body = byId[match.body] ?: return@mapNotNull null
            val label = byId[match.label] ?: return@mapNotNull null
            val biome = SafariRoster.named(match.species)?.biome ?: return@mapNotNull null
            val identity = SafariTracking.ledger.current?.observeSparkling(body.uuid, match.species, match.aliases) ?: body.uuid
            val alreadyAlerted = SafariTracking.ledger.current?.sparklingParticles?.let { particles ->
                SparklingEncounters.identify(particles.identify(body.x, body.y, body.z) +
                    particles.identify(label.x, label.y, label.z))
            } == true
            if (SafariTracking.ledger.current?.sparklingCaptures?.resolved(identity) == true) return@mapNotNull null
            SparklingEncounters.observe(identity, match.species, biome, body.blockPosition(), alreadyAlertedByParticle = alreadyAlerted)
            Shiny(label, body, match.species, biome, identity)
        }.sortedBy { player.distanceToSqr(it.body) }
        SafariTracking.ledger.current?.sparklingParticles?.recentUnknown(System.currentTimeMillis())?.forEach { point ->
            if (SparklingDetector.atCapture(point.x, point.y, point.z, captureMarkers) ||
                net.johnceo.sparklingmutuals.sync.SafariPartySync.hasOutstandingSharedSparkling(point.biome)) {
                point.identified = true
            }
            else
                SparklingEncounters.observeUnknown(point.id, point.biome,
                    net.minecraft.core.BlockPos.containing(point.x, point.y, point.z))
        }
        SparklingEncounters.tick(client)
    }

    fun diagnostics(): List<String> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        return SafariTracking.ledger.current?.sparklingParticles?.unknown().orEmpty().map {
            "Sparkling detection: source=wax_on species=unknown biome=${it.biome} pos=(${it.x}, ${it.y}, ${it.z}) lastSeen=${it.seenAt}"
        } + shinies.map { "Sparkling detection: species=${it.name} biome=${it.biome} " +
            "source=${if (it.label.customName?.string?.let(SafariRules::sparklingSpecies) != null) "name" else "skin"} " +
            "body=${it.body.uuid} label=${it.label.uuid} distance=${sqrt(player.distanceToSqr(it.body)).toInt()}m" }
    }
    internal fun visualsActive(client: Minecraft = Minecraft.getInstance()) =
        SafariIsland.onIsland && inSafari && client.level != null && client.level === lastLevel

    @JvmStatic fun hidePaintings() = ConfigManager.hideHauntedPaintings && inHaunted && visualsActive()
    @JvmStatic fun isHighlighted(id: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        return !ConfigManager.hideCritterEsp() && ConfigManager.shinyDetection && visualsActive(client) && shinies.any {
            !it.body.isRemoved && (it.body.id == id || it.label.id == id) &&
                net.johnceo.sparklingmutuals.config.SafariEspConfig.modWideEnabled(it.name)
        }
    }

    @JvmStatic fun isGlowHighlighted(id: Int)=isHighlighted(id) && shinies.none {
        (it.body.id==id || it.label.id==id) && net.johnceo.sparklingmutuals.config.SafariEspConfig.boxGlowFallback(it.name)
    }
    internal fun boxHighlights(): List<Pair<Entity,String>> = shinies.filter {
        net.johnceo.sparklingmutuals.config.SafariEspConfig.boxGlowFallback(it.name) && isHighlighted(it.body.id)
    }.distinctBy {it.body.id}.map {it.body to it.name}

    fun nearbyPanel(preview: Boolean = false): HudPanel? {
        val client = Minecraft.getInstance()
        if (preview) return HudPanel("Detected sparklings", rows = listOf(HudRow("Doomspiral", "32m", SafariRoster.color("Doomspiral"))))
        val player = client.player ?: return null
        val nearby = shinies.filter { !it.body.isRemoved }
        if (nearby.isEmpty()) return null
        val rows = nearby.take(6).map { HudRow(it.name, "${it.biome.label} ${sqrt(player.distanceToSqr(it.body)).toInt()}m", SafariRoster.color(it.name), HudRow.WHITE) }.toMutableList()
        if (nearby.size > 6) rows.add(HudRow("+${nearby.size - 6} more sightings", color = HudRow.GRAY))
        return HudPanel("Detected sparklings", rows = rows)
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.shinyDetection || !inSafari || Minecraft.getInstance().smScreen is ContestGui) return
        nearbyPanel()?.draw(graphics, SafariHud.SPARKLINGS)
    }

    fun reset() { shinies = emptyList(); inSafari = false; inHaunted = false; atEntrance = false;
        biome = null; entered = false; departing = false; location = SafariLocation.UNKNOWN; ticks = 0; lastLevel = null; SparklingEncounters.reset() }
}
