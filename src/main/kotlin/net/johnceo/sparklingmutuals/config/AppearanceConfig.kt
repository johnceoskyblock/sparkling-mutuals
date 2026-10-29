package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.ChromaColour
import java.util.Properties
import kotlin.math.roundToInt
import net.minecraft.client.gui.GuiGraphicsExtractor

class PanelStyle {
    var borderEnabled = true
    var borderColor = AppearanceConfig.color(SafariTheme.BORDER)
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
    val panels = listOf("miria", "progress", "missing", "captures", "sparklings", "alert", "gems", "bird_food", "incense", "warp", "run_complete", "doom_throw").associateWith { PanelStyle() }
    var runCompleteColor = color(0x55FF55)
    var selectedHex = "#FFD700"
    var nestColor = color(0x55FF55)
    var snooperColor = color(0xFFAA00)
    var sparklingColor = color(0xFFD700)
    private val waypointColors = listOf(::nestColor to 0x55FF55, ::snooperColor to 0xFFAA00, ::sparklingColor to 0xFFD700, ::runCompleteColor to 0x55FF55)
    fun withHex(current: String, hex: String): String? {
        val value = hex.trim().removePrefix("#")
        if (!value.matches(Regex("[0-9a-fA-F]{6}"))) return null
        val rgb = value.toInt(16)
        return current.split(':').take(2).joinToString(":") + ":${rgb shr 16}:${rgb shr 8 and 255}:${rgb and 255}"
    }
    fun load(properties: Properties) {
        selectedHex = properties.getProperty("appearance.selectedHex")?.takeIf { withHex(color(0), it) != null } ?: "#FFD700"
        panels.forEach { (id, style) ->
            style.borderEnabled = properties.getProperty("appearance.$id.borderEnabled")?.toBooleanStrictOrNull() ?: true
            val background = properties.getProperty("appearance.$id.backgroundColor")
            val legacyCompletion = id == "run_complete" && background == color(0xAAFFAA)
            // Upgrade exact old defaults only; user-picked colors, alpha and chroma remain intact.
            style.borderColor = migratedColor(properties.getProperty("appearance.$id.borderColor"), SafariTheme.BORDER, setOf(0xE3C77E))
            style.backgroundColor = migratedColor(background, SafariTheme.BACKGROUND,
                if (id == "run_complete") setOf(0x241F18, 0xAAFFAA) else setOf(0x241F18))
            val transparency = properties.getProperty("appearance.$id.transparency")?.toIntOrNull()
            style.transparency = if (legacyCompletion && transparency == 80) 20 else transparency ?: 20
        }
        waypointColors.forEach { (property, default) ->
            property.set(SafariEspConfig.validColor(properties.getProperty("appearance.${property.name}"), color(default)))
        }
    }
    private fun migratedColor(saved: String?, default: Int, legacy: Set<Int>): String =
        if (legacy.any { saved == color(it) }) color(default) else SafariEspConfig.validColor(saved, color(default))
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
