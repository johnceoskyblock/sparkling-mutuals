package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

/** Modes own targets; these switches only control where their highlights appear. */
class SafariEspSettings {
    @JvmField @ConfigOption(name = "Custom critter names", desc = "Change displayed names without changing outgoing messages.") @Accordion val names=CritterNameSettings()
    @JvmField @ConfigOption(name="Biome critter colors",desc="") @Accordion val biomeColors=BiomeCritterColorSettings()
    @JvmField @ConfigOption(name = "Individual critter colors", desc = "Used for biomes without a biome color override.") @Accordion
    var critterColors = CritterAppearanceSettings { AppearanceConfig.selectedHex }
    @JvmField @ConfigOption(name = "Floor drops", desc = "") @Accordion val floorDrops = FloorDropEspSettings()
    @JvmField @ConfigOption(name = "Structure and sparkling colors", desc = "") @Accordion
    var waypointColors = WaypointAppearanceSettings { AppearanceConfig.selectedHex }
    @JvmField @ConfigOption(name = "Structure label size", desc = "Adjust bee nest and Snoozle wall labels.") @Accordion val labels = EspLabelSettings()
    var textScale
        get() = labels.textScale
        set(value) { labels.textScale = value }
    fun apply(): Boolean {
        val scale = SafariEspConfig.validTextScale(textScale)
        val colorsChanged = listOf(floorDrops.apply(), biomeColors.apply(), names.apply()).any { it }
        val changed = colorsChanged || SafariEspConfig.textScale != scale
        SafariEspConfig.textScale = scale
        return changed
    }
}

class FloorDropEspSettings {
    @JvmField @ConfigOption(name = "Highlight color", desc = "Color of the floor tile around any visible drop.")
    @ConfigEditorColour var generic = SafariEspConfig.dropColor(FloorDropType.GENERIC)

    internal fun rows(path: String): List<SettingsCatalog.Row> {
        val field = javaClass.getField("generic")
        val option = field.getAnnotation(ConfigOption::class.java)
        return listOf(SettingsCatalog.Row(this, field, option.name, option.desc, "$path / ${option.name}"))
    }

    fun apply(): Boolean {
        val previous = SafariEspConfig.dropColor(FloorDropType.GENERIC)
        val color = SafariEspConfig.validColor(generic, previous)
        SafariEspConfig.floorColors[FloorDropType.GENERIC] = color
        return previous != color
    }
}

class EspLabelSettings {
    @JvmField @ConfigOption(name = "Structure label size", desc = "Adjust bee nest and Snoozle wall labels.")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var textScale = SafariEspConfig.textScale
}

class EspGeneralSettings(val drops: FloorDropEspSettings) {
    @JvmField @ConfigOption(name = "Critter highlights", desc = "Mark critters and objectives in view.")
    @ConfigEditorBoolean var critterEsp = ConfigManager.critterEspEnabled
    @JvmField @ConfigOption(name = "Glow highlights", desc = "Outline clearly visible critter models that support glow.")
    @ConfigEditorBoolean var glow = SafariEspConfig.critterGlow
    @JvmField @ConfigOption(name = "Box highlights", desc = "Draw boxes around visible critters.")
    @ConfigEditorBoolean var boxes = SafariEspConfig.critterBoxes
    @JvmField @ConfigOption(name = "Floor drop highlights", desc = "Draw a colored tile at floor drops you can see in your biome.")
    @ConfigEditorBoolean var floorDropEsp = ConfigManager.floorDropEspEnabled

    fun apply(): Boolean {
        val changed = SafariEspConfig.critterGlow != glow || SafariEspConfig.critterBoxes != boxes
        SafariEspConfig.critterGlow = glow
        SafariEspConfig.critterBoxes = boxes
        return changed
    }
}

class BiomeCritterColorSettings {
    @JvmField @ConfigOption(name = "Use biome color overrides", desc = "Use one color for each biome enabled below.") @ConfigEditorBoolean
    var enabled=SafariEspConfig.biomeCritterColors
    @JvmField @ConfigOption(name = "Override Forest colors", desc = "Turn off to use individual critter colors.") @ConfigEditorBoolean var forestEnabled=SafariEspConfig.biomeColorEnabled.getValue("forest")
    @JvmField @ConfigOption(name="Forest color",desc="") @ConfigEditorColour var forest=SafariEspConfig.biomeColors.getValue("forest")
    @JvmField @ConfigOption(name = "Override Cavern colors", desc = "Turn off to use individual critter colors.") @ConfigEditorBoolean var cavernEnabled=SafariEspConfig.biomeColorEnabled.getValue("cavern")
    @JvmField @ConfigOption(name="Cavern color",desc="") @ConfigEditorColour var cavern=SafariEspConfig.biomeColors.getValue("cavern")
    @JvmField @ConfigOption(name = "Override Icy colors", desc = "Turn off to use individual critter colors.") @ConfigEditorBoolean var icyEnabled=SafariEspConfig.biomeColorEnabled.getValue("icy")
    @JvmField @ConfigOption(name="Icy color",desc="") @ConfigEditorColour var icy=SafariEspConfig.biomeColors.getValue("icy")
    @JvmField @ConfigOption(name = "Override Haunted colors", desc = "Turn off to use individual critter colors.") @ConfigEditorBoolean var hauntedEnabled=SafariEspConfig.biomeColorEnabled.getValue("haunted")
    @JvmField @ConfigOption(name="Haunted color",desc="") @ConfigEditorColour var haunted=SafariEspConfig.biomeColors.getValue("haunted")
    fun apply(): Boolean {
        var changed=enabled!=SafariEspConfig.biomeCritterColors
        SafariEspConfig.biomeCritterColors=enabled
        mapOf("forest" to forestEnabled,"cavern" to cavernEnabled,"icy" to icyEnabled,"haunted" to hauntedEnabled).forEach {(id,on)->
            if(SafariEspConfig.biomeColorEnabled[id]!=on)changed=true
            SafariEspConfig.biomeColorEnabled[id]=on
        }
        mapOf("forest" to forest,"cavern" to cavern,"icy" to icy,"haunted" to haunted).forEach {(id,color)->
            val valid=SafariEspConfig.validColor(color,SafariEspConfig.biomeColors.getValue(id))
            if(valid!=SafariEspConfig.biomeColors[id])changed=true
            SafariEspConfig.biomeColors[id]=valid
        }
        return changed
    }
}
