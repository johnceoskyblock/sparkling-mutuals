package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.*

/** Shared controls keep the four biome categories consistent. */
open class SafariEspGroupSettings(private val group: String) {
    @JvmField @ConfigOption(name = "Enable ESP", desc = "Show through-terrain boxes for this group. Off by default.")
    @ConfigEditorBoolean var enabled = SafariEspConfig.groups.getValue(group).enabled
    @JvmField @ConfigOption(name = "Only in current biome", desc = "Hide this group unless you are in the same biome. Off: show across Safari.")
    @ConfigEditorBoolean var onlyInBiome = SafariEspConfig.groups.getValue(group).onlyInBiome
    fun apply(): Boolean {
        val saved = SafariEspConfig.groups.getValue(group)
        var changed = saved.enabled != enabled || saved.onlyInBiome != onlyInBiome
        saved.enabled = enabled; saved.onlyInBiome = onlyInBiome
        javaClass.fields.filter { it.type == SafariEspMobSettings::class.java }.forEach {
            if ((it.get(this) as SafariEspMobSettings).apply()) changed = true
        }
        return changed
    }
}
class SafariEspMobSettings(private val name: String) {
    @JvmField @ConfigOption(name = "Highlight", desc = "Draw this critter's box when its biome ESP is enabled.")
    @ConfigEditorBoolean var enabled = SafariEspConfig.mobs.getValue(name).enabled
    @JvmField @ConfigOption(name = "Color", desc = "Color of this critter's through-terrain box.")
    @ConfigEditorColour var color = SafariEspConfig.mobs.getValue(name).color
    fun apply(): Boolean {
        val saved = SafariEspConfig.mobs.getValue(name)
        val changed = saved.enabled != enabled || saved.color != color
        saved.enabled = enabled; saved.color = SafariEspConfig.validColor(color, saved.color)
        return changed
    }
}
class SafariFloorEspSettings : SafariEspGroupSettings("floor") {
    @JvmField @ConfigOption(name = "Color", desc = "Color of the floor drop tile. Three or more string displays identify a drop.")
    @ConfigEditorColour var color = SafariEspConfig.floorColor
    fun applyFloor(): Boolean {
        val changed = SafariEspConfig.floorColor != color
        SafariEspConfig.floorColor = SafariEspConfig.validColor(color, SafariEspConfig.floorColor)
        return apply() || changed
    }
}

class SafariCavernEspSettings : SafariEspGroupSettings("cavern") {
    @JvmField @ConfigOption(name = "Cavernfish", desc = "Highlight and color") @Accordion val cavernfish = SafariEspMobSettings("Cavernfish")
    @JvmField @ConfigOption(name = "Flitter", desc = "Highlight and color") @Accordion val flitter = SafariEspMobSettings("Flitter")
    @JvmField @ConfigOption(name = "Shyworm", desc = "Highlight and color") @Accordion val shyworm = SafariEspMobSettings("Shyworm")
    @JvmField @ConfigOption(name = "Driftling", desc = "Highlight and color") @Accordion val driftling = SafariEspMobSettings("Driftling")
    @JvmField @ConfigOption(name = "Chuckwalla", desc = "Highlight and color") @Accordion val chuckwalla = SafariEspMobSettings("Chuckwalla")
    @JvmField @ConfigOption(name = "Rockmite", desc = "Highlight and color") @Accordion val rockmite = SafariEspMobSettings("Rockmite")
    @JvmField @ConfigOption(name = "Scrappy", desc = "Highlight and color") @Accordion val scrappy = SafariEspMobSettings("Scrappy")
    @JvmField @ConfigOption(name = "Snoozle", desc = "Highlight and color") @Accordion val snoozle = SafariEspMobSettings("Snoozle")
    @JvmField @ConfigOption(name = "Gemzie", desc = "Highlight and color") @Accordion val gemzie = SafariEspMobSettings("Gemzie")
}

class SafariForestEspSettings : SafariEspGroupSettings("forest") {
    @JvmField @ConfigOption(name = "Foxtrot", desc = "Highlight and color") @Accordion val foxtrot = SafariEspMobSettings("Foxtrot")
    @JvmField @ConfigOption(name = "Bluebird", desc = "Highlight and color") @Accordion val bluebird = SafariEspMobSettings("Bluebird")
    @JvmField @ConfigOption(name = "Honeybug", desc = "Highlight and color") @Accordion val honeybug = SafariEspMobSettings("Honeybug")
    @JvmField @ConfigOption(name = "Treefrog", desc = "Highlight and color") @Accordion val treefrog = SafariEspMobSettings("Treefrog")
    @JvmField @ConfigOption(name = "Woodchucker", desc = "Highlight and color") @Accordion val woodchucker = SafariEspMobSettings("Woodchucker")
    @JvmField @ConfigOption(name = "Fluffling", desc = "Highlight and color") @Accordion val fluffling = SafariEspMobSettings("Fluffling")
    @JvmField @ConfigOption(name = "Hideonfloor", desc = "Highlight and color") @Accordion val hideonfloor = SafariEspMobSettings("Hideonfloor")
    @JvmField @ConfigOption(name = "Parakeet", desc = "Highlight and color") @Accordion val parakeet = SafariEspMobSettings("Parakeet")
    @JvmField @ConfigOption(name = "Macaw", desc = "Highlight and color") @Accordion val macaw = SafariEspMobSettings("Macaw")
}

class SafariIcyEspSettings : SafariEspGroupSettings("icy") {
    @JvmField @ConfigOption(name = "Strongarm", desc = "Highlight and color") @Accordion val strongarm = SafariEspMobSettings("Strongarm")
    @JvmField @ConfigOption(name = "Tepid", desc = "Highlight and color") @Accordion val tepid = SafariEspMobSettings("Tepid")
    @JvmField @ConfigOption(name = "Polaris", desc = "Highlight and color") @Accordion val polaris = SafariEspMobSettings("Polaris")
    @JvmField @ConfigOption(name = "Shuddersquid", desc = "Highlight and color") @Accordion val shuddersquid = SafariEspMobSettings("Shuddersquid")
    @JvmField @ConfigOption(name = "Billygoat", desc = "Highlight and color") @Accordion val billygoat = SafariEspMobSettings("Billygoat")
    @JvmField @ConfigOption(name = "Mantis Shrimp", desc = "Highlight and color") @Accordion val mantisshrimp = SafariEspMobSettings("Mantis Shrimp")
    @JvmField @ConfigOption(name = "Nozzlenose", desc = "Highlight and color") @Accordion val nozzlenose = SafariEspMobSettings("Nozzlenose")
    @JvmField @ConfigOption(name = "Troodon", desc = "Highlight and color") @Accordion val troodon = SafariEspMobSettings("Troodon")
    @JvmField @ConfigOption(name = "Wumpa", desc = "Highlight and color") @Accordion val wumpa = SafariEspMobSettings("Wumpa")
}

class SafariHauntedEspSettings : SafariEspGroupSettings("haunted") {
    @JvmField @ConfigOption(name = "Areita", desc = "Highlight and color") @Accordion val areita = SafariEspMobSettings("Areita")
    @JvmField @ConfigOption(name = "Bloodbat", desc = "Highlight and color") @Accordion val bloodbat = SafariEspMobSettings("Bloodbat")
    @JvmField @ConfigOption(name = "Duplico", desc = "Highlight and color") @Accordion val duplico = SafariEspMobSettings("Duplico")
    @JvmField @ConfigOption(name = "Gazer", desc = "Highlight and color") @Accordion val gazer = SafariEspMobSettings("Gazer")
    @JvmField @ConfigOption(name = "Litterbug", desc = "Highlight and color") @Accordion val litterbug = SafariEspMobSettings("Litterbug")
    @JvmField @ConfigOption(name = "Solsnatcher", desc = "Highlight and color") @Accordion val solsnatcher = SafariEspMobSettings("Solsnatcher")
    @JvmField @ConfigOption(name = "Gimmiegold", desc = "Highlight and color") @Accordion val gimmiegold = SafariEspMobSettings("Gimmiegold")
    @JvmField @ConfigOption(name = "Hideonwall", desc = "Highlight and color") @Accordion val hideonwall = SafariEspMobSettings("Hideonwall")
    @JvmField @ConfigOption(name = "Hideyho", desc = "Highlight and color") @Accordion val hideyho = SafariEspMobSettings("Hideyho")
    @JvmField @ConfigOption(name = "Doomspiral", desc = "Highlight and color") @Accordion val doomspiral = SafariEspMobSettings("Doomspiral")
}
