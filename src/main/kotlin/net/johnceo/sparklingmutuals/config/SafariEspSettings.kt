package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.*

/** Modes own targets; these switches only control where their highlights appear. */
class SafariEspSettings {
    @JvmField @ConfigOption(name = "ESP text scale", desc = "Size of floor-drop names, timers, nest labels and teammate names. 1 = original size; default 2.")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var textScale = SafariEspConfig.textScale
    @JvmField @ConfigOption(name = "Floor-drop colors", desc = "Set each named drop's highlight and label color independently.")
    @Accordion val floorColors = FloorDropColorSettings()
    @JvmField @ConfigOption(name = "Floor drops: only in current biome", desc = "Limit floor highlights to your current biome.")
    @ConfigEditorBoolean var floor = SafariEspConfig.groups.getValue("floor").onlyInBiome
    @JvmField @ConfigOption(name = "Forest: only in current biome", desc = "Limit Forest critter highlights to Forest.")
    @ConfigEditorBoolean var forest = SafariEspConfig.groups.getValue("forest").onlyInBiome
    @JvmField @ConfigOption(name = "Cavern: only in current biome", desc = "Limit Cavern critter highlights to Cavern.")
    @ConfigEditorBoolean var cavern = SafariEspConfig.groups.getValue("cavern").onlyInBiome
    @JvmField @ConfigOption(name = "Icy: only in current biome", desc = "Limit Icy critter highlights to Icy.")
    @ConfigEditorBoolean var icy = SafariEspConfig.groups.getValue("icy").onlyInBiome
    @JvmField @ConfigOption(name = "Haunted: only in current biome", desc = "Limit Haunted critter highlights to Haunted.")
    @ConfigEditorBoolean var haunted = SafariEspConfig.groups.getValue("haunted").onlyInBiome
    fun apply(): Boolean {
        val scale = SafariEspConfig.validTextScale(textScale)
        var changed = floorColors.apply() || SafariEspConfig.textScale != scale
        SafariEspConfig.textScale = scale
        mapOf("floor" to floor, "forest" to forest, "cavern" to cavern, "icy" to icy, "haunted" to haunted).forEach { (key, value) ->
            val group = SafariEspConfig.groups.getValue(key)
            if (group.onlyInBiome != value) changed = true
            group.onlyInBiome = value
        }
        return changed
    }
}

class FloorDropColorSettings {
    fun reload() {
        javaClass.fields.filter { it.type == String::class.java }.forEach { field ->
            val type = net.johnceo.sparklingmutuals.safari.FloorDropType.valueOf(field.name.uppercase())
            field.set(this, SafariEspConfig.dropColor(type))
        }
    }
    @JvmField @ConfigOption(name = "Lime Gem", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var lime = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.LIME)
    @JvmField @ConfigOption(name = "Purple Gem", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var purple = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.PURPLE)
    @JvmField @ConfigOption(name = "Orange Gem", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var orange = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.ORANGE)
    @JvmField @ConfigOption(name = "Wriggleworm", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var worm = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.WORM)
    @JvmField @ConfigOption(name = "Yogi Berry", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var berry = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.BERRY)
    @JvmField @ConfigOption(name = "Bag of Seeds", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var seeds = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.SEEDS)
    @JvmField @ConfigOption(name = "Soothing Incense", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var incense = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.INCENSE)
    @JvmField @ConfigOption(name = "Shining Coin", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var coin = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.COIN)
    @JvmField @ConfigOption(name = "Icebreaker", desc = "Highlight, fill and label color.")
    @ConfigEditorColour var icebreaker = SafariEspConfig.dropColor(net.johnceo.sparklingmutuals.safari.FloorDropType.ICEBREAKER)
    fun apply(): Boolean {
        val values = mapOf(
            net.johnceo.sparklingmutuals.safari.FloorDropType.LIME to lime,
            net.johnceo.sparklingmutuals.safari.FloorDropType.PURPLE to purple,
            net.johnceo.sparklingmutuals.safari.FloorDropType.ORANGE to orange,
            net.johnceo.sparklingmutuals.safari.FloorDropType.WORM to worm,
            net.johnceo.sparklingmutuals.safari.FloorDropType.BERRY to berry,
            net.johnceo.sparklingmutuals.safari.FloorDropType.SEEDS to seeds,
            net.johnceo.sparklingmutuals.safari.FloorDropType.INCENSE to incense,
            net.johnceo.sparklingmutuals.safari.FloorDropType.COIN to coin,
            net.johnceo.sparklingmutuals.safari.FloorDropType.ICEBREAKER to icebreaker
        )
        var changed = false
        values.forEach { (type, color) ->
            val valid = SafariEspConfig.validColor(color, SafariEspConfig.dropColor(type))
            if (valid != SafariEspConfig.dropColor(type)) changed = true
            SafariEspConfig.floorColors[type] = valid
        }
        return changed
    }
}
