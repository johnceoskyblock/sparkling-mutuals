package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.Properties
import kotlin.math.roundToInt

class PanelStyle {
    var borderEnabled = false
    var borderColor = AppearanceConfig.color(SafariTheme.ACCENT)
    var backgroundColor = AppearanceConfig.color(SafariTheme.BACKGROUND)
    var transparency = 20
        set(value) { field = value.coerceIn(0, 100) }
    val backgroundArgb get() = (((100 - transparency) * 2.55).roundToInt() shl 24) or (SafariEspConfig.rgb(backgroundColor) and 0xFFFFFF)
    fun draw(graphics: GuiGraphicsExtractor, width: Int, height: Int) {
        graphics.fill(0, 0, width, height, backgroundArgb)
        if (!borderEnabled) return
        val color = SafariEspConfig.rgb(borderColor)
        graphics.fill(0, 0, width, 1, color); graphics.fill(0, height - 1, width, height, color)
        graphics.fill(0, 0, 1, height, color); graphics.fill(width - 1, 0, width, height, color)
    }
}

object AppearanceConfig {
    fun color(rgb: Int) = ChromaColour.special(0, 255, rgb)
    val panels = listOf("miria", "progress", "missing", "captures", "sparklings", "alert", "gems", "bird_food", "incense", "warp").associateWith { PanelStyle() }
    var selectedHex = "#FFD700"
    var nestColor = color(0x55FF55)
    var snooperColor = color(0xFFAA00)
    var sparklingColor = color(0xFFD700)
    private val waypointColors = listOf(::nestColor to 0x55FF55, ::snooperColor to 0xFFAA00, ::sparklingColor to 0xFFD700)
    fun withHex(current: String, hex: String): String? {
        val value = hex.trim().removePrefix("#")
        if (!value.matches(Regex("[0-9a-fA-F]{6}"))) return null
        val rgb = value.toInt(16)
        return current.split(':').take(2).joinToString(":") + ":${rgb shr 16}:${rgb shr 8 and 255}:${rgb and 255}"
    }
    fun load(properties: Properties) {
        selectedHex = properties.getProperty("appearance.selectedHex")?.takeIf { withHex(color(0), it) != null } ?: "#FFD700"
        panels.forEach { (id, style) ->
            style.borderEnabled = properties.getProperty("appearance.$id.borderEnabled")?.toBooleanStrictOrNull() ?: false
            style.borderColor = SafariEspConfig.validColor(properties.getProperty("appearance.$id.borderColor"), color(SafariTheme.ACCENT))
            style.backgroundColor = SafariEspConfig.validColor(properties.getProperty("appearance.$id.backgroundColor"), color(SafariTheme.BACKGROUND))
            style.transparency = properties.getProperty("appearance.$id.transparency")?.toIntOrNull() ?: 20
        }
        waypointColors.forEach { (property, default) ->
            property.set(SafariEspConfig.validColor(properties.getProperty("appearance.${property.name}"), color(default)))
        }
    }
    fun save(properties: Properties) {
        properties.setProperty("appearance.selectedHex", selectedHex)
        panels.forEach { (id, style) ->
            properties.setProperty("appearance.$id.borderEnabled", style.borderEnabled.toString())
            properties.setProperty("appearance.$id.borderColor", style.borderColor)
            properties.setProperty("appearance.$id.backgroundColor", style.backgroundColor)
            properties.setProperty("appearance.$id.transparency", style.transparency.toString())
        }
        waypointColors.forEach { (property, _) -> properties.setProperty("appearance.${property.name}", property.get()) }
    }
}
