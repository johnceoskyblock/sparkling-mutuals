package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.*
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.history.ui.HistoryScreen
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.ui.CatchCountScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier

/** MoulConfig view model; the existing properties files remain the saved configuration. */
class SafariSettings : Config() {
    @JvmField @Category(name = "General", desc = "") val general = General()
    @JvmField @Category(name = "Modes", desc = "") val modes = Modes()
    @JvmField @Category(name = "Party Interactions", desc = "") val party = Party()
    @JvmField @Category(name = "Miria contest", desc = "") val miria = Miria()
    @JvmField @Category(name = "History", desc = "") val history = History()
    @JvmField @Category(name = "Warp reminders", desc = "") val warp = Warp()
    @JvmField @Category(name = "Safari helpers", desc = "") val safari = Safari()
    @JvmField @Category(name = "Safari progress", desc = "") val tracking = Tracking()
    @JvmField @Category(name = "API key", desc = "") val api = Api()
    @JvmField @Category(name = "ESP", desc = "") val esp = SafariEspSettings()

    @JvmField @Category(name = "Customization", desc = "") val customization = CustomizationSettings()

    @JvmField @Category(name = "Misc Endgame Features", desc = "") val miscEndgame = MiscEndgame()

    class MiscEndgame {
        @JvmField @ConfigOption(name = "Phantomleaf Solver", desc = "Stand still for two notes. Green: one candidate; yellow: move to narrow the search.")
        @ConfigEditorBoolean var phantomleafSolver = ConfigManager.phantomleafSolver
    }
    class General {
        @JvmField @ConfigOption(name = "Command help", desc = "List commands in chat.")
        @ConfigEditorButton(runnableId = 3, buttonText = "Show help") var help = false
        @JvmField @ConfigOption(name = "Move and resize HUDs", desc = "Drag to move; scroll to resize.")
        @ConfigEditorButton(runnableId = 1, buttonText = "Edit all HUDs") var edit = false
    }
    class Modes {
        @JvmField @ConfigOption(name = "Unique mode", desc = "") @Accordion val unique = Unique()
        @JvmField @ConfigOption(name = "Full clear mode", desc = "") @Accordion val fullClear = FullClear()
        @JvmField @ConfigOption(name = "Sparkling mode", desc = "") @Accordion val sparkling = Sparkling()
        private var selected = SafariFullClear.mode
        fun selection(): SafariMode {
            val toggles = listOf(SafariMode.UNIQUE to unique.enabled, SafariMode.FULL_CLEAR to fullClear.enabled,
                SafariMode.SPARKLING to sparkling.enabled)
            return toggles.firstOrNull { (mode, enabled) -> enabled && mode != selected }?.first
                ?: selected.takeIf { mode -> toggles.any { it.first == mode && it.second } } ?: SafariMode.UNIQUE
        }
        fun refresh() {
            selected = SafariFullClear.mode
            unique.enabled = selected == SafariMode.UNIQUE
            fullClear.enabled = selected == SafariMode.FULL_CLEAR
            sparkling.enabled = selected == SafariMode.SPARKLING
        }
    }
    class FullClear {
        @JvmField @ConfigOption(name = "Hotspot announcer", desc = "Announce your hotspot before starting items.")
        @ConfigEditorBoolean var hotspot = ConfigManager.fullClearHotspotAnnouncer
        @JvmField @ConfigOption(name = "Full clear mode", desc = "Catch all spawns; one Macaw minimum.")
        @ConfigEditorBoolean var enabled = SafariFullClear.mode == SafariMode.FULL_CLEAR
    }
    class Sparkling {
        @JvmField @ConfigOption(name = "Hotspot announcer", desc = "Announce your hotspot before starting items.")
        @ConfigEditorBoolean var hotspot = ConfigManager.sparklingHotspotAnnouncer
        @JvmField @ConfigOption(name = "Sparkling Mode", desc = "Check sparklings your party needs.")
        @ConfigEditorBoolean var enabled = ConfigManager.sparklingMode
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "Highlight the /profits top 10, except Rockmite.")
        @ConfigEditorBoolean var profitable = ConfigManager.sparklingProfitableShardEsp
    }
    class Unique {
        @JvmField @ConfigOption(name = "Hotspot announcer", desc = "Announce your hotspot before starting items.")
        @ConfigEditorBoolean var hotspot = ConfigManager.uniqueHotspotAnnouncer
        @JvmField @ConfigOption(name = "Unique mode", desc = "Catch each type and check needed sparklings.")
        @ConfigEditorBoolean var enabled = SafariFullClear.mode == SafariMode.UNIQUE
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "Highlight the /profits top 10, except Rockmite.")
        @ConfigEditorBoolean var profitable = ConfigManager.profitableShardEsp
    }
    class Party {
        @JvmField @ConfigOption(name = "Announce starting quest items", desc = "List your starting items in party chat after leaving the boat.")
        @ConfigEditorBoolean var startingItems = ConfigManager.announceStartingItems
        @JvmField @ConfigOption(name = "Respond to party commands", desc = "Reply to supported !commands.")
        @ConfigEditorBoolean var enabled = ConfigManager.partyCommandsEnabled
        @JvmField @ConfigOption(name = "!dt downtime", desc = "Block entry until ready. Contest holds expire after 20s in Torrhus. Shift bypasses.")
        @ConfigEditorBoolean var downtime = ConfigManager.downtimeCommand

    }
    class Miria {
        @JvmField @ConfigOption(name = "Ticket mode", desc = "Announce tickets at Uncommon (Basic) or Legendary (First-Class, 5,000 points).")
        @ConfigEditorDropdown(values = ["Basic", "First-Class"])
        var ticketMode = ContestConfig.ticketMode
        @JvmField @ConfigOption(name = "Contest HUD and tracking", desc = "")
        @ConfigEditorBoolean var enabled = ContestConfig.trackContest
        @JvmField @ConfigOption(name = "Auto !dt contest", desc = "Party leaders send !dt contest when a contest starts.")
        @ConfigEditorBoolean var autoContestDowntime = ContestConfig.autoContestDowntime
        @JvmField @ConfigOption(name = "Honeyhive beacons", desc = "Show ready Torrhus hives until your contest target is met.")
        @ConfigEditorBoolean var honeyHives = ConfigManager.honeyHives
        @JvmField @ConfigOption(name = "Warning", desc = "") @Accordion val warning = Warning()
        var five
            get() = warning.five
            set(value) { warning.five = value }
        var three
            get() = warning.three
            set(value) { warning.three = value }
        var one
            get() = warning.one
            set(value) { warning.one = value }
        var titles
            get() = warning.titles
            set(value) { warning.titles = value }
        var volume
            get() = warning.volume
            set(value) { warning.volume = value }
        var sound
            get() = warning.sound
            set(value) { warning.sound = value }

    }
    class Warning {
        @JvmField @ConfigOption(name = "5 minute warning", desc = "Warn if below your ticket target.")
        @ConfigEditorBoolean var five = 5 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "3 minute warning", desc = "Warn if below your ticket target.")
        @ConfigEditorBoolean var three = 3 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "1 minute warning", desc = "Warn if below your ticket target.")
        @ConfigEditorBoolean var one = 1 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "No contest warnings", desc = "")
        @ConfigEditorButton(runnableId = 2, buttonText = "None") var none = false
        @JvmField @ConfigOption(name = "Warning titles", desc = "")
        @ConfigEditorBoolean var titles = ContestConfig.contestWarnTitle
        @JvmField @ConfigOption(name = "Sound volume", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ContestConfig.contestSoundVolume.coerceIn(0, 100)
        @JvmField @ConfigOption(name = "Warning sound", desc = "Minecraft sound ID.")
        @ConfigEditorText var sound = ContestConfig.contestSound
        @JvmField @ConfigOption(name = "Save sound", desc = "")
        @ConfigEditorButton(runnableId = 7, buttonText = "Save sound") var saveSound = false
    }
    class History {
        @JvmField @ConfigOption(name = "History", desc = "Runs, bests, sparklings, achievements and stats.")
        @ConfigEditorButton(runnableId = 10, buttonText = "History") var open = false
        @JvmField @ConfigOption(name = "Import History from Logs", desc = "Add missing history and repair log-backed records. Backups are kept.")
        @ConfigEditorButton(runnableId = 11, buttonText = "Import History from Logs") var importLogs = false
        @JvmField @ConfigOption(name = "Save runs", desc = "Save Safari runs to history.")
        @ConfigEditorBoolean var runs = ConfigManager.runHistory
        @JvmField @ConfigOption(name = "Coins at insta-sell", desc = "Use insta-sell prices instead of sell offers.")
        @ConfigEditorBoolean var instaSell = ConfigManager.runCoinsInstaSell
        @JvmField @ConfigOption(name = "Achievements", desc = "")
        @ConfigEditorBoolean var achievements = ConfigManager.achievements
        @JvmField @ConfigOption(name = "Achievement sound", desc = "")
        @ConfigEditorBoolean var sound = ConfigManager.achievementSound
        @JvmField @ConfigOption(name = "Achievements to party", desc = "Announce unlocks in party chat.")
        @ConfigEditorBoolean var toParty = ConfigManager.achievementsToParty
    }
    class Warp {
        @JvmField @ConfigOption(name = "Warp reminder", desc = "Alert and /p warp after the delay, before entry closes.")
        @ConfigEditorBoolean var enabled = ConfigManager.warpAlertsEnabled
        @JvmField @ConfigOption(name = "Delay in seconds", desc = "1–86,400 seconds.")
        @ConfigEditorText var delay = ConfigManager.warpDelaySeconds.toString()
        @JvmField @ConfigOption(name = "Save delay", desc = "")
        @ConfigEditorButton(runnableId = 6, buttonText = "Save delay") var saveDelay = false
        @JvmField @ConfigOption(name = "Default delay", desc = "Reset to 25 seconds.")
        @ConfigEditorButton(runnableId = 5, buttonText = "Reset to 25s") var reset = false
    }
    class Safari {
        @JvmField @ConfigOption(name = "Safari entry", desc = "") @Accordion val entry = SafariEntrySettings()
        @JvmField @ConfigOption(name = "Capture tools", desc = "") @Accordion val capture = SafariCaptureSettings()
        @JvmField @ConfigOption(name = "Biome interactions", desc = "") @Accordion val interactions = SafariInteractionSettings()
        @JvmField @ConfigOption(name = "Party visibility", desc = "") @Accordion val teammates = SafariTeammateSettings()
        @JvmField @ConfigOption(name = "Visual cleanup", desc = "") @Accordion val visibility = SafariVisibilitySettings()
        @JvmField @ConfigOption(name = "Detection and alerts", desc = "") @Accordion val alerts = Alerts()
        var holdManagerOnKick
            get() = entry.holdManagerOnKick
            set(value) { entry.holdManagerOnKick = value }
        var autoSelectTicket
            get() = entry.autoSelectTicket
            set(value) { entry.autoSelectTicket = value }
        var ticketPreference
            get() = entry.ticketPreference
            set(value) { entry.ticketPreference = value }
        var experimentalCapsulePath
            get() = capture.experimentalCapsulePath
            set(value) { capture.experimentalCapsulePath = value }
        var capsulePathWidth
            get() = capture.capsulePathWidth
            set(value) { capture.capsulePathWidth = value }
        var showCapsuleGuideThroughTerrain
            get() = capture.showCapsuleGuideThroughTerrain
            set(value) { capture.showCapsuleGuideThroughTerrain = value }
        var autoClicker
            get() = capture.autoClicker
            set(value) { capture.autoClicker = value }
        var gemzieGemAutoSelect
            get() = interactions.gemzieGemAutoSelect
            set(value) { interactions.gemzieGemAutoSelect = value }
        var candleHitbox
            get() = interactions.candleHitbox
            set(value) { interactions.candleHitbox = value }
        var hideyhoAutoAccept
            get() = interactions.hideyhoAutoAccept
            set(value) { interactions.hideyhoAutoAccept = value }
        var hideyhoInsideOutside
            get() = interactions.hideyhoInsideOutside
            set(value) { interactions.hideyhoInsideOutside = value }
        var highlightTeammates
            get() = teammates.highlightTeammates
            set(value) { teammates.highlightTeammates = value }
        var showTeammateNames
            get() = teammates.showTeammateNames
            set(value) { teammates.showTeammateNames = value }
        var hideCaptureChat
            get() = visibility.hideCaptureChat
            set(value) { visibility.hideCaptureChat = value }
        var hideGroundCapsules
            get() = visibility.hideGroundCapsules
            set(value) { visibility.hideGroundCapsules = value }
        var hideFlyingCapsules
            get() = visibility.hideFlyingCapsules
            set(value) { visibility.hideFlyingCapsules = value }
        var capsuleHideDistance
            get() = visibility.capsuleHideDistance
            set(value) { visibility.capsuleHideDistance = value }
        var paintings
            get() = visibility.paintings
            set(value) { visibility.paintings = value }
        var darkness
            get() = visibility.darkness
            set(value) { visibility.darkness = value }
        var shiny
            get() = alerts.shiny
            set(value) { alerts.shiny = value }
        var alert
            get() = alerts.alert
            set(value) { alerts.alert = value }
        var announce
            get() = alerts.announce
            set(value) { alerts.announce = value }
    }
    class SafariEntrySettings {
        @JvmField @ConfigOption(name = "Guard Safari entry", desc = "Wait for missing party members and kick updates. Shift bypasses.")
        @ConfigEditorBoolean var holdManagerOnKick = ConfigManager.holdManagerOnKick
        @JvmField @ConfigOption(name = "Auto-select Safari ticket", desc = "Non-leaders: select a ticket after Safari entry. Entry guard applies.")
        @ConfigEditorBoolean var autoSelectTicket = ConfigManager.autoSelectSafariTicket
        @JvmField @ConfigOption(name = "Preferred ticket", desc = "Fall back to the nearest available ticket tier.")
        @ConfigEditorDropdown(values = ["First Class", "Basic"])
        var ticketPreference = ConfigManager.safariTicketPreference
    }
    class SafariCaptureSettings {
        @JvmField @ConfigOption(name = "Capture projections", desc = "Show capsule paths and estimated hits. Turn view bobbing off.")
        @ConfigEditorBoolean var experimentalCapsulePath = ConfigManager.experimentalCapsulePath
        @JvmField @ConfigOption(name = "Capsule path width", desc = "")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f) var capsulePathWidth = ConfigManager.capsulePathWidth
        @JvmField @ConfigOption(name = "Show guide through terrain", desc = "See the capsule guide through walls.")
        @ConfigEditorBoolean var showCapsuleGuideThroughTerrain = ConfigManager.showCapsuleGuideThroughTerrain
        @JvmField @ConfigOption(name = "Auto Clicker", desc = "Hold left-click on Rockmite mounds for 12 CPS.")
        @ConfigEditorBoolean var autoClicker = ConfigManager.autoClicker
    }
    class SafariInteractionSettings {
        @JvmField @ConfigOption(name = "Gemzie gem auto-select", desc = "Select and place the matching hotbar gem when clicking a podium.")
        @ConfigEditorBoolean var gemzieGemAutoSelect = ConfigManager.gemzieGemAutoSelect
        @JvmField @ConfigOption(name = "Full candle hitbox", desc = "Larger candle hitboxes while holding Soothing Incense.")
        @ConfigEditorBoolean var candleHitbox = ConfigManager.candleHitbox
        @JvmField @ConfigOption(name = "Auto-accept Hideyho", desc = "Accept the quest without opening chat.")
        @ConfigEditorBoolean var hideyhoAutoAccept = ConfigManager.hideyhoAutoAccept
        @JvmField @ConfigOption(name = "Hideyho inside/outside", desc = "Show the hiding location in Unique and Full Clear modes.")
        @ConfigEditorBoolean var hideyhoInsideOutside = ConfigManager.hideyhoInsideOutside
    }
    class SafariTeammateSettings {
        @JvmField @ConfigOption(name = "Highlight teammates", desc = "Outline loaded party members in SkyBlock.")
        @ConfigEditorBoolean var highlightTeammates = ConfigManager.highlightTeammates
        @JvmField @ConfigOption(name = "Show teammate names", desc = "Show party names through walls in SkyBlock.")
        @ConfigEditorBoolean var showTeammateNames = ConfigManager.showTeammateNames
    }
    class SafariVisibilitySettings {
        @JvmField @ConfigOption(name = "Clear water", desc = "See through water from above or below. Safari only.")
        @ConfigEditorBoolean var clearWater = ConfigManager.clearWater
        @JvmField @ConfigOption(name = "Water opacity", desc = "0% transparent; 100% opaque.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var waterOpacity = ConfigManager.waterOpacity
        @JvmField @ConfigOption(name = "Hide useless chats in safari", desc = "Hide routine capture and critter messages. Tracking stays active.")
        @ConfigEditorBoolean var hideCaptureChat = ConfigManager.hideCaptureChat
        @JvmField @ConfigOption(name = "Hide capsules on ground", desc = "")
        @ConfigEditorBoolean var hideGroundCapsules = ConfigManager.hideGroundCapsules
        @JvmField @ConfigOption(name = "Hide flying capsules", desc = "Hide flying capsules near your camera.")
        @ConfigEditorBoolean var hideFlyingCapsules = ConfigManager.hideFlyingCapsules
        @JvmField @ConfigOption(name = "Flying capsule distance", desc = "Distance in blocks.")
        @ConfigEditorSlider(minValue = .5f, maxValue = 6f, minStep = .5f)
        var capsuleHideDistance = ConfigManager.capsuleHideDistance
        @JvmField @ConfigOption(name = "Hide Haunted paintings", desc = "")
        @ConfigEditorBoolean var paintings = ConfigManager.hideHauntedPaintings
        @JvmField @ConfigOption(name = "Remove darkness", desc = "Safari only.")
        @ConfigEditorBoolean var darkness = ConfigManager.removeDarkness
    }
    class Alerts {
        @JvmField @ConfigOption(name = "Nearby shiny detection", desc = "Show detected sparklings within 80 blocks in your biome.")
        @ConfigEditorBoolean var shiny = ConfigManager.shinyDetection
        @JvmField @ConfigOption(name = "Sparkling alert", desc = "Show an on-screen detection alert.")
        @ConfigEditorBoolean var alert = ConfigManager.sparklingAlert
        @JvmField @ConfigOption(name = "Announce sparklings to party", desc = "Send the critter, biome and coordinates.")
        @ConfigEditorBoolean var announce = ConfigManager.sparklingPartyAnnouncer
        @JvmField @ConfigOption(name = "Sparkling sound", desc = "Minecraft sound ID.")
        @ConfigEditorText var sound = ConfigManager.sparklingSound
        @JvmField @ConfigOption(name = "Sparkling sound volume", desc = "0 mutes.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ConfigManager.sparklingSoundVolume
        @JvmField @ConfigOption(name = "Save sparkling sound", desc = "Apply and preview.")
        @ConfigEditorButton(runnableId = 9, buttonText = "Save sound") var saveSound = false
        @JvmField @ConfigOption(name = "All Gems Alert", desc = "One of each gem in inventory.")
        @ConfigEditorBoolean var gems = ConfigManager.allGemsAlert
        @JvmField @ConfigOption(name = "All Bird Food Alert", desc = "Three of each Forest bird food collected.")
        @ConfigEditorBoolean var birdFood = ConfigManager.allBirdFoodAlert
        @JvmField @ConfigOption(name = "All Incense Alert", desc = "Four incense in inventory.")
        @ConfigEditorBoolean var incense = ConfigManager.allIncenseAlert
        @JvmField @ConfigOption(name = "Run completion alert", desc = "Alert when all biomes are complete for your mode.")
        @ConfigEditorBoolean var runComplete = ConfigManager.runCompleteAlert
        @JvmField @ConfigOption(name = "Run completion title", desc = "")
        @ConfigEditorText var runTitle = ConfigManager.runCompleteTitle
        @JvmField @ConfigOption(name = "Run completion sound", desc = "Minecraft sound ID.")
        @ConfigEditorText var runSound = ConfigManager.runCompleteSound
        @JvmField @ConfigOption(name = "Run completion volume", desc = "0 mutes.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var runVolume = ConfigManager.runCompleteVolume
    }
    class Tracking {
        @JvmField @ConfigOption(name = "Progress HUD", desc = "Completion and run time.")
        @ConfigEditorBoolean var progress = ConfigManager.progressHud
        @JvmField @ConfigOption(name = "Show where", desc = "")
        @ConfigEditorDropdown(values = ["Only in Safari", "Safari and entrance", "Everywhere"])
        var where = ConfigManager.showWhere
        @JvmField @ConfigOption(name = "Missing / Unchecked panel", desc = "Show remaining catches or checks in your biome.")
        @ConfigEditorBoolean var missing = ConfigManager.missingPanel
        @JvmField @ConfigOption(name = "Capture count screen", desc = "Current or last run.")
        @ConfigEditorButton(runnableId = 8, buttonText = "View captures") var view = false

    }
    class Api {
        @JvmField @ConfigOption(name = "Use Shiny Hunter cloud API", desc = "No personal key needed. Your own key takes priority.")
        @ConfigEditorBoolean var cloud = ConfigManager.useCloudApi
        @JvmField @ConfigOption(name = "Hypixel API key", desc = "Optional when the cloud API is enabled.")
        @ConfigEditorButton(runnableId = 4, buttonText = "Edit API key") var edit = false
    }

    override fun getTitle(): StructuredText = StructuredText.of("Sparkling Mutuals · Safari").withColour(SafariTheme.ACCENT)
    override fun formatCategoryName(category: ProcessedCategory, selected: Boolean): StructuredText =
        category.displayName.copyShallow().withColour(if (selected) SafariTheme.ACCENT else SafariTheme.MUTED)

    private fun feedback(text: String) { Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component("[SM] $text")) }
    override fun executeRunnable(id: Int) {
        val client = Minecraft.getInstance()
        when (id) {
            1 -> { apply(); client.execute { client.smSetScreen(ContestGui(client.smScreen)) } }
            2 -> { miria.five = false; miria.three = false; miria.one = false; apply() }
            3 -> client.execute {
                client.smSetScreen(ChatScreen("", false))
                client.player?.sendSystemMessage(LocalChat.component(CommandHelp.localLines().joinToString("\n")))
            }
            4 -> client.execute { client.smSetScreen(ApiKeyScreen(client.smScreen)) }
            5 -> { warp.delay = "25"; saveDelay() }
            6 -> saveDelay()
            7 -> saveSound()
            8 -> { apply(); client.execute { client.smSetScreen(CatchCountScreen(client.smScreen)) } }
            9 -> { apply(); if (saveSparklingSound()) SparklingEncounters.playSound(client) }
            10 -> { apply(); client.execute { client.smSetScreen(HistoryScreen(client.smScreen)) } }
            11 -> { apply(); client.execute {
                client.smSetScreen(null)
                LogImporter.runImport(LogImporter.defaultLogs())
            } }
        }
    }
    override fun isValidRunnable(id: Int) = id in 1..11
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
    private fun saveSparklingSound(): Boolean {
        val sound = safari.alerts.sound.trim()
        if (Identifier.tryParse(sound) == null) { feedback("Enter a valid Minecraft sound identifier."); return false }
        if (ConfigManager.sparklingSound != sound) { ConfigManager.sparklingSound = sound; ConfigManager.save() }
        return true
    }
    fun saveTextFields() {
        saveDelay(); saveSound(); saveSparklingSound()
        val sound = Identifier.tryParse(safari.alerts.runSound.trim())
        if (sound == null) feedback("Enter a valid Minecraft sound identifier for run completion.")
        else ConfigManager.runCompleteSound = sound.toString()
        ConfigManager.runCompleteTitle = safari.alerts.runTitle.take(120).ifBlank { "Run Complete" }
        ConfigManager.save()
    }
    fun apply(notifyMode: Boolean = false) {
        if (warp.enabled != ConfigManager.warpAlertsEnabled) AlertManager.toggleAlert()
        val changes = listOf(ConfigManager::announceStartingItems to party.startingItems, ConfigManager::uniqueHotspotAnnouncer to modes.unique.hotspot, ConfigManager::fullClearHotspotAnnouncer to modes.fullClear.hotspot, ConfigManager::sparklingHotspotAnnouncer to modes.sparkling.hotspot, ConfigManager::phantomleafSolver to miscEndgame.phantomleafSolver, ConfigManager::highlightTeammates to safari.highlightTeammates, ConfigManager::showTeammateNames to safari.showTeammateNames, ConfigManager::profitableShardEsp to modes.unique.profitable, ConfigManager::sparklingProfitableShardEsp to modes.sparkling.profitable, ConfigManager::partyCommandsEnabled to party.enabled, ConfigManager::downtimeCommand to party.downtime, ConfigManager::useCloudApi to api.cloud, ConfigManager::hideHauntedPaintings to safari.paintings,
            ConfigManager::shinyDetection to safari.shiny, ConfigManager::hideyhoAutoAccept to safari.hideyhoAutoAccept, ConfigManager::autoSelectSafariTicket to safari.autoSelectTicket, ConfigManager::hideyhoInsideOutside to safari.hideyhoInsideOutside,
            ConfigManager::progressHud to tracking.progress,
            ConfigManager::experimentalCapsulePath to safari.experimentalCapsulePath,
            ConfigManager::gemzieGemAutoSelect to safari.gemzieGemAutoSelect,
            ConfigManager::showCapsuleGuideThroughTerrain to safari.showCapsuleGuideThroughTerrain,
            ConfigManager::missingPanel to tracking.missing,
            ConfigManager::removeDarkness to safari.darkness,
            ConfigManager::clearWater to safari.visibility.clearWater,
            ConfigManager::sparklingAlert to safari.alert, ConfigManager::sparklingPartyAnnouncer to safari.announce,
            ConfigManager::autoClicker to safari.autoClicker, ConfigManager::candleHitbox to safari.candleHitbox, ConfigManager::holdManagerOnKick to safari.holdManagerOnKick, ConfigManager::honeyHives to miria.honeyHives, ConfigManager::hideCaptureChat to safari.hideCaptureChat, ConfigManager::runHistory to history.runs, ConfigManager::achievements to history.achievements, ConfigManager::achievementSound to history.sound, ConfigManager::achievementsToParty to history.toParty, ConfigManager::runCoinsInstaSell to history.instaSell,
            ConfigManager::allGemsAlert to safari.alerts.gems, ConfigManager::allBirdFoodAlert to safari.alerts.birdFood,
            ConfigManager::allIncenseAlert to safari.alerts.incense, ConfigManager::runCompleteAlert to safari.alerts.runComplete,
            ConfigManager::hideGroundCapsules to safari.hideGroundCapsules, ConfigManager::hideFlyingCapsules to safari.hideFlyingCapsules)
        val espChanged = esp.apply()
        val customizationChanged = customization.apply()
        val distance = ConfigManager.validCapsuleDistance(safari.capsuleHideDistance)
        val pathWidth = ConfigManager.validCapsulePathWidth(safari.capsulePathWidth)
        val generalChanged = ConfigManager.waterOpacity != safari.visibility.waterOpacity.coerceIn(0, 100) || ConfigManager.capsulePathWidth != pathWidth || espChanged || customizationChanged || changes.any { (property, value) -> property.get() != value } ||
            ConfigManager.safariTicketPreference != safari.ticketPreference || ConfigManager.showWhere != tracking.where || ConfigManager.capsuleHideDistance != distance ||
            ConfigManager.runCompleteVolume != safari.alerts.runVolume.coerceIn(0, 100) || ConfigManager.sparklingSoundVolume != safari.alerts.volume.coerceIn(0, 100)
        changes.forEach { (property, value) -> property.set(value) }
        ConfigManager.safariTicketPreference = safari.ticketPreference.coerceIn(0, 1)
        ConfigManager.showWhere = tracking.where.coerceIn(0, 2)
        ConfigManager.capsuleHideDistance = distance
        ConfigManager.waterOpacity = safari.visibility.waterOpacity
        ConfigManager.capsulePathWidth = pathWidth
        ConfigManager.runCompleteVolume = safari.alerts.runVolume.coerceIn(0, 100)
        ConfigManager.sparklingSoundVolume = safari.alerts.volume.coerceIn(0, 100)
        if (generalChanged) ConfigManager.save()
        val warnings = listOfNotNull(5.takeIf { miria.five }, 3.takeIf { miria.three }, 1.takeIf { miria.one })
        val contestChanged = ContestConfig.ticketMode != miria.ticketMode || ContestConfig.autoContestDowntime != miria.autoContestDowntime || ContestConfig.trackContest != miria.enabled || ContestConfig.contestWarnTitle != miria.titles ||
            ContestConfig.contestSoundVolume != miria.volume || ContestConfig.warningMinutes() != warnings
        ContestConfig.trackContest = miria.enabled
        ContestConfig.ticketMode = miria.ticketMode.coerceIn(0, 1)
        ContestConfig.autoContestDowntime = miria.autoContestDowntime
        ContestConfig.contestWarnTitle = miria.titles
        ContestConfig.contestSoundVolume = miria.volume.coerceIn(0, 100)
        ContestConfig.contestWarnMinutes = warnings.joinToString(", ")
        ContestConfig.contestWarnEnabled = warnings.isNotEmpty()
        if (contestChanged) ContestConfig.save()
        val selection = modes.selection()
        if (selection != SafariFullClear.mode) {
            SafariFullClear.select(selection)
            if (notifyMode) SafariFullClear.modeLines().forEach {
                Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component(it))
            }
        }
        modes.refresh()
    }
}
