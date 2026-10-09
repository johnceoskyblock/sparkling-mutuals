package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.contest.ContestGui
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

data class HudNotice(val text: String, val color: Int, val expiresAt: Long)
class TimedHudAlerts {
    private val notices = mutableMapOf<SafariHud, HudNotice>()
    fun show(hud: SafariHud, text: String, color: Int, now: Long, duration: Long = 4000) {
        notices[hud] = HudNotice(text, color, now + duration)
    }
    fun active(now: Long): Map<SafariHud, HudNotice> {
        notices.entries.removeIf { it.value.expiresAt <= now }
        return notices.toMap()
    }
    fun clear(hud: SafariHud) { notices.remove(hud) }
}

/** Independent HUD channels leave server and sparkling titles alone. */
object SmallAlerts {
    private val notices = TimedHudAlerts()
    val titles = mapOf(SafariHud.GEMS to "ALL GEMS READY!", SafariHud.BIRD_FOOD to "ALL BIRD FOOD COLLECTED!",
        SafariHud.INCENSE to "ALL INCENSE READY!", SafariHud.WARP to "WARP REMINDER")
    fun preview(hud: SafariHud) = HudPanel(titles.getValue(hud), color(hud), emptyList())
    private fun color(hud: SafariHud) = if (hud == SafariHud.WARP) 0xFFFF5555.toInt() else 0xFF55FF55.toInt()
    fun show(hud: SafariHud) = notices.show(hud, titles.getValue(hud), color(hud), System.currentTimeMillis())
    fun clear(hud: SafariHud) = notices.clear(hud)
    fun resetInventory() = titles.keys.filter { it != SafariHud.WARP }.forEach(::clear)
    fun render(graphics: GuiGraphicsExtractor) {
        val active = notices.active(System.currentTimeMillis())
        val client = Minecraft.getInstance()
        if (client.player == null || client.options.hideGui || client.screen is ContestGui) return
        active.forEach { (hud, notice) -> HudPanel(notice.text, notice.color, emptyList()).draw(graphics, hud) }
    }
}
