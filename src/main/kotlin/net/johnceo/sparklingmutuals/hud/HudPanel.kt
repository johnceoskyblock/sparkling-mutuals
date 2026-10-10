package net.johnceo.sparklingmutuals.hud
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.PanelStyle

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Layout and progress tracks adapted from CritterMod v0.9.0 HudPanel (MIT). */
data class HudRow(val label: String = "", val value: String? = null, val color: Int = WHITE,
    val valueColor: Int = color, val fraction: Float? = null) {
    companion object {
        const val WHITE = -1
        val GRAY = 0xFFAAAAAA.toInt()
        val GOLD = 0xFF000000.toInt() or net.johnceo.sparklingmutuals.config.SafariTheme.ACCENT
    }
}
class HudPanel(val title: String, val titleColor: Int = HudRow.GOLD, val rows: List<HudRow>) {
    fun width(measure: (String) -> Int): Int {
        val labels = rows.maxOfOrNull { measure(it.label) } ?: 0
        val values = rows.mapNotNull { it.value }.maxOfOrNull(measure) ?: 0
        return maxOf(measure(title), labels + (if (values > 0) values + 6 else 0) +
            (if (rows.any { it.fraction != null }) 40 else 0)) + 12
    }
    val height get() = 12 + 11 * (1 + rows.size)
    fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, scale: Float = 1f,
        style: PanelStyle = AppearanceConfig.panels.getValue("captures")) {
        val font = Minecraft.getInstance().font
        val w = width(font::width)
        val valueWidth = rows.mapNotNull { it.value }.maxOfOrNull(font::width) ?: 0
        graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(scale, scale)
        style.draw(graphics, w, height)
        graphics.text(font, title, 6, 6, titleColor, true)
        rows.forEachIndexed { index, row ->
            val top = 17 + index * 11
            if (row.label.isNotEmpty()) graphics.text(font, row.label, 6, top, row.color, true)
            row.value?.let { graphics.text(font, it, w - 6 - font.width(it), top, row.valueColor, true) }
            row.fraction?.let { fraction ->
                val left = w - 6 - valueWidth - 6 - 34
                graphics.fill(left, top + 3, left + 34, top + 7, 0xFF555555.toInt())
                val filled = (34 * fraction.coerceIn(0f, 1f)).toInt()
                if (fraction > 0) graphics.fill(left, top + 3, left + filled.coerceAtLeast(1), top + 7, row.valueColor)
            }
        }
        graphics.pose().popMatrix()
    }
    fun bounds(hud: SafariHud, screenWidth: Int, screenHeight: Int) =
        hud.layout.bounds(screenWidth, screenHeight, width(Minecraft.getInstance().font::width), height)
    fun draw(graphics: GuiGraphicsExtractor, hud: SafariHud) {
        val window = Minecraft.getInstance().window
        val bounds = bounds(hud, window.guiScaledWidth, window.guiScaledHeight)
        draw(graphics, bounds.x, bounds.y, hud.layout.scale, AppearanceConfig.panels.getValue(hud.name.lowercase()))
    }
}
