package net.johnceo.sparklingmutuals.config

import java.util.Properties
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRoster

object CritterNameplates {
    data class Options(val enabled: Boolean=false,val replaceTag: Boolean=true,val text: String="{name}",
        val textScale: Float=2f,val range: Int=80,val useEspColor: Boolean=true,
        val color: String=AppearanceConfig.color(0xFFFFFF),val background: Boolean=true)
    val options=SafariRoster.all.associate {it.name to Options()}.toMutableMap()
    private var enabledNames=emptySet<String>()
    fun refresh() {enabledNames=options.filterValues {it.enabled}.keys.toSet()}
    fun enabled(name: String)=name in enabledNames
    val anyEnabled get()=enabledNames.isNotEmpty()
    fun clean(text: String)=text.replace(Regex("[\\p{Cc}\\p{Cf}]"),"").trim().take(96).ifBlank {"{name}"}
    fun label(name: String,text: String,distance: Int,sparkling: Boolean=false)=clean(text)
        .replace("{name}",if(sparkling) "SPARKLING $name" else name).replace("{distance}","${distance}m")
    private fun key(name: String)="esp.nameplate."+name.lowercase(java.util.Locale.ROOT).replace(' ','_')+"."
    fun load(p: Properties) { options.keys.toList().forEach {name ->
        val k=key(name);fun bool(s: String,default: Boolean)=p.getProperty(k+s)?.toBooleanStrictOrNull() ?: default
        options[name]=Options(bool("enabled",false),bool("replaceTag",true),clean(p.getProperty(k+"text") ?: "{name}"),
            SafariEspConfig.validTextScale(p.getProperty(k+"scale")?.toFloatOrNull() ?: 2f),
            (p.getProperty(k+"range")?.toIntOrNull() ?: 80).coerceIn(8,160),bool("useEspColor",true),
            SafariEspConfig.validColor(p.getProperty(k+"color"),AppearanceConfig.color(0xFFFFFF)),bool("background",true))
    };refresh() }
    fun save(p: Properties) {options.forEach {(name,v)->
        val k=key(name)
        mapOf("enabled" to v.enabled,"replaceTag" to v.replaceTag,"text" to v.text,"scale" to v.textScale,
            "range" to v.range,"useEspColor" to v.useEspColor,"color" to v.color,"background" to v.background)
            .forEach {(field,value)->p.setProperty(k+field,value.toString())}
    }}
}
class NameplateEntry(val canonical: String) {
    private val saved=CritterNameplates.options.getValue(canonical)
    @JvmField @ConfigOption(name="Enabled",desc="") @ConfigEditorBoolean var enabled=saved.enabled
    @JvmField @ConfigOption(name="Replace Hypixel nametag",desc="Only while this ESP nameplate is shown.") @ConfigEditorBoolean var replaceTag=saved.replaceTag
    @JvmField @ConfigOption(name="Text",desc="{name} and {distance}. Uses your custom critter name.") @ConfigEditorText var text=saved.text
    @JvmField @ConfigOption(name="Text size",desc="") @ConfigEditorSlider(minValue=.5f,maxValue=4f,minStep=.25f) var scale=saved.textScale
    @JvmField @ConfigOption(name="Maximum distance",desc="Blocks.") @ConfigEditorSlider(minValue=8f,maxValue=160f,minStep=1f) var range=saved.range
    @JvmField @ConfigOption(name="Use critter ESP color",desc="") @ConfigEditorBoolean var useEspColor=saved.useEspColor
    @JvmField @ConfigOption(name="Text color",desc="") @ConfigEditorColour var color=saved.color
    @JvmField @ConfigOption(name="Background",desc="") @ConfigEditorBoolean var background=saved.background
    fun apply(): Boolean {
        val next=CritterNameplates.Options(enabled,replaceTag,CritterNameplates.clean(text),SafariEspConfig.validTextScale(scale),range.coerceIn(8,160),useEspColor,
            SafariEspConfig.validColor(color,saved.color),background)
        val changed=CritterNameplates.options.put(canonical,next)!=next
        CritterNameplates.refresh()
        return changed
    }
}
class NameplateBiomeSettings(biome: SafariBiome) {
    val entries=biome.critters.map {NameplateEntry(it.name)}
    fun apply()=entries.map {it.apply()}.any {it}
}
class NameplateSettings {
    @JvmField @ConfigOption(name="Forest",desc="") @Accordion val forest=NameplateBiomeSettings(SafariBiome.FOREST)
    @JvmField @ConfigOption(name="Cavern",desc="") @Accordion val cavern=NameplateBiomeSettings(SafariBiome.CAVERN)
    @JvmField @ConfigOption(name="Icy",desc="") @Accordion val icy=NameplateBiomeSettings(SafariBiome.ICY)
    @JvmField @ConfigOption(name="Haunted",desc="") @Accordion val haunted=NameplateBiomeSettings(SafariBiome.HAUNTED)
    fun apply()=listOf(forest.apply(),cavern.apply(),icy.apply(),haunted.apply()).any {it}
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
