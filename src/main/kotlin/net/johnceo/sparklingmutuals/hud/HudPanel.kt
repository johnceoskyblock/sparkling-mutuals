package net.johnceo.sparklingmutuals.hud

import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.PanelStyle
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.ceil

data class HudIconCount(val icon: UiIcons.Kind,val count: Int,val color: Int)

/** Layout and progress tracks adapted from CritterMod v0.9.0 HudPanel (MIT). */
data class HudRow(val label: String = "", val value: String? = null, val color: Int = WHITE,
    val valueColor: Int = color, val fraction: Float? = null,
    val icon: UiIcons.Kind? = UiIcons.forLabel(label), val complete: Boolean? = null, val gems: List<Int> = emptyList(),
    val completedIcon: UiIcons.Kind = UiIcons.Kind.CHECK, val counters: List<HudIconCount> = emptyList()) {
    val displayIcon get() = when(complete) { true -> completedIcon; false -> UiIcons.Kind.CROSS; null -> icon }
    val protectedColor get() = label in fixedColors ||
        SafariRules.strip(label).trimStart('✓', '✕', '?', '×', ' ') in critterColors
    fun labelColor(style: PanelStyle)=if(!style.labels.enabled || protectedColor) color else style.labels.resolve(color)
    fun numberColor(style: PanelStyle)=if(!style.values.enabled || protectedColor) valueColor else style.values.resolve(valueColor)
    companion object {
        const val WHITE = -1
        val GRAY = 0xFFAAAAAA.toInt()
        val GOLD = 0xFF000000.toInt() or SafariTheme.ACCENT
        private val fixedColors = SafariBiome.entries.map { it.label }.toSet() + "Hotspot"
        private val critterColors = SafariRoster.all.map { it.name }.toSet()
    }
}
open class HudPanel(val title: String, val titleColor: Int = HudRow.GOLD, val rows: List<HudRow>,
    val titleIcon: UiIcons.Kind? = UiIcons.forLabel(title),private val widthOverride: Int?=null,private val heightOverride: Int?=null,
    val compactFooter: List<HudRow> = emptyList()) {
    protected class DrawMetrics(val font: Font, val title: String, val labelWidths: IntArray, val valueWidths: IntArray) {
        var width = 0
        var valueColumnWidth = 0
    }

    fun width(measure: (String) -> Int, style: PanelStyle? = null): Int =
        widthOverride ?: measureWidth(measure, style?.title(title) ?: title)

    private fun measureWidth(measure: (String) -> Int, heading: String, metrics: DrawMetrics? = null): Int {
        var labels: Int? = null
        var values: Int? = null
        var hasTrack = false
        rows.forEachIndexed { index, row ->
            val labelWidth = measure(row.label)
            metrics?.labelWidths?.set(index, labelWidth)
            if (widthOverride == null) {
                val fullLabelWidth = labelWidth + row.gems.size * 14 + row.counters.sumOf { 22 + measure(it.count.toString()) } +
                    if (row.icon != null || row.complete != null) 14 else 0
                labels = labels?.let { maxOf(it, fullLabelWidth) } ?: fullLabelWidth
                if (row.fraction != null) hasTrack = true
            }
            row.value?.let {
                val valueWidth = measure(it)
                metrics?.valueWidths?.set(index, valueWidth)
                values = values?.let { maxOf(it, valueWidth) } ?: valueWidth
            }
        }
        val valueWidth = values ?: 0
        metrics?.valueColumnWidth = valueWidth
        val footerWidth = if (compactFooter.isEmpty()) 0 else {
            val label = compactFooter.maxOf { measure(it.label) }
            val value = compactFooter.maxOf { measure(it.value.orEmpty()) }
            ceil((label + 8 + value) * FOOTER_SCALE).toInt()
        }
        return widthOverride ?: maxOf(
            measure(heading) + if (titleIcon != null) 14 else 0,
            (labels ?: 0) + (if (valueWidth > 0) valueWidth + 6 else 0) + if (hasTrack) 40 else 0,
            footerWidth
        ) + 12
    }

    private fun measureForDraw(style: PanelStyle): DrawMetrics {
        val size = rows.size
        val metrics = DrawMetrics(HudFonts.font, style.title(title), IntArray(size), IntArray(size))
        metrics.width = measureWidth(metrics.font::width, metrics.title, metrics)
        return metrics
    }

    val height get() = heightOverride ?: (12 + 11 * (1 + rows.size) +
        if (compactFooter.isEmpty()) 0 else 4 + ceil(compactFooter.size * 11 * FOOTER_SCALE).toInt())
    fun draw(graphics: GuiGraphicsExtractor, x: Int, y: Int, scale: Float = 1f,
        style: PanelStyle = AppearanceConfig.panels.getValue("captures"), borderOverride: Int? = null) {
        drawMeasured(graphics, x, y, scale, style, borderOverride, measureForDraw(style))
    }

    protected open fun drawMeasured(graphics: GuiGraphicsExtractor, x: Int, y: Int, scale: Float,
        style: PanelStyle, borderOverride: Int?, metrics: DrawMetrics) {
        val font = metrics.font
        val w = metrics.width
        val shadow = HudFonts.shadow
        graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(scale, scale)
        style.draw(graphics, w, height, borderOverride)
        titleIcon?.let { UiIcons.draw(graphics,6,6,it,titleColor) }
        graphics.text(font, metrics.title, if(titleIcon!=null) 20 else 6, 6, style.header.resolve(titleColor),shadow)
        rows.forEachIndexed { index, row ->
            val top = 17 + index * 11
            val icon = row.displayIcon
            val labelX = if (icon != null) 20 else 6
            val labelWidth = metrics.labelWidths[index]
            val protected = ((row.label.isNotEmpty() && style.labels.enabled) || (row.value != null && style.values.enabled)) &&
                row.protectedColor
            icon?.let { UiIcons.draw(graphics,6,top,it,if(row.complete==true) 0xFF55FF88.toInt() else if(row.complete==false) 0xFFFF6677.toInt() else row.color) }
            if (row.label.isNotEmpty())
                graphics.text(font, row.label, labelX, top, if (protected) row.color else style.labels.resolve(row.color), shadow)
            row.gems.forEachIndexed { i,tint -> UiIcons.draw(graphics,labelX+labelWidth+6+i*14,top,UiIcons.Kind.GEM,tint) }
            var counterX = labelX + labelWidth + if (row.label.isNotEmpty()) 8 else 0
            row.counters.forEach { counter ->
                val count = counter.count.toString()
                UiIcons.draw(graphics,counterX,top,counter.icon,counter.color)
                graphics.text(font,count,counterX+14,top,counter.color,shadow)
                counterX += 22 + font.width(count)
            }
            row.value?.let {
                graphics.text(font, it, w - 6 - metrics.valueWidths[index], top,
                    if (protected) row.valueColor else style.values.resolve(row.valueColor), shadow)
            }
            row.fraction?.let { fraction ->
                val left = w - 6 - metrics.valueColumnWidth - 6 - 34
                graphics.fill(left, top + 3, left + 34, top + 7, 0xFF555555.toInt())
                val filled = (34 * fraction.coerceIn(0f, 1f)).toInt()
                if (fraction > 0) graphics.fill(left, top + 3, left + filled.coerceAtLeast(1), top + 7, row.valueColor)
            }
        }
        if (compactFooter.isNotEmpty()) drawFooter(graphics, style, font, w, shadow)
        graphics.pose().popMatrix()
    }

    private fun drawFooter(graphics: GuiGraphicsExtractor, style: PanelStyle, font: Font, width: Int, shadow: Boolean) {
        val dividerY = 17 + rows.size * 11
        graphics.fill(6, dividerY, width - 6, dividerY + 1, 0x4D808080)
        graphics.pose().pushMatrix()
        graphics.pose().translate(6f, (dividerY + 5).toFloat())
        graphics.pose().scale(FOOTER_SCALE, FOOTER_SCALE)
        val right = ((width - 12) / FOOTER_SCALE).toInt()
        compactFooter.forEachIndexed { index, row ->
            val y = index * 11
            graphics.text(font, row.label, 0, y, row.labelColor(style), shadow)
            row.value?.let { graphics.text(font, it, right - font.width(it), y, row.numberColor(style), shadow) }
        }
        graphics.pose().popMatrix()
    }

    private companion object { const val FOOTER_SCALE = .8f }
    fun bounds(hud: SafariHud, screenWidth: Int, screenHeight: Int) =
        hud.layout.renderBounds(screenWidth, screenHeight, width(HudFonts.font::width,AppearanceConfig.panels[hud.name.lowercase()]), height)
    fun draw(graphics: GuiGraphicsExtractor, hud: SafariHud) {
        val window = Minecraft.getInstance().window
        val style = AppearanceConfig.panels.getValue(hud.name.lowercase())
        val metrics = measureForDraw(style)
        val bounds = hud.layout.renderBounds(window.guiScaledWidth, window.guiScaledHeight, metrics.width, height)
        val scale = hud.layout.renderScale(window.guiScaledWidth, window.guiScaledHeight, metrics.width, height)
        drawMeasured(graphics, bounds.x, bounds.y, scale, style, HudThemes.border(hud), metrics)
    }
}
