package net.johnceo.sparklingmutuals.config

import java.util.Properties
import kotlin.math.roundToInt
import net.minecraft.client.gui.GuiGraphicsExtractor

class HudTextColor(default: Int) {
    var enabled=false
    var color=AppearanceConfig.color(default)
    fun resolve(original: Int)=if(enabled) SafariEspConfig.rgb(color) else original
}

class PanelStyle {
    var headerText=""
    val header=HudTextColor(SafariTheme.ACCENT)
    val labels=HudTextColor(0xFFFFFF)
    val values=HudTextColor(0xFFFFFF)
    fun title(original: String)=headerText.ifBlank {original}
    var borderEnabled = true
    var borderColor = AppearanceConfig.color(SafariTheme.BORDER)
    var backgroundColor = AppearanceConfig.color(SafariTheme.BACKGROUND)
    var transparency = 20
        set(value) { field = value.coerceIn(0, 100) }
    val backgroundArgb get(): Int {
        val color = SafariEspConfig.rgb(backgroundColor)
        val alpha = ((color ushr 24) * (100 - transparency) / 100.0).roundToInt()
        return (alpha shl 24) or (color and 0xFFFFFF)
    }
    fun draw(graphics: GuiGraphicsExtractor, width: Int, height: Int, borderOverride: Int? = null) {
        if (backgroundArgb ushr 24 > 0) graphics.fill(0, 0, width, height, backgroundArgb)
        if (!borderEnabled) return
        val color = borderOverride ?: SafariEspConfig.rgb(borderColor)
        graphics.fill(0, 0, width, 1, color); graphics.fill(0, height - 1, width, height, color)
        graphics.fill(0, 0, 1, height, color); graphics.fill(width - 1, 0, width, height, color)
    }
}

object AppearanceConfig {
    var resourcePackFont = true
    var hudTextShadow = true
    var pixelAlignedHuds = false
    fun color(rgb: Int) = "0:255:${rgb shr 16 and 255}:${rgb shr 8 and 255}:${rgb and 255}"
    val panels = listOf("hotspot", "miria", "progress", "profitable", "lake_cleared", "missing", "objectives", "captures", "sparklings", "alert", "gems", "bird_food", "incense", "coins", "warp", "run_complete", "doom_throw", "entry_cooldown", "developer", "shard_guidance", "hunting_box_value").associateWith { PanelStyle() }
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
        net.johnceo.sparklingmutuals.safari.ui.CritterNames.load(properties)
        resourcePackFont = properties.getProperty("appearance.resourcePackFont")?.toBooleanStrictOrNull() ?: true
        hudTextShadow = properties.getProperty("appearance.hudTextShadow")?.toBooleanStrictOrNull() ?: true
        pixelAlignedHuds = properties.getProperty("appearance.pixelAlignedHuds")?.toBooleanStrictOrNull() ?: false
        selectedHex = properties.getProperty("appearance.selectedHex")?.takeIf { withHex(color(0), it) != null } ?: "#FFD700"
        panels.forEach { (id, style) ->
            // Seed the new panel from the existing Missing style, then save/edit it independently.
            fun saved(key: String) = properties.getProperty("appearance.$id.$key")
                ?: if (id in setOf("objectives", "profitable", "lake_cleared")) properties.getProperty("appearance.missing.$key") else null
            style.headerText=properties.getProperty("appearance.$id.headerText").orEmpty().replace(Regex("[\\p{Cc}\\p{Cf}]"),"").take(64)
            listOf("header" to style.header,"labels" to style.labels,"values" to style.values).forEach {(name,text)->
                text.enabled=properties.getProperty("appearance.$id.text.$name.enabled")?.toBooleanStrictOrNull() ?: false
                text.color=SafariEspConfig.validColor(properties.getProperty("appearance.$id.text.$name.color"),color(if(name=="header") SafariTheme.ACCENT else 0xFFFFFF))
            }
            style.borderEnabled = saved("borderEnabled")?.toBooleanStrictOrNull() ?: true
            val background = saved("backgroundColor")
            val legacyCompletion = id == "run_complete" && background == color(0xAAFFAA)
            // Upgrade exact old defaults only; user-picked colors, alpha and chroma remain intact.
            style.borderColor = migratedColor(saved("borderColor"), SafariTheme.BORDER, setOf(0xE3C77E))
            style.backgroundColor = migratedColor(background, SafariTheme.BACKGROUND,
                if (id == "run_complete") setOf(0x241F18, 0xAAFFAA) else setOf(0x241F18))
            val transparency = saved("transparency")?.toIntOrNull()
            style.transparency = if (legacyCompletion && transparency == 80) 20 else transparency ?: 20
        }
        waypointColors.forEach { (property, default) ->
            property.set(SafariEspConfig.validColor(properties.getProperty("appearance.${property.name}"), color(default)))
        }
    }
    private fun migratedColor(saved: String?, default: Int, legacy: Set<Int>): String =
        if (legacy.any { saved == color(it) }) color(default) else SafariEspConfig.validColor(saved, color(default))
    fun save(properties: Properties) {
        net.johnceo.sparklingmutuals.safari.ui.CritterNames.save(properties)
        properties.setProperty("appearance.resourcePackFont", resourcePackFont.toString())
        properties.setProperty("appearance.hudTextShadow", hudTextShadow.toString())
        properties.setProperty("appearance.pixelAlignedHuds", pixelAlignedHuds.toString())
        properties.setProperty("appearance.selectedHex", selectedHex)
        panels.forEach { (id, style) ->
            properties.setProperty("appearance.$id.headerText",style.headerText)
            listOf("header" to style.header,"labels" to style.labels,"values" to style.values).forEach {(name,text)->
                properties.setProperty("appearance.$id.text.$name.enabled",text.enabled.toString())
                properties.setProperty("appearance.$id.text.$name.color",text.color)
            }
            properties.setProperty("appearance.$id.borderEnabled", style.borderEnabled.toString())
            properties.setProperty("appearance.$id.borderColor", style.borderColor)
            properties.setProperty("appearance.$id.backgroundColor", style.backgroundColor)
            properties.setProperty("appearance.$id.transparency", style.transparency.toString())
        }
        waypointColors.forEach { (property, _) -> properties.setProperty("appearance.${property.name}", property.get()) }
    }
}
