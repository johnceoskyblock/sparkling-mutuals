package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.hud.*
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.effect.MobEffects
import kotlin.math.sqrt

/** Loaded-name scan and label/body pairing adapted from CritterMod's v0.9.0 CritterEntities. */
object SafariAssist {
    private data class Shiny(val label: Entity, val body: Entity, val name: String)
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
        if (!inSafari || !(ConfigManager.shinyDetection || ConfigManager.sparklingAlert || ConfigManager.sparklingPartyAnnouncer)) {
            shinies = emptyList(); SparklingEncounters.leave(); return
        }
        val entities = client.level!!.entitiesForRendering().filterNot { it.isRemoved }.toList()
        val bodies = entities.filter { it is LivingEntity && it !is ArmorStand && it !is Player }
        val matchedBodies = mutableSetOf<Int>()
        shinies = entities.mapNotNull { label ->
            val name = label.customName?.string?.let(SafariRules::sparklingSpecies) ?: return@mapNotNull null
            val body = bodies.minByOrNull { it.position().distanceToSqr(label.position()) }
                ?.takeIf { it.position().distanceToSqr(label.position()) < 9 } ?: label
            if (!SafariRules.withinRange(player.distanceToSqr(body)) ||
                biome == null || SafariAreaMap.biomeAt(body.x, body.y, body.z) != mappedBiome || !matchedBodies.add(body.id)) null
            else Shiny(label, body, name)
        }.sortedBy { player.distanceToSqr(it.body) }
        shinies.forEach { SparklingEncounters.observe(it.label.uuid, it.name, biome!!, it.body.blockPosition()) }
        SparklingEncounters.tick(client)
    }

    @JvmStatic fun hidePaintings() = ConfigManager.hideHauntedPaintings && inHaunted
    @JvmStatic fun isHighlighted(id: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        return ConfigManager.shinyDetection && inSafari && shinies.any {
            !it.body.isRemoved && (it.body.id == id || it.label.id == id) && SafariRules.withinRange(player.distanceToSqr(it.body))
        }
    }

    fun nearbyPanel(preview: Boolean = false): HudPanel? {
        val client = Minecraft.getInstance()
        if (preview) return HudPanel("Nearby sparklings (80m)", rows = listOf(HudRow("Doomspiral", "32m", SafariRoster.color("Doomspiral"))))
        val player = client.player ?: return null
        val nearby = shinies.filter { !it.body.isRemoved && SafariRules.withinRange(player.distanceToSqr(it.body)) }
        if (nearby.isEmpty()) return null
        val rows = nearby.take(6).map { HudRow(it.name, "${sqrt(player.distanceToSqr(it.body)).toInt()}m", SafariRoster.color(it.name), HudRow.WHITE) }.toMutableList()
        if (nearby.size > 6) rows.add(HudRow("+${nearby.size - 6} more nearby", color = HudRow.GRAY))
        return HudPanel("Nearby sparklings (80m)", rows = rows)
    }
    fun render(graphics: GuiGraphicsExtractor) {
        if (!ConfigManager.shinyDetection || !inSafari || Minecraft.getInstance().screen is ContestGui) return
        nearbyPanel()?.draw(graphics, SafariHud.SPARKLINGS)
    }

    fun reset() { shinies = emptyList(); inSafari = false; inHaunted = false; atEntrance = false;
        biome = null; entered = false; location = SafariLocation.UNKNOWN; ticks = 0; lastLevel = null; SparklingEncounters.reset() }
}
