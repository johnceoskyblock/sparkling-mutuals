package net.johnceo.sparklingmutuals.config

import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariRoster

/** Adapted from SafariUtils Markers / WaypointRenderer / CritterCatchRules, MIT, Serkosea & MrCloudy2.
 * Reference revision 12525e676f44299ebbe117c665351b8dc4650a9b. See licenses/SafariUtils-MIT.txt.
 * Only presentation policy lives here; SM's existing target eligibility remains authoritative.
 */
object CritterNameplates {
    data class Options(val enabled: Boolean=true, val display: Int=2, val distance: Boolean=false,
        val pity: Boolean=true, val eagle: Int=0, val color: String=AppearanceConfig.color(0xFFFFFF),
        val uniqueColors: Boolean=false, val rarityColors: Boolean=false, val entityColorOverride: Boolean=false,
        val fixedColor: Boolean=false)
    var settings=Options()
    private val critters = SafariRoster.all.associate { it.name to (it.name != "Litterbug") }.toMutableMap()
    fun enabledFor(name: String) = critters[name] == true
    fun applyCritters(options: CritterToggleSettings) = options.apply(critters)
    fun showsVanilla(option: Options=settings)=!option.enabled || option.display==1 || option.display==3
    fun showsMarker(option: Options=settings)=option.enabled && option.display in 2..3
    private val dedicated=setOf("Hideyho","Hideonwall","Duplico","Bloodbat","Hideonfloor")
    fun threshold(name: String): Int=when(SafariRoster.color(name) and 0xFFFFFF) {
        0x55FF55 -> 2; 0x5555FF -> 3; 0xAA00AA -> 5; 0xFFAA00 -> 20; else -> 1
    }
    fun pityLabel(name: String, attempts: Int, option: Options=settings): String {
        val threshold=threshold(name)
        if(!option.pity || name=="Hideyho" || threshold==1 || threshold==2 && option.eagle>=2)return ""
        return " ("+attempts.coerceAtLeast(0)+"/"+threshold+")"
    }
    fun label(name: String, attempts: Int, distance: Double, sparkling: Boolean=false, option: Options=settings,
        title: String = name, showPity: Boolean = true) =
        (if(sparkling) "SPARKLING " else "")+title+(if (showPity) pityLabel(name,attempts,option) else "")+
            (if(option.distance) " "+Math.round(distance)+"m" else "")
    fun matchesEspColor(name: String, option: Options=settings) =
        name in dedicated && !option.entityColorOverride ||
            !option.uniqueColors && !option.rarityColors && !option.fixedColor
    fun color(name: String, caught: Boolean, fallback: Int, option: Options=settings): Int {
        if(matchesEspColor(name,option))return fallback
        return if(option.uniqueColors) if(caught) 0xFF55FF55.toInt() else 0xFFFF7777.toInt()
            else if(option.rarityColors) SafariRoster.color(name) else SafariEspConfig.rgb(option.color)
    }
    private const val PREFIX="esp.nameplates."
    fun load(p: Properties) {
        fun bool(key: String, default: Boolean)=p.getProperty(PREFIX+key)?.toBooleanStrictOrNull() ?: default
        // The old per-critter template implementation is deliberately replaced; aliases use separate keys.
        settings=Options(bool("enabled",true),(p.getProperty(PREFIX+"display")?.toIntOrNull() ?: 2).coerceIn(0,3),
            bool("distance",false),bool("pity",true),(p.getProperty(PREFIX+"eagle")?.toIntOrNull() ?: 0).coerceIn(0,5),
            SafariEspConfig.validColor(p.getProperty(PREFIX+"color"),AppearanceConfig.color(0xFFFFFF)),
            bool("uniqueColors",false),bool("rarityColors",false),bool("entityColorOverride",false),bool("fixedColor",false))
        for (critter in SafariRoster.all) {
            critters[critter.name] = bool(critterKey(critter.name), critter.name != "Litterbug")
        }
    }
    private fun critterKey(name: String) = "critter." + name.lowercase(java.util.Locale.ROOT).replace(' ', '_')
    fun save(p: Properties) {
        p.stringPropertyNames().filter {it.startsWith("esp.nameplate.")}.forEach {p.remove(it)}
        val v=settings
        mapOf("enabled" to v.enabled,"display" to v.display,"distance" to v.distance,"pity" to v.pity,
            "eagle" to v.eagle,"color" to v.color,"uniqueColors" to v.uniqueColors,
            "rarityColors" to v.rarityColors,"entityColorOverride" to v.entityColorOverride,"fixedColor" to v.fixedColor)
            .forEach {(key,value)->p.setProperty(PREFIX+key,value.toString())}
        critters.forEach { (name, enabled) -> p.setProperty(PREFIX + critterKey(name), enabled.toString()) }
    }
}
class NameplateSettings {
    private val saved=CritterNameplates.settings
    @JvmField @ConfigOption(name = "Enable nameplates", desc = "Apply these name settings to critters shown by your ESP.") @ConfigEditorBoolean var enabled=saved.enabled
    @JvmField @ConfigOption(name = "Name display", desc = "Vanilla shows Hypixel's nametag. Marker shows our ESP label.")
    @ConfigEditorDropdown(values=["None","Vanilla","Marker","Both"]) var display=saved.display
    @JvmField @ConfigOption(name = "Show distance", desc = "Add the distance in blocks to ESP labels.") @ConfigEditorBoolean var distance=saved.distance
    @JvmField @ConfigOption(name = "Show catch pity", desc = "Show catch attempts and confirmed hits on Rockmite mounds.") @ConfigEditorBoolean var pity=saved.pity
    @JvmField @ConfigOption(name = "Eagle pet rarity", desc = "Uncommon or better Eagle makes Uncommon catches guaranteed, so their pity is hidden.")
    @ConfigEditorDropdown(values=["None","Common","Uncommon","Rare","Epic","Legendary"]) var eagle=saved.eagle
    @JvmField @ConfigOption(name = "Nameplate colors", desc = "") @Accordion val colors=NameplateColors(saved)
    @JvmField @ConfigOption(name = "Per-critter nameplates", desc = "Choose which critters get custom nameplates.") @Accordion
    val critters = CritterToggleSettings(CritterNameplates::enabledFor)
    fun apply(): Boolean {
        val next=CritterNameplates.Options(enabled,display.coerceIn(0,3),distance,pity,eagle.coerceIn(0,5),
            SafariEspConfig.validColor(colors.color,saved.color),colors.uniqueColors,colors.rarityColors,
            colors.entityColorOverride,colors.fixedColor)
        val changed=next!=CritterNameplates.settings
        CritterNameplates.settings=next
        return CritterNameplates.applyCritters(critters) || changed
    }
}
class NameplateColors(saved: CritterNameplates.Options) {
    @JvmField @ConfigOption(name = "Use fixed color", desc = "Set one nameplate color when first-capture and rarity colors are off. By default, nameplates match critter ESP.") @ConfigEditorBoolean var fixedColor=saved.fixedColor
    @JvmField @ConfigOption(name = "Fixed color", desc = "") @ConfigEditorColour var color=saved.color
    @JvmField @ConfigOption(name = "Color by first capture", desc = "Red until the party catches this critter type, then green. Overrides rarity and fixed colors.") @ConfigEditorBoolean var uniqueColors=saved.uniqueColors
    @JvmField @ConfigOption(name = "Color by rarity", desc = "Use rarity colors when first-capture colors are off.") @ConfigEditorBoolean var rarityColors=saved.rarityColors
    @JvmField @ConfigOption(name = "Apply to special critters", desc = "Also apply these colors to Hideyho, Hideonwall, Duplico, Bloodbat and Hideonfloor.") @ConfigEditorBoolean var entityColorOverride=saved.entityColorOverride
}
class TeammateAppearanceSettings(private val original: SafariSettings.SafariTeammateSettings) {
    private var appliedColor=original.color
    private var appliedScale=original.textScale
    @JvmField @ConfigOption(name="Highlight color",desc="") @ConfigEditorColour var color=original.color
    @JvmField @ConfigOption(name="Name text size",desc="") @ConfigEditorSlider(minValue=.5f,maxValue=4f,minStep=.25f) var textScale=original.textScale
    fun apply(): Boolean {
        val changed=appliedColor!=color || appliedScale!=textScale
        if(appliedColor!=color)original.color=color
        if(appliedScale!=textScale)original.textScale=textScale
        appliedColor=color;appliedScale=textScale
        return changed
    }
}
