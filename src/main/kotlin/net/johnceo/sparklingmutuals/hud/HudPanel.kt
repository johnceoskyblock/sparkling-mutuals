package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.PanelStyle
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

data class HudIconCount(val icon: UiIcons.Kind,val count: Int,val color: Int)

/** Layout and progress tracks adapted from CritterMod v0.9.0 HudPanel (MIT). */
data class HudRow(val label: String = "", val value: String? = null, val color: Int = WHITE,
    val valueColor: Int = color, val fraction: Float? = null,
    val icon: UiIcons.Kind? = UiIcons.forLabel(label), val complete: Boolean? = null, val gems: List<Int> = emptyList(),
    val completedIcon: UiIcons.Kind = UiIcons.Kind.CHECK, val counters: List<HudIconCount> = emptyList()) {
    val displayIcon get() = when(complete) { true -> completedIcon; false -> UiIcons.Kind.CROSS; null -> icon }
    companion object {
        const val WHITE = -1
        val GRAY = 0xFFAAAAAA.toInt()
        val GOLD = 0xFF000000.toInt() or SafariTheme.ACCENT
    }
}
open class HudPanel(val title: String, val titleColor: Int = HudRow.GOLD, val rows: List<HudRow>,
    val titleIcon: UiIcons.Kind? = UiIcons.forLabel(title),private val widthOverride: Int?=null,private val heightOverride: Int?=null) {
    fun width(measure: (String) -> Int): Int {
        widthOverride?.let { return it }
        val labels = rows.maxOfOrNull { row -> measure(row.label) + row.gems.size*14 +
            row.counters.sumOf { 22+measure(it.count.toString()) } + (if(row.icon!=null || row.complete!=null) 14 else 0) } ?: 0
        val values = rows.mapNotNull { it.value }.maxOfOrNull(measure) ?: 0
        return maxOf(measure(title) + (if(titleIcon!=null) 14 else 0), labels + (if (values > 0) values + 6 else 0) +
            (if (rows.any { it.fraction != null }) 40 else 0)) + 12
    }
    val height get() = heightOverride ?: (12 + 11 * (1 + rows.size))
    open fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, scale: Float = 1f,
        style: PanelStyle = AppearanceConfig.panels.getValue("captures"), borderOverride: Int? = null) {
        val font = Minecraft.getInstance().font
        val w = width(font::width)
        val valueWidth = rows.mapNotNull { it.value }.maxOfOrNull(font::width) ?: 0
        graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(scale, scale)
        style.draw(graphics, w, height, borderOverride)
        titleIcon?.let { UiIcons.draw(graphics,6,6,it,titleColor) }
        graphics.text(font, title, if(titleIcon!=null) 20 else 6, 6, titleColor, true)
        rows.forEachIndexed { index, row ->
            val top = 17 + index * 11
            val icon=row.displayIcon
            icon?.let { UiIcons.draw(graphics,6,top,it,if(row.complete==true) 0xFF55FF88.toInt() else if(row.complete==false) 0xFFFF6677.toInt() else row.color) }
            if (row.label.isNotEmpty()) graphics.text(font,row.label,if(icon!=null) 20 else 6,top,row.color,true)
            row.gems.forEachIndexed { i,tint -> UiIcons.draw(graphics,(if(icon!=null) 20 else 6)+font.width(row.label)+6+i*14,top,UiIcons.Kind.GEM,tint) }
            var counterX=(if(icon!=null) 20 else 6)+font.width(row.label)+(if(row.label.isNotEmpty()) 8 else 0)
            row.counters.forEach { counter ->
                UiIcons.draw(graphics,counterX,top,counter.icon,counter.color)
                graphics.text(font,counter.count.toString(),counterX+14,top,counter.color,true)
                counterX+=22+font.width(counter.count.toString())
            }
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
        val scale = hud.layout.effectiveScale(window.guiScaledWidth, window.guiScaledHeight,
            width(Minecraft.getInstance().font::width), height)
        draw(graphics, bounds.x, bounds.y, scale, AppearanceConfig.panels.getValue(hud.name.lowercase()), HudThemes.border(hud))
    }
}
