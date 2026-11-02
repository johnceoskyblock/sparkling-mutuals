package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
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
    val titles get() = mapOf(SafariHud.RUN_COMPLETE to ConfigManager.runCompleteTitle, SafariHud.GEMS to "ALL GEMS READY!", SafariHud.BIRD_FOOD to "ALL BIRD FOOD COLLECTED!",
        SafariHud.COINS to "ALL COINS COLLECTED!", SafariHud.INCENSE to "ALL INCENSE READY!", SafariHud.WARP to "Warping Now")
    fun previews() = mapOf(SafariHud.BANNER to SafariBanners.previewPanel())
    fun preview(hud: SafariHud) = HudPanel(titles.getValue(hud), color(hud), emptyList())
    private fun color(hud: SafariHud) = if (hud == SafariHud.RUN_COMPLETE)
        SafariEspConfig.rgb(AppearanceConfig.runCompleteColor)
        else if (hud == SafariHud.WARP) 0xFFFF5555.toInt() else 0xFF55FF55.toInt()
    private fun kind(hud: SafariHud) = when(hud) {
        SafariHud.GEMS -> BannerKind.GEMS;SafariHud.BIRD_FOOD -> BannerKind.BIRD_FOOD;SafariHud.COINS -> BannerKind.COINS
        SafariHud.INCENSE -> BannerKind.INCENSE;SafariHud.RUN_COMPLETE -> BannerKind.RUN_COMPLETE;SafariHud.WARP -> BannerKind.WARP
        else -> null
    }
    fun show(hud: SafariHud) { kind(hud)?.let { SafariBanners.fire(it) } }
    fun clear(hud: SafariHud) { notices.clear(hud);kind(hud)?.let(SafariBanners::clear) }
    fun resetInventory() = titles.keys.filter { it != SafariHud.WARP && it != SafariHud.RUN_COMPLETE }.forEach(::clear)
    fun render(graphics: GuiGraphicsExtractor) = SafariBanners.render(graphics)
}
