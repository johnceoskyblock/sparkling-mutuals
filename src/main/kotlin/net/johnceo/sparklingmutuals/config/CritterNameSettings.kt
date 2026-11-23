package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.ui.CritterNames

class CustomizationGuide(rename: Boolean) {
    @JvmField @StandaloneDescription @ConfigOption(name="",desc="") val guide=false
    val description = when {
        !rename -> "Choose server nametags, ESP marker titles, both or neither. Marker titles automatically use the names set in Critter Names. Pity tracks identified catch attempts for each critter."
        SafariSettings.standardEdition -> "Set the name shown in HUDs, local chat and Hunting Box tooltips."
        else -> "Set the name shown in HUDs, nametags, Hypixel messages and Hunting Box tooltips. Names are visual only. Nameplates use these names automatically."
    }
}

class CritterNameEntry(val canonical: String) {
    @JvmField @ConfigOption(name="Displayed name",desc="Blank uses the original name. Visual only; outgoing messages keep the original.")
    @ConfigEditorText var name = CritterNames.get(canonical)
}
class CritterNameBiomeSettings(biome: SafariBiome) {
    val entries = biome.critters.map {CritterNameEntry(it.name)}
    fun apply() = entries.map {CritterNames.set(it.canonical,it.name)}.any {it}
}
class CritterNameSettings {
        @JvmField @ConfigOption(name = "Use custom critter names", desc = "Show your saved custom names in client-side visuals.")
        @ConfigEditorBoolean var enabled = ConfigManager.customCritterNames
    @JvmField @ConfigOption(name="Forest",desc="") @Accordion val forest=CritterNameBiomeSettings(SafariBiome.FOREST)
    @JvmField @ConfigOption(name="Cavern",desc="") @Accordion val cavern=CritterNameBiomeSettings(SafariBiome.CAVERN)
    @JvmField @ConfigOption(name="Icy",desc="") @Accordion val icy=CritterNameBiomeSettings(SafariBiome.ICY)
    @JvmField @ConfigOption(name="Haunted",desc="") @Accordion val haunted=CritterNameBiomeSettings(SafariBiome.HAUNTED)
    fun apply() = listOf(forest.apply(),cavern.apply(),icy.apply(),haunted.apply()).any {it}
}
