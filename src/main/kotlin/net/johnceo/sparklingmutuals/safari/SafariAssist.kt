package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
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

    fun onClientTick(client: Minecraft) {
        if (client.level !== lastLevel) { reset(); lastLevel = client.level }
        val player = client.player
        if (client.level == null || player == null) { reset(); return }
        if (++ticks < 5) return
        ticks = 0
        val lines = SkyblockSidebar.areaName(client)?.let { listOf("Area: $it") }.orEmpty() + SkyblockSidebar.lines(client)
        val biome = SafariAreaMap.biomeAt(player.x, player.y, player.z)
        inSafari = SkyblockSidebar.inSkyblock(client) && SafariRules.isSafari(lines) && biome != null
        inHaunted = inSafari && (SafariRules.isHaunted(lines) || biome == 4)
        if (!inSafari || !ConfigManager.shinyDetection) { shinies = emptyList(); return }
        val entities = client.level!!.entitiesForRendering().filterNot { it.isRemoved }.toList()
        val bodies = entities.filter { it is LivingEntity && it !is ArmorStand && it !is Player }
        val matchedBodies = mutableSetOf<Int>()
        shinies = entities.mapNotNull { label ->
            val name = label.customName?.string?.let(SafariRules::sparklingSpecies) ?: return@mapNotNull null
            val body = bodies.filter { it.position().distanceToSqr(label.position()) < 9 }
                .minByOrNull { it.position().distanceToSqr(label.position()) } ?: label
            if (!SafariRules.withinRange(player.distanceToSqr(body)) ||
                SafariAreaMap.biomeAt(body.x, body.y, body.z) != biome || !matchedBodies.add(body.id)) null
            else Shiny(label, body, name)
        }.sortedBy { player.distanceToSqr(it.body) }
    }

    @JvmStatic fun hidePaintings() = ConfigManager.hideHauntedPaintings && inHaunted
    @JvmStatic fun isHighlighted(id: Int): Boolean {
        val client = Minecraft.getInstance()
        val player = client.player ?: return false
        return ConfigManager.shinyDetection && inSafari && shinies.any {
            !it.body.isRemoved && (it.body.id == id || it.label.id == id) && SafariRules.withinRange(player.distanceToSqr(it.body))
        }
    }

    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (!ConfigManager.shinyDetection || !inSafari) return
        val nearby = shinies.filter { !it.body.isRemoved && SafariRules.withinRange(player.distanceToSqr(it.body)) }
        if (nearby.isEmpty()) return
        val rows = nearby.take(6).map { "${it.name} · ${sqrt(player.distanceToSqr(it.body)).toInt()}m" }.toMutableList()
        if (nearby.size > 6) rows.add("+${nearby.size - 6} more nearby")
        val x = 8
        val y = (client.window.guiScaledHeight - rows.size * 12 - 55).coerceAtLeast(8)
        val panelWidth = maxOf(client.font.width("Nearby sparklings (80m)"), rows.maxOf(client.font::width)) + 12
        graphics.fill(x, y, x + panelWidth, y + 20 + rows.size * 12, 0xDD16211A.toInt())
        graphics.text(client.font, "Nearby sparklings (80m)", x + 6, y + 5, 0xFFE3BB67.toInt(), true)
        rows.forEachIndexed { index, row -> graphics.text(client.font, row, x + 6, y + 19 + index * 12, 0xFFF0E1BE.toInt(), true) }
    }

    fun reset() { shinies = emptyList(); inSafari = false; inHaunted = false; ticks = 0; lastLevel = null }
}
