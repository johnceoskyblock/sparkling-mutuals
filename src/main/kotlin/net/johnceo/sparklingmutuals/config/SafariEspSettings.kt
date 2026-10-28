package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.*
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

/** Modes own targets; these switches only control where their highlights appear. */
class SafariEspSettings {
    @JvmField @ConfigOption(name = "Party member highlight color", desc = "Outlines and name labels.")
    @ConfigEditorColour var teammateColor = SafariEspConfig.teammateColor
    @JvmField @ConfigOption(name = "Floor drops", desc = "") @Accordion val floorDrops = FloorDropEspSettings()
    @JvmField @ConfigOption(name = "Other text sizes", desc = "") @Accordion val labels = EspLabelSettings()
    @JvmField @ConfigOption(name = "Critter biome limits", desc = "") @Accordion val biomes = EspBiomeSettings()
    var rewardDrops
        get() = floorDrops.rewardDrops
        set(value) { floorDrops.rewardDrops = value }
    val floorTextSizes get() = floorDrops.floorTextSizes
    val floorColors get() = floorDrops.floorColors
    var textScale
        get() = labels.textScale
        set(value) { labels.textScale = value }
    var teammateTextScale
        get() = labels.teammateTextScale
        set(value) { labels.teammateTextScale = value }
    var forest
        get() = biomes.forest
        set(value) { biomes.forest = value }
    var cavern
        get() = biomes.cavern
        set(value) { biomes.cavern = value }
    var icy
        get() = biomes.icy
        set(value) { biomes.icy = value }
    var haunted
        get() = biomes.haunted
        set(value) { biomes.haunted = value }
    fun apply(): Boolean {
        val color = SafariEspConfig.validColor(teammateColor, SafariEspConfig.teammateColor)
        val teammateColorChanged = color != SafariEspConfig.teammateColor
        SafariEspConfig.teammateColor = color
        val scale = SafariEspConfig.validTextScale(textScale)
        val colorsChanged = floorColors.apply()
        val sizesChanged = floorTextSizes.apply()
        val visibilityChanged = floorDrops.visibility.apply()
        val teammateScale = SafariEspConfig.validTextScale(teammateTextScale)
        var changed = teammateColorChanged || colorsChanged || sizesChanged || visibilityChanged || SafariEspConfig.textScale != scale ||
            SafariEspConfig.teammateTextScale != teammateScale || SafariEspConfig.rewardDrops != rewardDrops
        SafariEspConfig.teammateTextScale = teammateScale
        SafariEspConfig.rewardDrops = rewardDrops
        SafariEspConfig.textScale = scale
        mapOf("forest" to forest, "cavern" to cavern, "icy" to icy, "haunted" to haunted).forEach { (key, value) ->
            val group = SafariEspConfig.groups.getValue(key)
            if (group.onlyInBiome != value) changed = true
            group.onlyInBiome = value
        }
        return changed
    }
}

class FloorDropEspSettings {
    @JvmField @ConfigOption(name = "XP / Essence / Shard floor drops", desc = "Highlight reward floor drops.")
    @ConfigEditorBoolean var rewardDrops = SafariEspConfig.rewardDrops
    @JvmField @ConfigOption(name = "Show outside current biome", desc = "Mode and collection rules still apply.")
    @Accordion val visibility = FloorDropVisibilitySettings()
    @JvmField @ConfigOption(name = "Floor-drop text sizes", desc = "1 = original size.")
    @Accordion val floorTextSizes = FloorDropScaleSettings()
    @JvmField @ConfigOption(name = "Floor-drop colors", desc = "Outlines, fills and labels.")
    @Accordion val floorColors = FloorDropColorSettings()
}

class FloorDropVisibilitySettings {
    @JvmField @ConfigOption(name = "Bag of Seeds", desc = "")
    @ConfigEditorBoolean var seeds = !SafariEspConfig.dropOnlyInBiome(FloorDropType.SEEDS)
    @JvmField @ConfigOption(name = "Wriggleworm", desc = "")
    @ConfigEditorBoolean var worm = !SafariEspConfig.dropOnlyInBiome(FloorDropType.WORM)
    @JvmField @ConfigOption(name = "Yogi Berry", desc = "")
    @ConfigEditorBoolean var berry = !SafariEspConfig.dropOnlyInBiome(FloorDropType.BERRY)
    @JvmField @ConfigOption(name = "Lime Gem", desc = "")
    @ConfigEditorBoolean var lime = !SafariEspConfig.dropOnlyInBiome(FloorDropType.LIME)
    @JvmField @ConfigOption(name = "Purple Gem", desc = "")
    @ConfigEditorBoolean var purple = !SafariEspConfig.dropOnlyInBiome(FloorDropType.PURPLE)
    @JvmField @ConfigOption(name = "Orange Gem", desc = "")
    @ConfigEditorBoolean var orange = !SafariEspConfig.dropOnlyInBiome(FloorDropType.ORANGE)
    @JvmField @ConfigOption(name = "Icebreaker", desc = "")
    @ConfigEditorBoolean var icebreaker = !SafariEspConfig.dropOnlyInBiome(FloorDropType.ICEBREAKER)
    @JvmField @ConfigOption(name = "Soothing Incense", desc = "")
    @ConfigEditorBoolean var incense = !SafariEspConfig.dropOnlyInBiome(FloorDropType.INCENSE)
    @JvmField @ConfigOption(name = "Shining Coin", desc = "")
    @ConfigEditorBoolean var coin = !SafariEspConfig.dropOnlyInBiome(FloorDropType.COIN)
    @JvmField @ConfigOption(name = "XP / Essence / Shard", desc = "Requires reward floor-drop ESP.")
    @ConfigEditorBoolean var generic = !SafariEspConfig.dropOnlyInBiome(FloorDropType.GENERIC)
    fun apply(): Boolean {
        val values = mapOf(FloorDropType.SEEDS to seeds, FloorDropType.WORM to worm, FloorDropType.BERRY to berry,
            FloorDropType.LIME to lime, FloorDropType.PURPLE to purple, FloorDropType.ORANGE to orange,
            FloorDropType.ICEBREAKER to icebreaker, FloorDropType.INCENSE to incense,
            FloorDropType.COIN to coin, FloorDropType.GENERIC to generic)
        var changed = false
        values.forEach { (type, outside) ->
            val onlyInBiome = !outside
            if (SafariEspConfig.dropOnlyInBiome(type) != onlyInBiome) changed = true
            SafariEspConfig.floorOnlyInBiome[type] = onlyInBiome
        }
        return changed
    }
}

class EspLabelSettings {
    @JvmField @ConfigOption(name = "Other ESP text scale", desc = "Critter timers, nests and unknown sparklings.")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var textScale = SafariEspConfig.textScale
    @JvmField @ConfigOption(name = "Party member text scale", desc = "1 = original size.")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var teammateTextScale = SafariEspConfig.teammateTextScale
}

class EspBiomeSettings {
    @JvmField @ConfigOption(name = "Forest: only in current biome", desc = "")
    @ConfigEditorBoolean var forest = SafariEspConfig.groups.getValue("forest").onlyInBiome
    @JvmField @ConfigOption(name = "Cavern: only in current biome", desc = "")
    @ConfigEditorBoolean var cavern = SafariEspConfig.groups.getValue("cavern").onlyInBiome
    @JvmField @ConfigOption(name = "Icy: only in current biome", desc = "")
    @ConfigEditorBoolean var icy = SafariEspConfig.groups.getValue("icy").onlyInBiome
    @JvmField @ConfigOption(name = "Haunted: only in current biome", desc = "")
    @ConfigEditorBoolean var haunted = SafariEspConfig.groups.getValue("haunted").onlyInBiome
}

class FloorDropColorSettings {
    fun reload() {
        javaClass.fields.filter { it.type == String::class.java }.forEach { field ->
            val type = FloorDropType.valueOf(field.name.uppercase())
            field.set(this, SafariEspConfig.dropColor(type))
        }
    }
    @JvmField @ConfigOption(name = "Lime Gem", desc = "")
    @ConfigEditorColour var lime = SafariEspConfig.dropColor(FloorDropType.LIME)
    @JvmField @ConfigOption(name = "Purple Gem", desc = "")
    @ConfigEditorColour var purple = SafariEspConfig.dropColor(FloorDropType.PURPLE)
    @JvmField @ConfigOption(name = "Orange Gem", desc = "")
    @ConfigEditorColour var orange = SafariEspConfig.dropColor(FloorDropType.ORANGE)
    @JvmField @ConfigOption(name = "Wriggleworm", desc = "")
    @ConfigEditorColour var worm = SafariEspConfig.dropColor(FloorDropType.WORM)
    @JvmField @ConfigOption(name = "Yogi Berry", desc = "")
    @ConfigEditorColour var berry = SafariEspConfig.dropColor(FloorDropType.BERRY)
    @JvmField @ConfigOption(name = "Bag of Seeds", desc = "")
    @ConfigEditorColour var seeds = SafariEspConfig.dropColor(FloorDropType.SEEDS)
    @JvmField @ConfigOption(name = "Soothing Incense", desc = "")
    @ConfigEditorColour var incense = SafariEspConfig.dropColor(FloorDropType.INCENSE)
    @JvmField @ConfigOption(name = "Shining Coin", desc = "")
    @ConfigEditorColour var coin = SafariEspConfig.dropColor(FloorDropType.COIN)
    @JvmField @ConfigOption(name = "Icebreaker", desc = "")
    @ConfigEditorColour var icebreaker = SafariEspConfig.dropColor(FloorDropType.ICEBREAKER)
    @JvmField @ConfigOption(name = "XP / Essence / Shard", desc = "")
    @ConfigEditorColour var generic = SafariEspConfig.dropColor(FloorDropType.GENERIC)
    fun apply(): Boolean {
        val values = mapOf(
            FloorDropType.GENERIC to generic,
            FloorDropType.LIME to lime,
            FloorDropType.PURPLE to purple,
            FloorDropType.ORANGE to orange,
            FloorDropType.WORM to worm,
            FloorDropType.BERRY to berry,
            FloorDropType.SEEDS to seeds,
            FloorDropType.INCENSE to incense,
            FloorDropType.COIN to coin,
            FloorDropType.ICEBREAKER to icebreaker
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

class FloorDropScaleSettings {
    @JvmField @ConfigOption(name = "Lime Gem", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var lime = SafariEspConfig.dropTextScale(FloorDropType.LIME)
    @JvmField @ConfigOption(name = "Purple Gem", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var purple = SafariEspConfig.dropTextScale(FloorDropType.PURPLE)
    @JvmField @ConfigOption(name = "Orange Gem", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var orange = SafariEspConfig.dropTextScale(FloorDropType.ORANGE)
    @JvmField @ConfigOption(name = "Wriggleworm", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var worm = SafariEspConfig.dropTextScale(FloorDropType.WORM)
    @JvmField @ConfigOption(name = "Yogi Berry", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var berry = SafariEspConfig.dropTextScale(FloorDropType.BERRY)
    @JvmField @ConfigOption(name = "Bag of Seeds", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var seeds = SafariEspConfig.dropTextScale(FloorDropType.SEEDS)
    @JvmField @ConfigOption(name = "Soothing Incense", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var incense = SafariEspConfig.dropTextScale(FloorDropType.INCENSE)
    @JvmField @ConfigOption(name = "Shining Coin", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var coin = SafariEspConfig.dropTextScale(FloorDropType.COIN)
    @JvmField @ConfigOption(name = "Icebreaker", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var icebreaker = SafariEspConfig.dropTextScale(FloorDropType.ICEBREAKER)
    @JvmField @ConfigOption(name = "XP / Essence / Shard", desc = "")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var generic = SafariEspConfig.dropTextScale(FloorDropType.GENERIC)
    fun apply(): Boolean {
        val values = mapOf(
            FloorDropType.LIME to lime,
            FloorDropType.PURPLE to purple,
            FloorDropType.ORANGE to orange,
            FloorDropType.WORM to worm,
            FloorDropType.BERRY to berry,
            FloorDropType.SEEDS to seeds,
            FloorDropType.INCENSE to incense,
            FloorDropType.COIN to coin,
            FloorDropType.ICEBREAKER to icebreaker,
            FloorDropType.GENERIC to generic
        )
        var changed = false
        values.forEach { (type, scale) ->
            val valid = SafariEspConfig.validTextScale(scale)
            if (SafariEspConfig.dropTextScale(type) != valid) changed = true
            SafariEspConfig.floorTextScales[type] = valid
        }
        return changed
    }
}
