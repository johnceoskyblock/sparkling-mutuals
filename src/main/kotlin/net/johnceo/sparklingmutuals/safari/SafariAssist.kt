package net.johnceo.sparklingmutuals.safari

import kotlin.math.sqrt
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.party.PartyManager
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp
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

/** Positive name/skin scan of loaded entities; alerts and progress share the same run-owned evidence. */
object SafariAssist {
    private data class Shiny(val label: Entity, val body: Entity, val name: String, val biome: SafariBiome)
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
    var location = SafariLocation.UNKNOWN
        private set
    fun markEntered() { entered = true; inSafari = true; location = SafariLocation.INSIDE }

    fun onClientTick(client: Minecraft) {
        if (client.level !== lastLevel) { reset(); lastLevel = client.level }
        val player = client.player
        if (client.level == null || player == null) { reset(); return }
        if (ConfigManager.removeDarkness && inSafari) player.removeEffect(MobEffects.DARKNESS)
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
            shinies = emptyList(); SparklingEncounters.leave(); return
        }
        val entities = client.level!!.entitiesForRendering().filterNot { it.isRemoved }.toList()
        val realPlayers = client.connection?.listedOnlinePlayers?.map { it.profile.id }?.toSet().orEmpty() + player.uuid
        val partyMembers = PartyManager.getMembers().toSet()
        val snapshots = entities.map { entity ->
            val realPlayer = entity is Player && (entity.uuid in realPlayers || entity.uuid.toString() in partyMembers)
            val carrier = entity is Player || entity is ArmorStand || entity is net.minecraft.world.entity.Display.ItemDisplay
            SparklingEntity(entity.id, entity.uuid, entity.x, entity.y, entity.z, entity.customName?.string,
                if (entity is Player && !realPlayer)
                    SafariEsp.texture(client.connection?.getPlayerInfo(entity.uuid)?.profile ?: entity.gameProfile)
                else if (carrier && !realPlayer) SafariEsp.describe(entity).texture else null, realPlayer, carrier,
                entity is LivingEntity && entity !is ArmorStand && !realPlayer)
        }
        val byId = entities.associateBy { it.id }
        shinies = SparklingDetector.detect(snapshots).mapNotNull { match ->
            val body = byId[match.body] ?: return@mapNotNull null
            val label = byId[match.label] ?: return@mapNotNull null
            val biome = SafariRoster.named(match.species)?.biome ?: return@mapNotNull null
            val identity = SafariTracking.ledger.current?.observeSparkling(body.uuid, match.species, match.aliases) ?: body.uuid
            SparklingEncounters.observe(identity, match.species, biome, body.blockPosition())
            Shiny(label, body, match.species, biome)
        }.sortedBy { player.distanceToSqr(it.body) }
        SparklingEncounters.tick(client)
    }

    fun diagnostics(): List<String> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        return shinies.map { "Sparkling detection: species=${it.name} biome=${it.biome} " +
            "source=${if (it.label.customName?.string?.let(SafariRules::sparklingSpecies) != null) "name" else "skin"} " +
            "body=${it.body.uuid} label=${it.label.uuid} distance=${sqrt(player.distanceToSqr(it.body)).toInt()}m" }
    }
    @JvmStatic fun hidePaintings() = ConfigManager.hideHauntedPaintings && inHaunted
    @JvmStatic fun isHighlighted(id: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        return ConfigManager.shinyDetection && inSafari && shinies.any {
            !it.body.isRemoved && (it.body.id == id || it.label.id == id)
        }
    }

    fun nearbyPanel(preview: Boolean = false): HudPanel? {
        val client = Minecraft.getInstance()
        if (preview) return HudPanel("Detected sparklings", rows = listOf(HudRow("Doomspiral", "32m", SafariRoster.color("Doomspiral"))))
        val player = client.player ?: return null
        val nearby = shinies.filter { !it.body.isRemoved }
        if (nearby.isEmpty()) return null
        val rows = nearby.take(6).map { HudRow(it.name, "${it.biome.label} ${sqrt(player.distanceToSqr(it.body)).toInt()}m", SafariRoster.color(it.name), HudRow.WHITE) }.toMutableList()
        if (nearby.size > 6) rows.add(HudRow("+${nearby.size - 6} more nearby", color = HudRow.GRAY))
        return HudPanel("Detected sparklings", rows = rows)
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.shinyDetection || !inSafari || Minecraft.getInstance().screen is ContestGui) return
        nearbyPanel()?.draw(graphics, SafariHud.SPARKLINGS)
    }

    fun reset() { shinies = emptyList(); inSafari = false; inHaunted = false; atEntrance = false;
        biome = null; entered = false; location = SafariLocation.UNKNOWN; ticks = 0; lastLevel = null; SparklingEncounters.reset() }
}
