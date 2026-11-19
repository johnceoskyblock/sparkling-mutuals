package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.ui.CritterNames

class CritterNameEntry(val canonical: String) {
    @JvmField @ConfigOption(name="Custom name",desc="Blank uses the original name. Visual only; chat reports keep the original.")
    @ConfigEditorText var name = CritterNames.get(canonical)
}
class CritterNameBiomeSettings(biome: SafariBiome) {
    val entries = biome.critters.map {CritterNameEntry(it.name)}
    fun apply() = entries.map {CritterNames.set(it.canonical,it.name)}.any {it}
}
class CritterNameSettings {
    @JvmField @ConfigOption(name="Forest",desc="") @Accordion val forest=CritterNameBiomeSettings(SafariBiome.FOREST)
    @JvmField @ConfigOption(name="Cavern",desc="") @Accordion val cavern=CritterNameBiomeSettings(SafariBiome.CAVERN)
    @JvmField @ConfigOption(name="Icy",desc="") @Accordion val icy=CritterNameBiomeSettings(SafariBiome.ICY)
    @JvmField @ConfigOption(name="Haunted",desc="") @Accordion val haunted=CritterNameBiomeSettings(SafariBiome.HAUNTED)
    fun apply() = listOf(forest.apply(),cavern.apply(),icy.apply(),haunted.apply()).any {it}
}
