package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.ChromaColour
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType

object SafariEspConfig {
    data class Group(var enabled: Boolean = true, var onlyInBiome: Boolean = true)
    data class Mob(var enabled: Boolean, var color: String)
    private val palette = mapOf(
        "Foxtrot" to 0xFFF26F14.toInt(),
        "Bluebird" to 0xFF173AE8.toInt(),
        "Honeybug" to 0xFFF0BE1E.toInt(),
        "Treefrog" to 0xFF39FF57.toInt(),
        "Woodchucker" to 0xFFF32F13.toInt(),
        "Fluffling" to 0xFF66DAFA.toInt(),
        "Hideonfloor" to 0xFF0095FF.toInt(),
        "Parakeet" to 0xFF6BEE3B.toInt(),
        "Macaw" to 0xFFFFD200.toInt(),
        "Cavernfish" to 0xFFF68929.toInt(),
        "Flitter" to 0xFF55C0EB.toInt(),
        "Shyworm" to 0xFF79F24C.toInt(),
        "Driftling" to 0xFFFFEC5B.toInt(),
        "Chuckwalla" to 0xFFE3C0A2.toInt(),
        "Rockmite" to 0xFF525252.toInt(),
        "Scrappy" to 0xFFEA5577.toInt(),
        "Snoozle" to 0xFFEB3B3B.toInt(),
        "Gemzie" to 0xFF9132FF.toInt(),
        "Strongarm" to 0xFFE47D15.toInt(),
        "Tepid" to 0xFFFF0000.toInt(),
        "Polaris" to 0xFFFF0000.toInt(),
        "Shuddersquid" to 0xFFFF0000.toInt(),
        "Billygoat" to 0xFFFF0000.toInt(),
        "Mantis Shrimp" to 0xFFFF0000.toInt(),
        "Nozzlenose" to 0xFFFF0000.toInt(),
        "Troodon" to 0xFFFF0000.toInt(),
        "Wumpa" to 0xFFFF0000.toInt(),
        "Areita" to 0xFF00F7FF.toInt(),
        "Bloodbat" to 0xFFF31C12.toInt(),
        "Duplico" to 0xFFD7D7D7.toInt(),
        "Gazer" to 0xFF07CFF3.toInt(),
        "Litterbug" to 0xFFAB00FF.toInt(),
        "Solsnatcher" to 0xFFFF0000.toInt(),
        "Gimmiegold" to 0xFFF3C900.toInt(),
        "Hideonwall" to 0xFFFF00F2.toInt(),
        "Hideyho" to 0xFFFFFFFF.toInt(),
        "Doomspiral" to 0xFF00F0EF.toInt()
    )
    val groups = listOf("floor", "cavern", "forest", "icy", "haunted").associateWith { Group() }
    val mobs = SafariEspRules.mobs.associate { it.name to Mob(true, defaultColor(palette.getValue(it.name))) }
    const val DEFAULT_TEXT_SCALE = 2f
    fun validTextScale(value: Float) = if (value.isFinite()) value.coerceIn(.5f, 4f) else DEFAULT_TEXT_SCALE
    var textScale = DEFAULT_TEXT_SCALE
        set(value) { field = validTextScale(value) }
    val labelWorldScale get() = .025f * textScale
    val floorTextScales = FloorDropType.entries.associateWith { DEFAULT_TEXT_SCALE }.toMutableMap()
    fun dropTextScale(type: FloorDropType) = validTextScale(floorTextScales[type] ?: textScale)
    fun dropLabelWorldScale(type: FloorDropType) = .025f * dropTextScale(type)
    val scrappyFoodColors = net.johnceo.sparklingmutuals.safari.esp.ScrappyFood.entries.associateWith { defaultColor(it.defaultRgb) }.toMutableMap()
    var scrappyTextScale = DEFAULT_TEXT_SCALE
        set(value) { field = validTextScale(value) }
    val scrappyLabelWorldScale get() = .025f * scrappyTextScale
    var teammateTextScale = DEFAULT_TEXT_SCALE
        set(value) { field = validTextScale(value) }
    val teammateLabelWorldScale get() = .025f * teammateTextScale
    var teammateColor = defaultColor(0xFF55FF55.toInt())
    var rewardDrops = false
    val floorOnlyInBiome = FloorDropType.entries.associateWith { it == FloorDropType.GENERIC }.toMutableMap()
    fun dropOnlyInBiome(type: FloorDropType?) = floorOnlyInBiome[type] ?: true
    private fun defaultDropColor(type: FloorDropType) = defaultColor(if (type == FloorDropType.GENERIC) 0xFF6D6D75.toInt() else 0xFF00FF00.toInt())
    val floorColors = FloorDropType.entries.associateWith(::defaultDropColor).toMutableMap()
    fun dropColor(type: FloorDropType) = floorColors[type] ?: floorColor
    var floorColor = defaultColor(0xFF00FF00.toInt())
        set(value) {
            if (field != value) floorColors.keys.forEach { floorColors[it] = value }
            field = value
        }
    fun setAllDropColors(value: String) {
        floorColor = value
        floorColors.keys.forEach { floorColors[it] = value }
    }
    var rockmiteMoundEnabled = true
    var rockmiteMoundColor = defaultColor(0xFFFFFFFF.toInt())
    fun entityEnabled(name: String, mound: Boolean = false) =
        if (name == "Rockmite" && mound) rockmiteMoundEnabled else mobs.getValue(name).enabled
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
        rewardDrops = properties.getProperty("esp.floor.rewards.enabled")?.toBooleanStrictOrNull() ?: false
        floorOnlyInBiome.keys.forEach { type ->
            floorOnlyInBiome[type] = properties.getProperty("esp.floor.${type.name.lowercase()}.onlyInBiome")
                ?.toBooleanStrictOrNull() ?: (type == FloorDropType.GENERIC)
        }
        textScale = properties.getProperty("esp.textScale")?.toFloatOrNull() ?: DEFAULT_TEXT_SCALE
        scrappyTextScale = properties.getProperty("esp.scrappy.textScale")?.toFloatOrNull() ?: DEFAULT_TEXT_SCALE
        scrappyFoodColors.keys.forEach { food ->
            scrappyFoodColors[food] = validColor(properties.getProperty("esp.scrappy.${food.name.lowercase()}.color"), defaultColor(food.defaultRgb))
        }
        teammateTextScale = properties.getProperty("esp.teammate.textScale")?.toFloatOrNull() ?: textScale
        teammateColor = validColor(properties.getProperty("esp.teammate.color"), defaultColor(0xFF55FF55.toInt()))
        floorTextScales.keys.forEach { type ->
            floorTextScales[type] = validTextScale(properties.getProperty("esp.floor.${type.name.lowercase()}.textScale")?.toFloatOrNull() ?: textScale)
        }
        groups.forEach { (name, group) ->
            group.enabled = properties.getProperty("esp.$name.enabled")?.toBooleanStrictOrNull() ?: true
            group.onlyInBiome = properties.getProperty("esp.$name.onlyInBiome")?.toBooleanStrictOrNull() ?: true
        }
        mobs.forEach { (name, mob) ->
            mob.enabled = properties.getProperty("esp.${key(name)}.enabled")?.toBooleanStrictOrNull() ?: true
            mob.color = validColor(properties.getProperty("esp.${key(name)}.color"), defaultColor(palette.getValue(name)))
        }
        floorColor = validColor(properties.getProperty("esp.floor.color"), defaultColor(0xFF00FF00.toInt()))
        floorColors.keys.forEach { type -> floorColors[type] = validColor(properties.getProperty("esp.floor.${type.name.lowercase()}.color"),
            if (type == FloorDropType.GENERIC) defaultDropColor(type) else floorColor) }
        rockmiteMoundEnabled = properties.getProperty("esp.rockmite.moundEnabled")?.toBooleanStrictOrNull()
            ?: properties.getProperty("esp.rockmite.enabled")?.toBooleanStrictOrNull() ?: true
        rockmiteMoundColor = validColor(properties.getProperty("esp.rockmite.moundColor"),
            properties.getProperty("esp.rockmite.color")?.let { mobs.getValue("Rockmite").color } ?: defaultColor(0xFFFFFFFF.toInt()))
    }
    fun save(properties: Properties) {
        properties.setProperty("esp.floor.rewards.enabled", rewardDrops.toString())
        floorOnlyInBiome.forEach { (type, value) ->
            properties.setProperty("esp.floor.${type.name.lowercase()}.onlyInBiome", value.toString())
        }
        properties.setProperty("esp.scrappy.textScale", scrappyTextScale.toString())
        scrappyFoodColors.forEach { (food,color) -> properties.setProperty("esp.scrappy.${food.name.lowercase()}.color",color) }
        properties.setProperty("esp.textScale", textScale.toString())
        properties.setProperty("esp.teammate.textScale", teammateTextScale.toString())
        properties.setProperty("esp.teammate.color", teammateColor)
        floorTextScales.forEach { (type, _) ->
            properties.setProperty("esp.floor.${type.name.lowercase()}.textScale", dropTextScale(type).toString())
        }
        groups.forEach { (name, group) ->
            properties.setProperty("esp.$name.enabled", group.enabled.toString())
            if (name != "floor") properties.setProperty("esp.$name.onlyInBiome", group.onlyInBiome.toString())
        }
        mobs.forEach { (name, mob) ->
            properties.setProperty("esp.${key(name)}.enabled", mob.enabled.toString())
            properties.setProperty("esp.${key(name)}.color", mob.color)
        }
        properties.setProperty("esp.floor.color", floorColor)
        floorColors.forEach { (type, color) -> properties.setProperty("esp.floor.${type.name.lowercase()}.color", color) }
        properties.setProperty("esp.rockmite.moundEnabled", rockmiteMoundEnabled.toString())
        properties.setProperty("esp.rockmite.moundColor", rockmiteMoundColor)
    }
}
