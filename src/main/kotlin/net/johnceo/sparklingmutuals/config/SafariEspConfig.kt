package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.ChromaColour
import net.johnceo.sparklingmutuals.safari.SafariEspRules
import java.util.Properties

object SafariEspConfig {
    data class Group(var enabled: Boolean = true, var onlyInBiome: Boolean = true)
    data class Mob(var enabled: Boolean, var color: String)
    private val defaultMobs = setOf("Rockmite", "Treefrog", "Woodchucker", "Hideonfloor", "Shuddersquid",
        "Billygoat", "Nozzlenose", "Duplico", "Hideonwall", "Hideyho", "Doomspiral")
    val groups = listOf("floor", "cavern", "forest", "icy", "haunted").associateWith { Group() }
    val mobs = SafariEspRules.mobs.associate { it.name to Mob(it.name in defaultMobs, defaultColor(it.color)) }
    var floorColor = defaultColor(0xFF00FF00.toInt())
    var rockmiteMoundColor = mobs.getValue("Rockmite").color
    fun entityColor(name: String, mound: Boolean = false) =
        if (name == "Rockmite" && mound) rockmiteMoundColor else mobs.getValue(name).color
    private fun defaultColor(rgb: Int) = ChromaColour.special(0, 255, rgb)
    fun validColor(value: String?, fallback: String): String = value?.takeIf { color ->
        val parts = color.split(':')
        parts.size == 5 && parts.all { it.toIntOrNull() in 0..255 }
    } ?: fallback
    fun rgb(color: String) = ChromaColour.specialToChromaRGB(color)
    private fun key(name: String) = name.lowercase().replace(' ', '_')
    fun load(properties: Properties) {
        groups.forEach { (name, group) ->
            group.enabled = properties.getProperty("esp.$name.enabled")?.toBooleanStrictOrNull() ?: true
            group.onlyInBiome = properties.getProperty("esp.$name.onlyInBiome")?.toBooleanStrictOrNull() ?: true
        }
        mobs.forEach { (name, mob) ->
            mob.enabled = properties.getProperty("esp.${key(name)}.enabled")?.toBooleanStrictOrNull() ?: (name in defaultMobs)
            mob.color = validColor(properties.getProperty("esp.${key(name)}.color"), defaultColor(SafariEspRules.mobs.first { it.name == name }.color))
        }
        floorColor = validColor(properties.getProperty("esp.floor.color"), defaultColor(0xFF00FF00.toInt()))
        rockmiteMoundColor = validColor(properties.getProperty("esp.rockmite.moundColor"), mobs.getValue("Rockmite").color)
    }
    fun save(properties: Properties) {
        groups.forEach { (name, group) ->
            properties.setProperty("esp.$name.enabled", group.enabled.toString())
            properties.setProperty("esp.$name.onlyInBiome", group.onlyInBiome.toString())
        }
        mobs.forEach { (name, mob) ->
            properties.setProperty("esp.${key(name)}.enabled", mob.enabled.toString())
            properties.setProperty("esp.${key(name)}.color", mob.color)
        }
        properties.setProperty("esp.floor.color", floorColor)
        properties.setProperty("esp.rockmite.moundColor", rockmiteMoundColor)
    }
}
