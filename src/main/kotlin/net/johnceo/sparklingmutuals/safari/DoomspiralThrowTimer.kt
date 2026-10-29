package net.johnceo.sparklingmutuals.safari

import java.util.Locale
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.monster.warden.Warden

/** Counts observed server ticks, never client frames or elapsed wall time. */
class DoomspiralCountdown {
    private var triggered = false
    private var active = false
    private var elapsed = 0
    private var throwAt = 0
    val remaining: Int? get() = if (active) (throwAt - elapsed).coerceAtLeast(0) else null
    fun trigger(raw: String, inSafari: Boolean, emergenceTicks: Int, leadTicks: Int): Boolean {
        if (!inSafari || triggered || SafariMessages.lines(raw).none { it == "Something stirs in the Haunted Biome..." }) return false
        triggered = true
        active = true
        elapsed = 0
        throwAt = (emergenceTicks.coerceIn(1, 600) - leadTicks.coerceIn(0, 40)).coerceAtLeast(0)
        return true
    }
    fun serverTick() {
        if (!active) return
        elapsed++
        if (elapsed >= throwAt + 40) clear()
    }
    fun clear() { active = false }
    fun reset() { clear(); triggered = false; elapsed = 0 }
}

/** PY-style server ping clock, independently implemented after reviewing NoammAddons (CC0). */
object DoomspiralThrowTimer {
    private val countdown = DoomspiralCountdown()
    private var run: SafariRun? = null
    private var world: Any? = null
    private var connection: Any? = null
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay && SafariMessages.lines(message.string).any { it == "Something stirs in the Haunted Biome..." }) Minecraft.getInstance().execute {
                val client = Minecraft.getInstance()
                if (!ConfigManager.doomspiralThrowTimer || !SafariAssist.inSafari) return@execute
                SafariTracking.ensureRun(client)
                if (!context(client)) return@execute
                if ((run?.count("Doomspiral") ?: 0) == 0)
                    countdown.trigger(message.string, true, ConfigManager.doomspiralEmergenceTicks, ConfigManager.doomspiralThrowLeadTicks)
            }
        }
    }
    private fun context(client: Minecraft): Boolean {
        val current = SafariTracking.ledger.current
        if (client.player == null || client.level == null || client.connection == null ||
            !SafariAssist.inSafari || current == null || current.endedAt != null || !ConfigManager.doomspiralThrowTimer) {
            reset(); return false
        }
        if (run !== current || world !== client.level || connection !== client.connection) {
            countdown.reset(); run = current; world = client.level; connection = client.connection
        }
        if (current.count("Doomspiral") > 0) countdown.clear()
        return true
    }
    fun tick(client: Minecraft) { context(client) }
    /** Called only after vanilla has processed a server ping on the render thread. */
    fun serverTick(listener: Any, pingId: Int) {
        val client = Minecraft.getInstance()
        if (pingId != 0 && client.connection === listener && context(client)) countdown.serverTick()
    }
    fun panel(ticks: Int): HudPanel {
        val value = if (ticks == 0) "THROW NOW" else "$ticks ticks (${String.format(Locale.ROOT, "%.2f", ticks / 20.0)}s)"
        val color = when { ticks == 0 -> 0xFF55FF55.toInt(); ticks <= 10 -> 0xFFFF5555.toInt(); ticks <= 25 -> 0xFFFFAA00.toInt(); else -> HudRow.WHITE }
        return HudPanel("Doomspiral throw", rows = listOf(HudRow(value, color = color)))
    }
    fun preview() = panel(40)
    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (client.options.smHideGui || client.smScreen is ContestGui || !context(client)) return
        val ticks = countdown.remaining ?: return
        val player = client.player ?: return
        val level = client.level ?: return
        if (level.getEntitiesOfClass(Warden::class.java, player.boundingBox.inflate(10.0)) {
                !it.isRemoved && it.isAlive && player.distanceToSqr(it) <= 100.0
            }.isEmpty()) return
        panel(ticks).draw(graphics, SafariHud.DOOM_THROW)
    }
    fun reset() { countdown.reset(); run = null; world = null; connection = null }
}
