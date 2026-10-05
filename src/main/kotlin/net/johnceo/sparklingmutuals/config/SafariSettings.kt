package net.johnceo.sparklingmutuals.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.*
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.commands.*
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** MoulConfig view model; the existing properties files remain the saved configuration. */
class SafariSettings : Config() {
    @JvmField @Category(name = "Party commands", desc = "Responses and help") val party = Party()
    @JvmField @Category(name = "Miria contest", desc = "HUD and warnings") val miria = Miria()
    @JvmField @Category(name = "Warp reminders", desc = "Hotspot reminders") val warp = Warp()
    @JvmField @Category(name = "Safari helpers", desc = "Paintings and sparklings") val safari = Safari()
    @JvmField @Category(name = "API key", desc = "Profile lookup authentication") val api = Api()

    class Party {
        @JvmField @ConfigOption(name = "Respond to party commands", desc = "Updated mod clients coordinate so one handles each request. All mod users should update.")
        @ConfigEditorBoolean var enabled = ConfigManager.partyCommandsEnabled
        @JvmField @ConfigOption(name = "Command help", desc = "List commands locally. Party members can also use !commands.")
        @ConfigEditorButton(runnableId = 3, buttonText = "Show help") var help = false
    }
    class Miria {
        @JvmField @ConfigOption(name = "Contest HUD and tracking", desc = "Track Miria contests. Agatha is excluded.")
        @ConfigEditorBoolean var enabled = ContestConfig.trackContest
        @JvmField @ConfigOption(name = "5 minute warning", desc = "Warn if Uncommon has not been reached.")
        @ConfigEditorBoolean var five = 5 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "3 minute warning", desc = "Warn if Uncommon has not been reached.")
        @ConfigEditorBoolean var three = 3 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "1 minute warning", desc = "Warn if Uncommon has not been reached.")
        @ConfigEditorBoolean var one = 1 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "No contest warnings", desc = "Turn all three warning times off.")
        @ConfigEditorButton(runnableId = 2, buttonText = "None") var none = false
        @JvmField @ConfigOption(name = "Warning titles", desc = "Show a title as well as a chat warning.")
        @ConfigEditorBoolean var titles = ContestConfig.contestWarnTitle
        @JvmField @ConfigOption(name = "Sound volume", desc = "Warning sound volume in percent.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ContestConfig.contestSoundVolume.coerceIn(0, 100)
        @JvmField @ConfigOption(name = "Warning sound", desc = "Minecraft sound identifier. Saved on close or with Save sound.")
        @ConfigEditorText var sound = ContestConfig.contestSound
        @JvmField @ConfigOption(name = "Save sound", desc = "Apply the sound after editing.")
        @ConfigEditorButton(runnableId = 7, buttonText = "Save sound") var saveSound = false
        @JvmField @ConfigOption(name = "Move and resize HUD", desc = "Drag to move; scroll to resize.")
        @ConfigEditorButton(runnableId = 1, buttonText = "Edit HUD") var edit = false
    }
    class Warp {
        @JvmField @ConfigOption(name = "Warp reminder", desc = "Remind you to /p warp after a Hotspot message. Requires the Hotspot perk.")
        @ConfigEditorBoolean var enabled = ConfigManager.warpAlertsEnabled
        @JvmField @ConfigOption(name = "Delay in seconds", desc = "Default: 25. Enter 1 to 86400. Saved on close or with Save delay.")
        @ConfigEditorText var delay = ConfigManager.warpDelaySeconds.toString()
        @JvmField @ConfigOption(name = "Save delay", desc = "Apply the delay after editing.")
        @ConfigEditorButton(runnableId = 6, buttonText = "Save delay") var saveDelay = false
        @JvmField @ConfigOption(name = "Default delay", desc = "Reset to 25 seconds.")
        @ConfigEditorButton(runnableId = 5, buttonText = "Reset to 25s") var reset = false
    }
    class Safari {
        @JvmField @ConfigOption(name = "Hideyho quest clicks", desc = "With chat open, left-click anywhere to accept Hideyho's current Sure option. One click, one acceptance. Disabled by default.")
        @ConfigEditorBoolean var hideyho = ConfigManager.hideyhoQuestClicks
        @JvmField @ConfigOption(name = "Hide Haunted paintings", desc = "Hide paintings only in the Haunted Safari area. They remain in the world and hittable.")
        @ConfigEditorBoolean var paintings = ConfigManager.hideHauntedPaintings
        @JvmField @ConfigOption(name = "Nearby shiny detection", desc = "Gold highlights and name/distance within 80 blocks in your biome. Uses CritterMod 0.9.0 loaded nametags.")
        @ConfigEditorBoolean var shiny = ConfigManager.shinyDetection
    }
    class Api {
        @JvmField @ConfigOption(name = "Hypixel API key", desc = "Open a masked key editor. Missing/invalid keys, unavailable data and request limits have distinct errors.")
        @ConfigEditorButton(runnableId = 4, buttonText = "Edit API key") var edit = false
    }

    override fun getTitle(): StructuredText = StructuredText.of("Sparkling Mutuals · Safari").withColour(0xE3BB67)
    override fun formatCategoryName(category: ProcessedCategory, selected: Boolean): StructuredText =
        category.displayName.copyShallow().withColour(if (selected) 0xE3BB67 else 0xB5C4A1)

    private fun feedback(text: String) { Minecraft.getInstance().player?.sendSystemMessage(Component.literal("[SM] $text")) }
    override fun executeRunnable(id: Int) {
        val client = Minecraft.getInstance()
        when (id) {
            1 -> { apply(); client.execute { client.setScreen(ContestGui(client.screen)) } }
            2 -> { miria.five = false; miria.three = false; miria.one = false; apply() }
            3 -> feedback(SafariLookup.run(PartyCommand(PartyCommandKind.HELP), emptyList()).removePrefix("[SM] "))
            4 -> client.execute { client.setScreen(ApiKeyScreen(client.screen)) }
            5 -> { warp.delay = "25"; saveDelay() }
            6 -> saveDelay()
            7 -> saveSound()
        }
    }
    override fun isValidRunnable(id: Int) = id in 1..7
    private fun saveDelay() {
        val seconds = warp.delay.trim().toIntOrNull()
        if (seconds == null || seconds !in 1..86400) feedback("Enter a delay from 1 to 86400 seconds.")
        else if (seconds != ConfigManager.warpDelaySeconds) AlertManager.setDelay(seconds)
    }
    private fun saveSound() {
        val sound = miria.sound.trim()
        if (Identifier.tryParse(sound) == null) feedback("Enter a valid Minecraft sound identifier.")
        else if (ContestConfig.contestSound != sound) { ContestConfig.contestSound = sound; ContestConfig.save() }
    }
    fun saveTextFields() { saveDelay(); saveSound() }
    fun apply() {
        if (warp.enabled != ConfigManager.warpAlertsEnabled) AlertManager.toggleAlert()
        val generalChanged = party.enabled != ConfigManager.partyCommandsEnabled ||
            safari.paintings != ConfigManager.hideHauntedPaintings || safari.shiny != ConfigManager.shinyDetection ||
            safari.hideyho != ConfigManager.hideyhoQuestClicks
        ConfigManager.partyCommandsEnabled = party.enabled
        ConfigManager.hideHauntedPaintings = safari.paintings
        ConfigManager.shinyDetection = safari.shiny
        ConfigManager.hideyhoQuestClicks = safari.hideyho
        if (generalChanged) ConfigManager.save()
        val warnings = listOfNotNull(5.takeIf { miria.five }, 3.takeIf { miria.three }, 1.takeIf { miria.one })
        val contestChanged = ContestConfig.trackContest != miria.enabled || ContestConfig.contestWarnTitle != miria.titles ||
            ContestConfig.contestSoundVolume != miria.volume || ContestConfig.warningMinutes() != warnings
        ContestConfig.trackContest = miria.enabled
        ContestConfig.contestWarnTitle = miria.titles
        ContestConfig.contestSoundVolume = miria.volume.coerceIn(0, 100)
        ContestConfig.contestWarnMinutes = warnings.joinToString(", ")
        ContestConfig.contestWarnEnabled = warnings.isNotEmpty()
        if (contestChanged) ContestConfig.save()
    }
}
