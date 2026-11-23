package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

/** Modes own targets; these switches only control where their highlights appear. */
class SafariEspSettings {
    @JvmField @ConfigOption(name = "Critter nameplates", desc = "Choose names, distance and catch pity for highlighted critters.") @Accordion val nameplates=NameplateSettings()
    @JvmField @ConfigOption(name="Teammate appearance",desc="") @Accordion var teammates=TeammateAppearanceSettings(SafariSettings.SafariTeammateSettings())
    @JvmField @ConfigOption(name = "Custom critter names", desc = "Change displayed names without changing outgoing messages.") @Accordion val names=CritterNameSettings()
    @JvmField @ConfigOption(name="Biome critter colors",desc="") @Accordion val biomeColors=BiomeCritterColorSettings()
    @JvmField @ConfigOption(name = "Individual critter colors", desc = "Used for biomes without a biome color override.") @Accordion
    var critterColors = CritterAppearanceSettings { AppearanceConfig.selectedHex }
    var teammateColor = SafariEspConfig.teammateColor
    @JvmField @ConfigOption(name = "Floor drops", desc = "") @Accordion val floorDrops = FloorDropEspSettings()
    @JvmField @ConfigOption(name = "Structure and sparkling colors", desc = "") @Accordion
    var waypointColors = WaypointAppearanceSettings { AppearanceConfig.selectedHex }
    @JvmField @ConfigOption(name = "Other waypoint text size", desc = "Adjust timers, bee nests and unknown sparkling labels.") @Accordion val labels = EspLabelSettings()
    val biomes = EspBiomeSettings()
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
        val scale = SafariEspConfig.validTextScale(textScale)
        val colorsChanged = listOf(floorColors.apply(),biomeColors.apply(),names.apply(),nameplates.apply(),teammates.apply()).any {it}
        val sizesChanged = floorTextSizes.apply()
        val visibilityChanged = floorDrops.visibility.apply()
        var changed = colorsChanged || sizesChanged || visibilityChanged || SafariEspConfig.textScale != scale ||
            SafariEspConfig.rewardDrops != rewardDrops
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
    @JvmField @ConfigOption(name = "XP, essence and shard drops", desc = "Highlight reward drops on the ground.")
    @ConfigEditorBoolean var rewardDrops = SafariEspConfig.rewardDrops
    @JvmField @ConfigOption(name = "Show outside current biome", desc = "")
    @Accordion val visibility = FloorDropVisibilitySettings()
    @JvmField @ConfigOption(name = "Floor-drop text sizes", desc = "1 = original size.")
    @Accordion val floorTextSizes = FloorDropScaleSettings()
    @JvmField @ConfigOption(name = "Floor-drop colors", desc = "")
    @Accordion val floorColors = FloorDropColorSettings()

    internal fun rows(path: String): List<SettingsCatalog.Row> {
        val order=listOf("seeds","worm","berry","lime","purple","orange","icebreaker","incense","coin","generic")
        return order.map {key ->
            val type=net.johnceo.sparklingmutuals.safari.resources.FloorDropType.valueOf(key.uppercase())
            val color=floorColors.javaClass.getDeclaredField(key).apply {isAccessible=true}
            val scale=floorTextSizes.javaClass.getDeclaredField(key).apply {isAccessible=true}
            val itemPath=path+" / "+type.label
            SettingsCatalog.Row(floorColors,color,type.label,"",itemPath,listOf(
                SettingsCatalog.Row(floorColors,color,"Color","",itemPath+" / Color",navigationName="color"),
                SettingsCatalog.Row(floorTextSizes,scale,"Text size","1 = original size.",itemPath+" / Text size",navigationName="textScale")
            ),navigationName=key,groupOverride=true)
        }
    }
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
    @JvmField @ConfigOption(name = "XP / Essence / Shard", desc = "")
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
    @JvmField @ConfigOption(name = "Other waypoint text size", desc = "Timers, bee nests and unknown sparkling labels.")
    @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
    var textScale = SafariEspConfig.textScale
    var teammateTextScale = SafariEspConfig.teammateTextScale
}

class EspBiomeSettings {
    @JvmField @ConfigOption(name = "Forest critters only in Forest", desc = "")
    @ConfigEditorBoolean var forest = SafariEspConfig.groups.getValue("forest").onlyInBiome
    @JvmField @ConfigOption(name = "Cavern critters only in Cavern", desc = "")
    @ConfigEditorBoolean var cavern = SafariEspConfig.groups.getValue("cavern").onlyInBiome
    @JvmField @ConfigOption(name = "Icy critters only in Icy", desc = "")
    @ConfigEditorBoolean var icy = SafariEspConfig.groups.getValue("icy").onlyInBiome
    @JvmField @ConfigOption(name = "Haunted critters only in Haunted", desc = "")
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

class EspGeneralSettings(limits: EspBiomeSettings,val drops: FloorDropEspSettings) {
        @JvmField @ConfigOption(name = "Critter ESP", desc = "Highlight Safari critters and their related objectives.")
        @ConfigEditorBoolean var critterEsp = ConfigManager.critterEspEnabled
        @JvmField @ConfigOption(name = "Floor Drop ESP", desc = "Highlight uncollected Safari floor drops.")
        @ConfigEditorBoolean var floorDropEsp = ConfigManager.floorDropEspEnabled
    @JvmField @ConfigOption(name = "Critter glow outlines", desc = "Outline the critter's model. Can be used with boxes.") @ConfigEditorBoolean
    var glow=SafariEspConfig.critterGlow
    @JvmField @ConfigOption(name = "Critter boxes", desc = "Draw a box around each highlighted critter. Can be used with glow outlines.") @ConfigEditorBoolean
    var boxes=SafariEspConfig.critterBoxes
    @JvmField @ConfigOption(name = "Critter visibility by biome", desc = "Choose whether each biome's critters are shown only while you are in that biome.") @Accordion val biomes=limits
    @JvmField @ConfigOption(name = "Floor drops outside your biome", desc = "Enable an item to show its ESP from other biomes.") @Accordion val floorVisibility=drops.visibility
    fun apply(): Boolean {
        val changed=glow!=SafariEspConfig.critterGlow || boxes!=SafariEspConfig.critterBoxes
        SafariEspConfig.critterGlow=glow;SafariEspConfig.critterBoxes=boxes
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
