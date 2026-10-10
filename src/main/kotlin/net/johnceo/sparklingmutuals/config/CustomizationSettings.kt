package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.LocalChat

import io.github.notenoughupdates.moulconfig.annotations.*
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/** One shared hex selection; color actions never change feature toggles. */
class ColorChoice(private val read: () -> String, private val write: (String) -> Unit, private val selection: () -> String) {
    @JvmField @ConfigOption(name = "Color", desc = "Choose a color.") @ConfigEditorColour var color = read()
    @JvmField @ConfigOption(name = "Apply selected color", desc = "Use the hex color at the top of this tab.")
    @ConfigEditorButton(buttonText = "Set Color") val setColor = Runnable {
        if (setSelected()) ConfigManager.save()
        else Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component("[SM] Enter a hex color such as #55FF55."))
    }
    fun setSelected(): Boolean {
        val value = AppearanceConfig.withHex(SafariEspConfig.validColor(color, read()), selection()) ?: return false
        color = value; write(value)
        return true
    }
    fun apply(): Boolean {
        val value = SafariEspConfig.validColor(color, read())
        val changed = value != read()
        write(value)
        return changed
    }
}

class PanelAppearanceSettings(private val style: PanelStyle, selection: () -> String) {
    @JvmField @ConfigOption(name = "Border", desc = "Draw a border around this panel.")
    @ConfigEditorBoolean var borderEnabled = style.borderEnabled
    @JvmField @ConfigOption(name = "Border color", desc = "") @Accordion
    val border = ColorChoice({ style.borderColor }, { style.borderColor = it }, selection)
    @JvmField @ConfigOption(name = "Background color", desc = "") @Accordion
    val background = ColorChoice({ style.backgroundColor }, { style.backgroundColor = it }, selection)
    @JvmField @ConfigOption(name = "Background transparency", desc = "0% is opaque; 100% is invisible.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var transparency = style.transparency
    fun apply(): Boolean {
        val changed = style.borderEnabled != borderEnabled || style.transparency != transparency
        style.borderEnabled = borderEnabled; style.transparency = transparency
        return listOf(border.apply(), background.apply(), changed).any { it }
    }
}

class CustomizationSettings {
    @JvmField @ConfigOption(name = "Selected hex color", desc = "Enter #RRGGBB, then press Set Color for each item.")
    @ConfigEditorText var selectedHex = AppearanceConfig.selectedHex
    private fun panel(id: String) = PanelAppearanceSettings(AppearanceConfig.panels.getValue(id)) { selectedHex }
    @JvmField @ConfigOption(name = "Miria contest HUD", desc = "Border and background.") @Accordion val miria = panel("miria")
    @JvmField @ConfigOption(name = "Progress HUD", desc = "Border and background.") @Accordion val progress = panel("progress")
    @JvmField @ConfigOption(name = "Missing panel HUD", desc = "Border and background.") @Accordion val missing = panel("missing")
    @JvmField @ConfigOption(name = "Biome captures HUD", desc = "Border and background.") @Accordion val captures = panel("captures")
    @JvmField @ConfigOption(name = "Nearby sparklings HUD", desc = "Border and background.") @Accordion val sparklings = panel("sparklings")
    @JvmField @ConfigOption(name = "Sparkling alert HUD", desc = "Border and background.") @Accordion val alert = panel("alert")
    @JvmField @ConfigOption(name = "All Gems alert HUD", desc = "") @Accordion val gems = panel("gems")
    @JvmField @ConfigOption(name = "All Bird Food alert HUD", desc = "") @Accordion val birdFood = panel("bird_food")
    @JvmField @ConfigOption(name = "All Incense alert HUD", desc = "") @Accordion val incense = panel("incense")
    @JvmField @ConfigOption(name = "Run completion alert HUD", desc = "") @Accordion val runComplete = panel("run_complete")
    @JvmField @ConfigOption(name = "Run completion text color", desc = "") @Accordion val runCompleteText = ColorChoice(
        { AppearanceConfig.runCompleteColor }, { AppearanceConfig.runCompleteColor = it }) { selectedHex }
    @JvmField @ConfigOption(name = "Warp reminder HUD", desc = "") @Accordion val warp = panel("warp")
    @JvmField @ConfigOption(name = "Bee nest waypoints", desc = "") @Accordion val beeNests = ColorChoice(
        { AppearanceConfig.nestColor }, { AppearanceConfig.nestColor = it }) { selectedHex }
    @JvmField @ConfigOption(name = "Snooper wall waypoints", desc = "") @Accordion val snooperWalls = ColorChoice(
        { AppearanceConfig.snooperColor }, { AppearanceConfig.snooperColor = it }) { selectedHex }
    @JvmField @ConfigOption(name = "Sparkling highlights and alerts", desc = "") @Accordion val sparklingHighlights = ColorChoice(
        { AppearanceConfig.sparklingColor }, { AppearanceConfig.sparklingColor = it }) { selectedHex }
    @JvmField @ConfigOption(name = "Floor drop ESP", desc = "") @Accordion val floor = ColorChoice(
        { SafariEspConfig.floorColor }, { SafariEspConfig.floorColor = it }) { selectedHex }
    @JvmField @ConfigOption(name = "Cavern critter colors", desc = "") @Accordion val cavern = CavernColorSettings { selectedHex }
    val cavernfish get() = cavern.cavernfish
    val flitter get() = cavern.flitter
    val shyworm get() = cavern.shyworm
    val driftling get() = cavern.driftling
    val chuckwalla get() = cavern.chuckwalla
    val rockmite get() = cavern.rockmite
    val rockmiteMound get() = cavern.rockmiteMound
    val scrappy get() = cavern.scrappy
    val snoozle get() = cavern.snoozle
    val gemzie get() = cavern.gemzie
    @JvmField @ConfigOption(name = "Forest critter colors", desc = "") @Accordion val forest = ForestColorSettings { selectedHex }
    val foxtrot get() = forest.foxtrot
    val bluebird get() = forest.bluebird
    val honeybug get() = forest.honeybug
    val treefrog get() = forest.treefrog
    val woodchucker get() = forest.woodchucker
    val fluffling get() = forest.fluffling
    val hideonfloor get() = forest.hideonfloor
    val parakeet get() = forest.parakeet
    val macaw get() = forest.macaw
    @JvmField @ConfigOption(name = "Icy critter colors", desc = "") @Accordion val icy = IcyColorSettings { selectedHex }
    val strongarm get() = icy.strongarm
    val tepid get() = icy.tepid
    val polaris get() = icy.polaris
    val shuddersquid get() = icy.shuddersquid
    val billygoat get() = icy.billygoat
    val mantisshrimp get() = icy.mantisshrimp
    val nozzlenose get() = icy.nozzlenose
    val troodon get() = icy.troodon
    val wumpa get() = icy.wumpa
    @JvmField @ConfigOption(name = "Haunted critter colors", desc = "") @Accordion val haunted = HauntedColorSettings { selectedHex }
    val areita get() = haunted.areita
    val bloodbat get() = haunted.bloodbat
    val duplico get() = haunted.duplico
    val gazer get() = haunted.gazer
    val litterbug get() = haunted.litterbug
    val solsnatcher get() = haunted.solsnatcher
    val gimmiegold get() = haunted.gimmiegold
    val hideonwall get() = haunted.hideonwall
    val hideyho get() = haunted.hideyho
    val doomspiral get() = haunted.doomspiral

    fun apply(): Boolean {
        val selectedChanged = AppearanceConfig.withHex(AppearanceConfig.color(0), selectedHex) != null &&
            AppearanceConfig.selectedHex != selectedHex
        if (selectedChanged) AppearanceConfig.selectedHex = selectedHex
        val changed = javaClass.fields.map { field ->
            when (val option = field.get(this)) {
                is ColorChoice -> option.apply()
                is PanelAppearanceSettings -> option.apply()
                is BiomeColorSettings -> option.apply()
                else -> false
            }
        }.any { it }
        return selectedChanged || changed
    }
}

open class BiomeColorSettings(private val selection: () -> String) {
    protected fun mob(name: String) = ColorChoice({ SafariEspConfig.mobs.getValue(name).color },
        { SafariEspConfig.mobs.getValue(name).color = it }, selection)
    fun apply(): Boolean = javaClass.fields.filter { it.type == ColorChoice::class.java }
        .map { (it.get(this) as ColorChoice).apply() }.any { it }
}

class CavernColorSettings(selection: () -> String) : BiomeColorSettings(selection) {
    @JvmField @ConfigOption(name = "Cavernfish ESP", desc = "") @Accordion val cavernfish = mob("Cavernfish")
    @JvmField @ConfigOption(name = "Flitter ESP", desc = "") @Accordion val flitter = mob("Flitter")
    @JvmField @ConfigOption(name = "Shyworm ESP", desc = "") @Accordion val shyworm = mob("Shyworm")
    @JvmField @ConfigOption(name = "Driftling ESP", desc = "") @Accordion val driftling = mob("Driftling")
    @JvmField @ConfigOption(name = "Chuckwalla ESP", desc = "") @Accordion val chuckwalla = mob("Chuckwalla")
    @JvmField @ConfigOption(name = "Rockmite silverfish ESP", desc = "") @Accordion val rockmite = mob("Rockmite")
    @JvmField @ConfigOption(name = "Rockmite mound ESP", desc = "") @Accordion val rockmiteMound = ColorChoice(
        { SafariEspConfig.rockmiteMoundColor }, { SafariEspConfig.rockmiteMoundColor = it }, selection)
    @JvmField @ConfigOption(name = "Scrappy ESP", desc = "") @Accordion val scrappy = mob("Scrappy")
    @JvmField @ConfigOption(name = "Snoozle ESP", desc = "") @Accordion val snoozle = mob("Snoozle")
    @JvmField @ConfigOption(name = "Gemzie ESP", desc = "") @Accordion val gemzie = mob("Gemzie")
}

class ForestColorSettings(selection: () -> String) : BiomeColorSettings(selection) {
    @JvmField @ConfigOption(name = "Foxtrot ESP", desc = "") @Accordion val foxtrot = mob("Foxtrot")
    @JvmField @ConfigOption(name = "Bluebird ESP", desc = "") @Accordion val bluebird = mob("Bluebird")
    @JvmField @ConfigOption(name = "Honeybug ESP", desc = "") @Accordion val honeybug = mob("Honeybug")
    @JvmField @ConfigOption(name = "Treefrog ESP", desc = "") @Accordion val treefrog = mob("Treefrog")
    @JvmField @ConfigOption(name = "Woodchucker ESP", desc = "") @Accordion val woodchucker = mob("Woodchucker")
    @JvmField @ConfigOption(name = "Fluffling ESP", desc = "") @Accordion val fluffling = mob("Fluffling")
    @JvmField @ConfigOption(name = "Hideonfloor ESP", desc = "") @Accordion val hideonfloor = mob("Hideonfloor")
    @JvmField @ConfigOption(name = "Parakeet ESP", desc = "") @Accordion val parakeet = mob("Parakeet")
    @JvmField @ConfigOption(name = "Macaw ESP", desc = "") @Accordion val macaw = mob("Macaw")
}

class IcyColorSettings(selection: () -> String) : BiomeColorSettings(selection) {
    @JvmField @ConfigOption(name = "Strongarm ESP", desc = "") @Accordion val strongarm = mob("Strongarm")
    @JvmField @ConfigOption(name = "Tepid ESP", desc = "") @Accordion val tepid = mob("Tepid")
    @JvmField @ConfigOption(name = "Polaris ESP", desc = "") @Accordion val polaris = mob("Polaris")
    @JvmField @ConfigOption(name = "Shuddersquid ESP", desc = "") @Accordion val shuddersquid = mob("Shuddersquid")
    @JvmField @ConfigOption(name = "Billygoat ESP", desc = "") @Accordion val billygoat = mob("Billygoat")
    @JvmField @ConfigOption(name = "Mantis Shrimp ESP", desc = "") @Accordion val mantisshrimp = mob("Mantis Shrimp")
    @JvmField @ConfigOption(name = "Nozzlenose ESP", desc = "") @Accordion val nozzlenose = mob("Nozzlenose")
    @JvmField @ConfigOption(name = "Troodon ESP", desc = "") @Accordion val troodon = mob("Troodon")
    @JvmField @ConfigOption(name = "Wumpa ESP", desc = "") @Accordion val wumpa = mob("Wumpa")
}

class HauntedColorSettings(selection: () -> String) : BiomeColorSettings(selection) {
    @JvmField @ConfigOption(name = "Areita ESP", desc = "") @Accordion val areita = mob("Areita")
    @JvmField @ConfigOption(name = "Bloodbat ESP", desc = "") @Accordion val bloodbat = mob("Bloodbat")
    @JvmField @ConfigOption(name = "Duplico ESP", desc = "") @Accordion val duplico = mob("Duplico")
    @JvmField @ConfigOption(name = "Gazer ESP", desc = "") @Accordion val gazer = mob("Gazer")
    @JvmField @ConfigOption(name = "Litterbug ESP", desc = "") @Accordion val litterbug = mob("Litterbug")
    @JvmField @ConfigOption(name = "Solsnatcher ESP", desc = "") @Accordion val solsnatcher = mob("Solsnatcher")
    @JvmField @ConfigOption(name = "Gimmiegold ESP", desc = "") @Accordion val gimmiegold = mob("Gimmiegold")
    @JvmField @ConfigOption(name = "Hideonwall ESP", desc = "") @Accordion val hideonwall = mob("Hideonwall")
    @JvmField @ConfigOption(name = "Hideyho ESP", desc = "") @Accordion val hideyho = mob("Hideyho")
    @JvmField @ConfigOption(name = "Doomspiral ESP", desc = "") @Accordion val doomspiral = mob("Doomspiral")
}
