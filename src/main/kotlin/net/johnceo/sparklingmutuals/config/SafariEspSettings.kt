package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.annotations.*

/** Modes own targets; these switches only control where their highlights appear. */
class SafariEspSettings {
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
        var changed = false
        mapOf("floor" to floor, "forest" to forest, "cavern" to cavern, "icy" to icy, "haunted" to haunted).forEach { (key, value) ->
            val group = SafariEspConfig.groups.getValue(key)
            if (group.onlyInBiome != value) changed = true
            group.onlyInBiome = value
        }
        return changed
    }
}
