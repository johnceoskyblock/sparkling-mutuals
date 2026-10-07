package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.*

/** Shared controls keep the four biome categories consistent. */
open class SafariEspGroupSettings(private val group: String) {
    @JvmField @ConfigOption(name = "Enable ESP", desc = "Show highlights through terrain.")
    @ConfigEditorBoolean var enabled = SafariEspConfig.groups.getValue(group).enabled
    @JvmField @ConfigOption(name = "Only in current biome", desc = "Show only targets in your current biome.")
    @ConfigEditorBoolean var onlyInBiome = SafariEspConfig.groups.getValue(group).onlyInBiome
    open fun apply(): Boolean {
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
    fun apply(): Boolean {
        val saved = SafariEspConfig.mobs.getValue(name)
        val changed = saved.enabled != enabled
        saved.enabled = enabled
        return changed
    }
}
class SafariFloorEspSettings : SafariEspGroupSettings("floor")

class SafariCavernEspSettings : SafariEspGroupSettings("cavern") {
    @JvmField @ConfigOption(name = "Cavernfish", desc = "Highlight this critter.") @Accordion val cavernfish = SafariEspMobSettings("Cavernfish")
    @JvmField @ConfigOption(name = "Flitter", desc = "Highlight this critter.") @Accordion val flitter = SafariEspMobSettings("Flitter")
    @JvmField @ConfigOption(name = "Shyworm", desc = "Highlight this critter.") @Accordion val shyworm = SafariEspMobSettings("Shyworm")
    @JvmField @ConfigOption(name = "Driftling", desc = "Highlight this critter.") @Accordion val driftling = SafariEspMobSettings("Driftling")
    @JvmField @ConfigOption(name = "Chuckwalla", desc = "Highlight this critter.") @Accordion val chuckwalla = SafariEspMobSettings("Chuckwalla")
    @JvmField @ConfigOption(name = "Rockmite silverfish", desc = "Highlight this critter.") @Accordion val rockmite = SafariEspMobSettings("Rockmite")
    @JvmField @ConfigOption(name = "Rockmite mound", desc = "Highlight unbroken Rockmite mounds.")
    @ConfigEditorBoolean var rockmiteMound = SafariEspConfig.rockmiteMoundEnabled
    override fun apply(): Boolean {
        val changed = SafariEspConfig.rockmiteMoundEnabled != rockmiteMound
        SafariEspConfig.rockmiteMoundEnabled = rockmiteMound
        return super.apply() || changed
    }
    @JvmField @ConfigOption(name = "Scrappy", desc = "Highlight this critter.") @Accordion val scrappy = SafariEspMobSettings("Scrappy")
    @JvmField @ConfigOption(name = "Snoozle", desc = "Highlight this critter.") @Accordion val snoozle = SafariEspMobSettings("Snoozle")
    @JvmField @ConfigOption(name = "Gemzie", desc = "Highlight this critter.") @Accordion val gemzie = SafariEspMobSettings("Gemzie")
}

class SafariForestEspSettings : SafariEspGroupSettings("forest") {
    @JvmField @ConfigOption(name = "Foxtrot", desc = "Highlight this critter.") @Accordion val foxtrot = SafariEspMobSettings("Foxtrot")
    @JvmField @ConfigOption(name = "Bluebird", desc = "Highlight this critter.") @Accordion val bluebird = SafariEspMobSettings("Bluebird")
    @JvmField @ConfigOption(name = "Honeybug", desc = "Highlight this critter.") @Accordion val honeybug = SafariEspMobSettings("Honeybug")
    @JvmField @ConfigOption(name = "Treefrog", desc = "Highlight this critter.") @Accordion val treefrog = SafariEspMobSettings("Treefrog")
    @JvmField @ConfigOption(name = "Woodchucker", desc = "Highlight this critter.") @Accordion val woodchucker = SafariEspMobSettings("Woodchucker")
    @JvmField @ConfigOption(name = "Fluffling", desc = "Highlight this critter.") @Accordion val fluffling = SafariEspMobSettings("Fluffling")
    @JvmField @ConfigOption(name = "Hideonfloor", desc = "Highlight this critter.") @Accordion val hideonfloor = SafariEspMobSettings("Hideonfloor")
    @JvmField @ConfigOption(name = "Parakeet", desc = "Highlight this critter.") @Accordion val parakeet = SafariEspMobSettings("Parakeet")
    @JvmField @ConfigOption(name = "Macaw", desc = "Highlight this critter.") @Accordion val macaw = SafariEspMobSettings("Macaw")
}

class SafariIcyEspSettings : SafariEspGroupSettings("icy") {
    @JvmField @ConfigOption(name = "Strongarm", desc = "Highlight this critter.") @Accordion val strongarm = SafariEspMobSettings("Strongarm")
    @JvmField @ConfigOption(name = "Tepid", desc = "Highlight this critter.") @Accordion val tepid = SafariEspMobSettings("Tepid")
    @JvmField @ConfigOption(name = "Polaris", desc = "Highlight this critter.") @Accordion val polaris = SafariEspMobSettings("Polaris")
    @JvmField @ConfigOption(name = "Shuddersquid", desc = "Highlight this critter.") @Accordion val shuddersquid = SafariEspMobSettings("Shuddersquid")
    @JvmField @ConfigOption(name = "Billygoat", desc = "Highlight this critter.") @Accordion val billygoat = SafariEspMobSettings("Billygoat")
    @JvmField @ConfigOption(name = "Mantis Shrimp", desc = "Highlight this critter.") @Accordion val mantisshrimp = SafariEspMobSettings("Mantis Shrimp")
    @JvmField @ConfigOption(name = "Nozzlenose", desc = "Highlight this critter.") @Accordion val nozzlenose = SafariEspMobSettings("Nozzlenose")
    @JvmField @ConfigOption(name = "Troodon", desc = "Highlight this critter.") @Accordion val troodon = SafariEspMobSettings("Troodon")
    @JvmField @ConfigOption(name = "Wumpa", desc = "Highlight this critter.") @Accordion val wumpa = SafariEspMobSettings("Wumpa")
}

class SafariHauntedEspSettings : SafariEspGroupSettings("haunted") {
    @JvmField @ConfigOption(name = "Areita", desc = "Highlight this critter.") @Accordion val areita = SafariEspMobSettings("Areita")
    @JvmField @ConfigOption(name = "Bloodbat", desc = "Highlight this critter.") @Accordion val bloodbat = SafariEspMobSettings("Bloodbat")
    @JvmField @ConfigOption(name = "Duplico", desc = "Highlight this critter.") @Accordion val duplico = SafariEspMobSettings("Duplico")
    @JvmField @ConfigOption(name = "Gazer", desc = "Highlight this critter.") @Accordion val gazer = SafariEspMobSettings("Gazer")
    @JvmField @ConfigOption(name = "Litterbug", desc = "Highlight this critter.") @Accordion val litterbug = SafariEspMobSettings("Litterbug")
    @JvmField @ConfigOption(name = "Solsnatcher", desc = "Highlight this critter.") @Accordion val solsnatcher = SafariEspMobSettings("Solsnatcher")
    @JvmField @ConfigOption(name = "Gimmiegold", desc = "Highlight this critter.") @Accordion val gimmiegold = SafariEspMobSettings("Gimmiegold")
    @JvmField @ConfigOption(name = "Hideonwall", desc = "Highlight this critter.") @Accordion val hideonwall = SafariEspMobSettings("Hideonwall")
    @JvmField @ConfigOption(name = "Hideyho", desc = "Highlight this critter.") @Accordion val hideyho = SafariEspMobSettings("Hideyho")
    @JvmField @ConfigOption(name = "Doomspiral", desc = "Highlight this critter.") @Accordion val doomspiral = SafariEspMobSettings("Doomspiral")
}
