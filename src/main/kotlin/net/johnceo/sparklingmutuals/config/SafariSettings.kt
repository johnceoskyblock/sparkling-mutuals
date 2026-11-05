package net.johnceo.sparklingmutuals.config

import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.alerts.AlertManager
import net.johnceo.sparklingmutuals.commands.CommandHelp
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.history.ui.HistoryScreen
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.ui.CatchCountScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier

/** Settings view model; the existing properties files remain the saved configuration. */
class SafariSettings {
    @JvmField @Category(name = "General", desc = "") val general = General()
    @JvmField @Category(name = "Modes", desc = "") val modes = Modes()
    @JvmField @Category(name = "Party Interactions", desc = "") val party = Party()
    @JvmField @Category(name = "Miria contest", desc = "") val miria = Miria()
    @JvmField @Category(name = "History", desc = "") val history = History()
    @JvmField @Category(name = "Sparkle Warping", desc = "") val warp = Warp()
    @JvmField @Category(name = "Safari helpers", desc = "") val safari = Safari()
    @JvmField @Category(name = "HUDs", desc = "") val tracking = Tracking()
    @JvmField @Category(name = "API key", desc = "") val api = Api()
    @JvmField @Category(name = "ESP", desc = "") val esp = SafariEspSettings()

    @JvmField @Category(name = "Customization", desc = "") val customization = CustomizationSettings()

    @JvmField @Category(name = "SkyBlock", desc = "") val skyblock = SkyBlock()
    @JvmField @Category(name = "Banner", desc = "") val banner = BannerSettings()
    @JvmField @Category(name = "Mode-wide Critter Toggles", desc = "")
    val modWideCritters = CritterToggleSettings(SafariEspConfig::modWideEnabled)
    @JvmField @Category(name = "Profitable Critter Toggles", desc = "")
    val profitableCritters = CritterToggleSettings { SafariEspConfig.profitableToggles.getValue(it) }
    @JvmField @Category(name = "Always On Toggle", desc = "")
    val alwaysOnCritters = CritterToggleSettings(SafariEspConfig::alwaysOn)
    @JvmField @Category(name = "Floor Drop Always On Toggle", desc = "")
    val alwaysOnFloorDrops = FloorDropToggleSettings()
    init {
        esp.critterColors = customization.critters
        esp.waypointColors = customization.waypoints
        safari.alerts.appearance = customization.alert
        safari.alerts.nearbyAppearance = customization.huds.sparklings
        safari.capture.doomAppearance = customization.huds.doomThrow
        tracking.progressHud.appearance=customization.huds.progress
        tracking.missingHud.appearance=customization.huds.missing
        tracking.objectivesHud.appearance=customization.huds.objectives
        tracking.capturesHud.appearance=customization.huds.captures
        tracking.contestHud.appearance=customization.huds.miria
    }
    val miscEndgame get() = skyblock.endgame
    class SkyBlock {
        @JvmField @ConfigOption(name = "Helpers", desc = "") @Accordion val helpers = SkyBlockHelpers()
        @JvmField @ConfigOption(name = "Endgame", desc = "") @Accordion val endgame = MiscEndgame()
    }
    class SkyBlockHelpers {
        @JvmField @ConfigOption(name = "Shard sale guidance (experimental)", desc = "Hunting Box recent-price ranges and observed sell-offer alerts. Not a forecast.")
        @ConfigEditorBoolean var shardSaleGuidance = ConfigManager.shardSaleGuidance
        @JvmField @ConfigOption(name = "Skills overflow", desc = "Virtual levels in Skills menu tooltips.")
        @ConfigEditorBoolean var skillOverflow = ConfigManager.skillOverflow
        @JvmField @ConfigOption(name = "Overflow level-up messages", desc = "")
        @ConfigEditorBoolean var skillOverflowChat = ConfigManager.skillOverflowChat
        @JvmField @ConfigOption(name = "Use numbers for skill levels", desc = "Show 61 instead of LXI.")
        @ConfigEditorBoolean var skillOverflowNumbers = ConfigManager.skillOverflowNumbers
        @JvmField var estimatedItemValue = ConfigManager.estimatedItemValue
        @JvmField var estimatedChestValue = ConfigManager.estimatedChestValue
        @JvmField @ConfigOption(name = "Hunting Box total value", desc = "One estimated sell-offer total across visited pages.")
        @ConfigEditorBoolean var huntingBoxValue = ConfigManager.huntingBoxValue
    }

    class MiscEndgame {
        @JvmField @ConfigOption(name = "Phantomleaf Solver", desc = "Stand still for two notes; move if yellow.")
        @ConfigEditorBoolean var phantomleafSolver = ConfigManager.phantomleafSolver
    }
    class General {
        @JvmField @ConfigOption(name = "Command help", desc = "")
        @ConfigEditorButton(runnableId = 3, buttonText = "Show help") var help = false
        @JvmField @ConfigOption(name = "Move and resize HUDs", desc = "Drag to move; scroll to resize.")
        @ConfigEditorButton(runnableId = 1, buttonText = "Edit HUDs") var edit = false
    }
    class Modes {
        @JvmField @ConfigOption(name = "Unique mode", desc = "") @Accordion val unique = Unique()
        @JvmField @ConfigOption(name = "Full clear mode", desc = "") @Accordion val fullClear = FullClear()
        @JvmField @ConfigOption(name = "Sparkling mode", desc = "") @Accordion val sparkling = Sparkling()
    }

    class FullClear {
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.fullClearHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.fullClearHideFloorDropEsp
    }
    class Sparkling {
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.sparklingHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.sparklingHideFloorDropEsp
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "/profits top 10, except Rockmite.")
        @ConfigEditorBoolean var profitable = ConfigManager.sparklingProfitableShardEsp
    }
    class Unique {
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.uniqueHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.uniqueHideFloorDropEsp
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "/profits top 10, except Rockmite.")
        @ConfigEditorBoolean var profitable = ConfigManager.profitableShardEsp
    }
    class Party {
        @JvmField @ConfigOption(name = "Auto biome reports", desc = "Announce completed biomes in party chat. Backend sync stays on.")
        @ConfigEditorBoolean var biomeReports = ConfigManager.autoBiomeReports
        @JvmField @ConfigOption(name = "Announce starting quest items", desc = "Includes unused-item reminders.")
        @ConfigEditorBoolean var startingItems = ConfigManager.announceStartingItems
        @JvmField @ConfigOption(name = "Respond to party commands", desc = "")
        @ConfigEditorBoolean var enabled = ConfigManager.partyCommandsEnabled
        @JvmField @ConfigOption(name = "Respect !dt requests", desc = "Hold Shift to bypass entry blocks.")
        @ConfigEditorBoolean var downtime = ConfigManager.downtimeCommand

    }
    class Miria {
        @JvmField @ConfigOption(name = "Ticket mode", desc = "Basic: Uncommon. First-Class: Legendary.")
        @ConfigEditorDropdown(values = ["Basic", "First-Class"])
        var ticketMode = ContestConfig.ticketMode
        @JvmField @ConfigOption(name = "Contest tracking", desc = "")
        @ConfigEditorBoolean var enabled = ContestConfig.trackContest
        @JvmField @ConfigOption(name = "Party Contest Guard Entry", desc = "Guard entry into safari until all party members send ticket earned confirmation message. Party leader only.")
        @ConfigEditorBoolean var autoContestDowntime = ContestConfig.autoContestDowntime
        @JvmField @ConfigOption(name = "Honeyhive beacons", desc = "Ready hives until your contest target.")
        @ConfigEditorBoolean var honeyHives = ConfigManager.honeyHives
        @JvmField val warning = Warning()
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
        @JvmField @ConfigOption(name = "5 minute warning", desc = "Below your ticket target.")
        @ConfigEditorBoolean var five = 5 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "3 minute warning", desc = "Below your ticket target.")
        @ConfigEditorBoolean var three = 3 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "1 minute warning", desc = "Below your ticket target.")
        @ConfigEditorBoolean var one = 1 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "No contest warnings", desc = "")
        @ConfigEditorButton(runnableId = 2, buttonText = "None") var none = false
        @JvmField @ConfigOption(name = "Warning titles", desc = "")
        @ConfigEditorBoolean var titles = ContestConfig.contestWarnTitle
        @JvmField @ConfigOption(name = "Sound volume", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ContestConfig.contestSoundVolume.coerceIn(0, 100)
        @JvmField @ConfigOption(name = "Warning sound", desc = "Minecraft sound ID.")
        @ConfigEditorText var sound = ContestConfig.contestSound
        @JvmField @ConfigOption(name = "Preview sound", desc = "")
        @ConfigEditorButton(runnableId = 7, buttonText = "Preview") var saveSound = false
    }
    class History {
        @JvmField @ConfigOption(name = "History", desc = "")
        @ConfigEditorButton(runnableId = 10, buttonText = "History") var open = false
        @JvmField @ConfigOption(name = "Import History from Logs", desc = "Import and repair history. Keeps backups.")
        @ConfigEditorButton(runnableId = 11, buttonText = "Import logs") var importLogs = false
        @JvmField @ConfigOption(name = "Save runs", desc = "")
        @ConfigEditorBoolean var runs = ConfigManager.runHistory
        @JvmField @ConfigOption(name = "Coins at insta-sell", desc = "Instead of sell-offer prices.")
        @ConfigEditorBoolean var instaSell = ConfigManager.runCoinsInstaSell
        @JvmField @ConfigOption(name = "Achievements", desc = "")
        @ConfigEditorBoolean var achievements = ConfigManager.achievements
        @JvmField @ConfigOption(name = "Achievement sound", desc = "")
        @ConfigEditorBoolean var sound = ConfigManager.achievementSound
        @JvmField @ConfigOption(name = "Achievements to party", desc = "")
        @ConfigEditorBoolean var toParty = ConfigManager.achievementsToParty
    }
    class Warp {
        @JvmField @ConfigOption(name = "Sparkle warping", desc = "This method allows players to get warped into runs before the 30s warp window is up, allowing for players to roll for sparkling critters that take more than 30s to find without using a ticket. Run /start with the party you want to bring back. It saves the roster and starts the cycle. Solo disbands the party, and Duo keeps your runner. Run /start again with a different roster to replace the saved party. Previous leechers are invited together at 24s, then warped at the set delay. Returning to Torrhus repeats the cycle. Auto partying enables you to have a warp list of players that need specific sparkling critters that can be detected within 24s of you starting the run. Then, when you find a sparkling critter, it will attempt to party the players in the list in priority order.")
        @StandaloneDescription var description = false
        @JvmField @ConfigOption(name = "Leader mode", desc = "Solo: disband. Duo: keep your runner.")
        @ConfigEditorDropdown(values = ["Solo", "Duo"]) var mode = ConfigManager.sparkleWarpMode
        @JvmField @ConfigOption(name = "Duo runner", desc = "Set with /duo <IGN>.")
        @ConfigEditorText var duo = ConfigManager.warpDuo
        @JvmField @ConfigOption(name = "Auto-party", desc = "Warp List invites until 24s.")
        @ConfigEditorBoolean var autoParty = ConfigManager.autoParty
        @JvmField @ConfigOption(name = "Invite online players only", desc = net.johnceo.sparklingmutuals.alerts.WarpPlayerStatus.DESCRIPTION)
        @ConfigEditorBoolean var autoPartyOnlineOnly = ConfigManager.autoPartyOnlineOnly
        @JvmField @ConfigOption(name = "Warp List", desc = "Who to invite for each critter.")
        @ConfigEditorButton(runnableId = 12, buttonText = "Warp List") var list = false
        @JvmField @ConfigOption(name = "Auto warping", desc = "/start begins the cycle. Uses the Safari progress timer.")
        @ConfigEditorBoolean var enabled = ConfigManager.warpAlertsEnabled
        @JvmField @ConfigOption(name = "Leech", desc = "Accept whitelisted invites; leave your current party.")
        @ConfigEditorBoolean var leech = ConfigManager.leechMode
        @JvmField @ConfigOption(name = "Leech whitelist", desc = "Inviter IGNs. Empty accepts nobody.")
        @ConfigEditorText var whitelist = ConfigManager.leechWhitelist
        @JvmField @ConfigOption(name = "Delay in seconds", desc = "")
        @ConfigEditorText var delay = ConfigManager.warpDelaySeconds.toString()
        @JvmField var saveDelay = false
        @JvmField @ConfigOption(name = "Default delay", desc = "28 seconds.")
        @ConfigEditorButton(runnableId = 5, buttonText = "Reset to 28s") var reset = false
        @JvmField @ConfigOption(name = "Sparkling Auto-entry", desc = "Automatically uses a ticket when party detects a sparkling critter.\nUSE AT YOUR OWN RISK!!!!")
        @DescriptionColor(rgb = 0xFF5555)
        @ConfigEditorBoolean var autoEnter = ConfigManager.safariEntryAutoEnter
    }
    class Safari {
        @JvmField @ConfigOption(name = "Safari entry", desc = "") @Accordion val entry = SafariEntrySettings()
        @JvmField @ConfigOption(name = "Sparkling detection and alerts", desc = "") @Accordion val alerts = Alerts()
        @JvmField @ConfigOption(name = "Capture tools", desc = "") @Accordion val capture = SafariCaptureSettings()
        @JvmField @ConfigOption(name = "Biome interactions", desc = "") @Accordion val interactions = SafariInteractionSettings()
        @JvmField @ConfigOption(name = "Party visibility", desc = "") @Accordion val teammates = SafariTeammateSettings()
        @JvmField @ConfigOption(name = "Visual cleanup", desc = "") @Accordion val visibility = SafariVisibilitySettings()
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
        @JvmField @ConfigOption(name = "Guard Safari entry", desc = "Wait for party members. Shift bypasses.")
        @ConfigEditorBoolean var holdManagerOnKick = ConfigManager.holdManagerOnKick
        @JvmField @ConfigOption(name = "Auto-select Safari ticket", desc = "Non-leaders, inside Safari only.")
        @ConfigEditorBoolean var autoSelectTicket = ConfigManager.autoSelectSafariTicket
        @JvmField @ConfigOption(name = "Preferred ticket", desc = "Uses the nearest tier if unavailable.")
        @ConfigEditorDropdown(values = ["First Class", "Basic"])
        var ticketPreference = ConfigManager.safariTicketPreference
    }
    class SafariCaptureSettings {
        @JvmField @ConfigOption(name = "Doomspiral capsule throw tick timer", desc = "Within 10 blocks of Doomspiral.")
        @ConfigEditorBoolean var doomTimer = ConfigManager.doomspiralThrowTimer
        @JvmField @ConfigOption(name = "Doomspiral emergence ticks", desc = "")
        @ConfigEditorSlider(minValue = 1f, maxValue = 600f, minStep = 1f) var doomTicks = ConfigManager.doomspiralEmergenceTicks
        @JvmField @ConfigOption(name = "Doomspiral throw lead ticks", desc = "Ticks before emergence to throw.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 40f, minStep = 1f) var doomLead = ConfigManager.doomspiralThrowLeadTicks
        @JvmField @ConfigOption(name="Throw timer appearance",desc="") @Accordion
        var doomAppearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("doom_throw")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Capsule projections", desc = "Turn view bobbing off.")
        @ConfigEditorBoolean var experimentalCapsulePath = ConfigManager.experimentalCapsulePath
        @JvmField @ConfigOption(name = "Capsule path width", desc = "")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f) var capsulePathWidth = ConfigManager.capsulePathWidth
        @JvmField @ConfigOption(name = "Show guide through terrain", desc = "")
        @ConfigEditorBoolean var showCapsuleGuideThroughTerrain = ConfigManager.showCapsuleGuideThroughTerrain
        @JvmField @ConfigOption(name = "Auto Clicker", desc = "Left-click Rockmite mounds.")
        @ConfigEditorBoolean var autoClicker = ConfigManager.autoClicker
    }
    class SafariInteractionSettings {
        @JvmField @ConfigOption(name = "Gemzie gem auto-select", desc = "Hold right-click over podiums.")
        @ConfigEditorBoolean var gemzieGemAutoSelect = ConfigManager.gemzieGemAutoSelect
        @JvmField @ConfigOption(name = "Full candle hitbox", desc = "While holding incense.")
        @ConfigEditorBoolean var candleHitbox = ConfigManager.candleHitbox
        @JvmField @ConfigOption(name = "Auto-accept Hideyho", desc = "")
        @ConfigEditorBoolean var hideyhoAutoAccept = ConfigManager.hideyhoAutoAccept
    }
    class SafariTeammateSettings {
        @JvmField @ConfigOption(name = "Highlight teammates", desc = "")
        @ConfigEditorBoolean var highlightTeammates = ConfigManager.highlightTeammates
        @JvmField @ConfigOption(name = "Show teammate names", desc = "Through walls.")
        @ConfigEditorBoolean var showTeammateNames = ConfigManager.showTeammateNames
        @JvmField @ConfigOption(name = "Highlight color", desc = "")
        @ConfigEditorColour var color = SafariEspConfig.teammateColor
        @JvmField @ConfigOption(name = "Name text scale", desc = "")
        @ConfigEditorSlider(minValue = .5f, maxValue = 4f, minStep = .25f)
        var textScale = SafariEspConfig.teammateTextScale
    }
    class WaterOpacitySettings {
        @JvmField @ConfigOption(name = "Forest", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var forest = ConfigManager.waterOpacityFor(SafariBiome.FOREST)
        @JvmField @ConfigOption(name = "Cavern", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var cavern = ConfigManager.waterOpacityFor(SafariBiome.CAVERN)
        @JvmField @ConfigOption(name = "Icy", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var icy = ConfigManager.waterOpacityFor(SafariBiome.ICY)
        @JvmField @ConfigOption(name = "Haunted", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
        var haunted = ConfigManager.waterOpacityFor(SafariBiome.HAUNTED)

        fun values() = mapOf(SafariBiome.FOREST to forest, SafariBiome.CAVERN to cavern,
            SafariBiome.ICY to icy, SafariBiome.HAUNTED to haunted).mapValues { it.value.coerceIn(0, 100) }
    }
    class SafariVisibilitySettings {
        @JvmField @ConfigOption(name = "Clear water", desc = "Safari only.")
        @ConfigEditorBoolean var clearWater = ConfigManager.clearWater
        @JvmField @ConfigOption(name = "Water opacity", desc = "0% transparent, 100% opaque.") @Accordion
        val waterOpacity = WaterOpacitySettings()
        @JvmField @ConfigOption(name = "Hide useless chats in safari", desc = "")
        @ConfigEditorBoolean var hideCaptureChat = ConfigManager.hideCaptureChat
        @JvmField @ConfigOption(name = "Chat Toggles", desc = "") @Accordion
        val chatToggles = SafariChatToggleSettings()
        @JvmField @ConfigOption(name = "Hide capsules on ground", desc = "")
        @ConfigEditorBoolean var hideGroundCapsules = ConfigManager.hideGroundCapsules
        @JvmField @ConfigOption(name = "Hide flying capsules", desc = "Near your camera.")
        @ConfigEditorBoolean var hideFlyingCapsules = ConfigManager.hideFlyingCapsules
        @JvmField @ConfigOption(name = "Flying capsule distance", desc = "Blocks.")
        @ConfigEditorSlider(minValue = .5f, maxValue = 6f, minStep = .5f)
        var capsuleHideDistance = ConfigManager.capsuleHideDistance
        @JvmField @ConfigOption(name = "Hide Haunted paintings", desc = "")
        @ConfigEditorBoolean var paintings = ConfigManager.hideHauntedPaintings
        @JvmField @ConfigOption(name = "Remove darkness", desc = "Safari only.")
        @ConfigEditorBoolean var darkness = ConfigManager.removeDarkness
    }
    class Alerts {
        @JvmField @ConfigOption(name = "Sparkling catch count", desc = "Catch number in party chat.")
        @ConfigEditorBoolean var catchCount = ConfigManager.sparklingCatchCount
        @JvmField @ConfigOption(name = "Unique Sparkling catch alert", desc = "New discoveries only.")
        @ConfigEditorBoolean var caught = ConfigManager.specialCatchAlert
        @JvmField @ConfigOption(name = "Always play Sparkling catch celebration", desc = "Celebrate every Sparkling catch, including duplicates.")
        @ConfigEditorBoolean var alwaysCelebrate = ConfigManager.alwaysCelebrateSparkling
        @JvmField @ConfigOption(name = "Catch celebration", desc = "")
        @ConfigEditorDropdown(values = ["Prismatic Whisper", "Prismatic Gleam", "Starlight Drift", "Starlight Waltz", "Aurora Rise", "Aurora Cascade", "Comet Shower", "Comet Tempest", "Firework Bloom", "Firework Symphony", "Celestial Chorus", "Celestial Carnival", "Nebula Surge", "Nebula Rhapsody", "Galactic Pulse", "Galactic Tempest", "Astral Reverie", "Astral Ascension", "Supernova", "Hypernova", "Cosmic Jubilee", "Cosmic Overdrive", "Radiant Singularity", "Celestial Transcendence", "Event Horizon", "Infinite Radiance", "Endless Cosmos", "Chromatic Zenith", "Ethereal Genesis", "Ethereal Ascendance", "Mythic Dominion", "Mythic Eternity", "Celestial Genesis", "Celestial Ascendance", "Infinite Transcendence", "Infinite Apotheosis", "Eternal Dawn", "Eternal Resonance", "Eternal Dominion", "Eternal Transcendence", "Eternal Apotheosis"]) var caughtStyle = ConfigManager.specialCatchStyle
        @JvmField @ConfigOption(name = "Preview catch celebration", desc = "")
        @ConfigEditorButton(runnableId = 13, buttonText = "Preview") var previewCatch = false

        @JvmField @ConfigOption(name = "Nearby shiny detection", desc = "Within 80 blocks in your biome.")
        @ConfigEditorBoolean var shiny = ConfigManager.shinyDetection
        @JvmField @ConfigOption(name="Nearby sparklings HUD appearance",desc="") @Accordion
        var nearbyAppearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("sparklings")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Sparkling detection alert", desc = "")
        @ConfigEditorBoolean var alert = ConfigManager.sparklingAlert
        @JvmField @ConfigOption(name="Detection alert appearance",desc="") @Accordion
        var appearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("alert")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Announce sparklings to party", desc = "")
        @ConfigEditorBoolean var announce = ConfigManager.sparklingPartyAnnouncer
        @JvmField @ConfigOption(name = "Sparkling sound", desc = "Minecraft sound ID.")
        @ConfigEditorText var sound = ConfigManager.sparklingSound
        @JvmField @ConfigOption(name = "Sparkling sound volume", desc = "0 mutes.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ConfigManager.sparklingSoundVolume
        @JvmField @ConfigOption(name = "Preview sparkling sound", desc = "")
        @ConfigEditorButton(runnableId = 9, buttonText = "Preview") var saveSound = false
        @JvmField var gems = ConfigManager.allGemsAlert
        @JvmField var birdFood = ConfigManager.allBirdFoodAlert
        @JvmField var coins = ConfigManager.allCoinsAlert
        @JvmField var incense = ConfigManager.allIncenseAlert
        @JvmField var runComplete = ConfigManager.runCompleteAlert
        @JvmField var runTitle = ConfigManager.runCompleteTitle
        @JvmField var runSound = ConfigManager.runCompleteSound
        @JvmField var runVolume = ConfigManager.runCompleteVolume
    }
    class Tracking {
        @JvmField @ConfigOption(name="Progress HUD",desc="") @Accordion val progressHud=ProgressHudOptions()
        @JvmField @ConfigOption(name="Missing / Unchecked",desc="") @Accordion val missingHud=HudOptions(ConfigManager.missingPanel,"missing")
        @JvmField @ConfigOption(name="Biome Objectives",desc="") @Accordion val objectivesHud=HudOptions(ConfigManager.biomeObjectives,"objectives")
        @JvmField @ConfigOption(name="Biome captures",desc="") @Accordion val capturesHud=HudOptions(ConfigManager.catchCountPanel,"captures")
        @JvmField @ConfigOption(name="Safari contest timer",desc="") @Accordion val contestHud=ContestHudOptions()
        @JvmField @ConfigOption(name="Show Safari HUDs",desc="")
        @ConfigEditorDropdown(values=["Only in Safari","Safari and entrance","Everywhere"]) var where=ConfigManager.showWhere
        var progress get()=progressHud.enabled;set(value) { progressHud.enabled=value }
        var missing get()=missingHud.enabled;set(value) { missingHud.enabled=value }
        var objectives get()=objectivesHud.enabled;set(value) { objectivesHud.enabled=value }
        var captures get()=capturesHud.enabled;set(value) { capturesHud.enabled=value }
        var contestTimer get()=contestHud.enabled;set(value) { contestHud.enabled=value }
    }
    open class HudOptions(initial: Boolean,id: String) {
        @JvmField @ConfigOption(name="Enabled",desc="") @ConfigEditorBoolean var enabled=initial
        @JvmField @ConfigOption(name="Appearance",desc="") @Accordion
        var appearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue(id)) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name="Position and size",desc="") @ConfigEditorButton(buttonText="Edit HUDs")
        val placement=Runnable { val client=Minecraft.getInstance();client.smSetScreen(ContestGui(client.smScreen)) }
    }
    class ProgressHudOptions: HudOptions(ConfigManager.progressHud,"progress") {
        @JvmField @ConfigOption(name="Profit line",desc="Shards, feathers and essence.") @ConfigEditorBoolean var profit=ConfigManager.progressProfit
        @JvmField @ConfigOption(name="Hotspot line",desc="") @ConfigEditorBoolean var hotspot=ConfigManager.progressHotspot
    }
    class ContestHudOptions: HudOptions(ContestConfig.showContestTimer,"miria") {
        @JvmField @ConfigOption(name="Bracket row",desc="") @ConfigEditorBoolean var bracket=ContestConfig.showBracket
        @JvmField @ConfigOption(name="Score row",desc="") @ConfigEditorBoolean var score=ContestConfig.showScore
    }
    class Api {
        @JvmField @ConfigOption(name = "Use Shiny Hunter cloud API", desc = "No key needed. Personal key takes priority.")
        @ConfigEditorBoolean var cloud = ConfigManager.useCloudApi
        @JvmField @ConfigOption(name = "Hypixel API key", desc = "Optional with cloud API.")
        @ConfigEditorButton(runnableId = 4, buttonText = "Edit API key") var edit = false
    }

    private fun feedback(text: String) { Minecraft.getInstance().player?.sendSystemMessage(LocalChat.component("[SM] $text")) }
    fun executeRunnable(id: Int) {
        val client = Minecraft.getInstance()
        when (id) {
            1 -> { apply(); client.execute { client.smSetScreen(ContestGui(client.smScreen)) } }
            2 -> { miria.five = false; miria.three = false; miria.one = false; apply() }
            3 -> client.execute {
                client.smSetScreen(ChatScreen("", false))
                client.player?.sendSystemMessage(LocalChat.component(CommandHelp.localLines().joinToString("\n")))
            }
            4 -> client.execute { client.smSetScreen(ApiKeyScreen(client.smScreen)) }
            5 -> { warp.delay = "28"; saveDelay() }
            6 -> saveDelay()
            7 -> { apply(); saveSound()
                Identifier.tryParse(ContestConfig.contestSound)?.let { id ->
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null)?.let { sound ->
                        client.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound,1f,ContestConfig.contestSoundVolume/100f))
                    }
                }
            }
            8 -> { apply(); client.execute { client.smSetScreen(CatchCountScreen(client.smScreen)) } }
            9 -> { apply(); if (saveSparklingSound()) SparklingEncounters.playSound(client) }
            10 -> { apply(); client.execute { client.smSetScreen(HistoryScreen(client.smScreen)) } }
            13 -> { apply(); client.smSetScreen(CatchCelebrationPreviewScreen(client.smScreen ?: SafariConfigScreen(this))) }
            12 -> { apply(); client.execute { client.smSetScreen(net.johnceo.sparklingmutuals.alerts.WarpListScreen(client.smScreen,
                autoPartyChanged = { warp.autoParty = it }, onlineOnlyChanged = { warp.autoPartyOnlineOnly = it })) } }
            11 -> { apply(); client.execute {
                client.smSetScreen(null)
                LogImporter.runImport(LogImporter.defaultLogs())
            } }
        }
    }
    fun isValidRunnable(id: Int) = id in 1..13
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
    private var lastAppliedMode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
    fun apply(notifyMode: Boolean = false) {
        val currentMode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
        if (currentMode != lastAppliedMode) {
            tracking.captures = ConfigManager.catchCountPanel
            lastAppliedMode = currentMode
        }
        val bannerChanged = banner.apply()
        if (modes.unique.hideCritters || modes.unique.hideDrops) modes.unique.profitable=false
        if (modes.sparkling.hideCritters || modes.sparkling.hideDrops) modes.sparkling.profitable=false
        val duoName=warp.duo.trim().takeIf { Regex("[A-Za-z0-9_]{1,16}").matches(it) }.orEmpty()
        val whitelist=net.johnceo.sparklingmutuals.alerts.WarpList.parse(warp.whitelist).joinToString(" ")
        val whitelistChanged=whitelist != ConfigManager.leechWhitelist
        if (whitelistChanged) {
            ConfigManager.leechWhitelist=whitelist
            net.johnceo.sparklingmutuals.alerts.SparkleWarping.whitelistChanged()
        }
        if (warp.enabled && !ConfigManager.warpAlertsEnabled) warp.leech = false
        if (warp.leech) warp.enabled = false
        val warpModesChanged = net.johnceo.sparklingmutuals.alerts.SparkleWarping.setModes(warp.enabled, warp.leech)
        val warpConfigurationChanged = warp.mode.coerceIn(0,1) != ConfigManager.sparkleWarpMode ||
            duoName != ConfigManager.warpDuo
        if (warpConfigurationChanged) {
            ConfigManager.sparkleWarpMode=warp.mode.coerceIn(0,1)
            ConfigManager.warpDuo=duoName
            net.johnceo.sparklingmutuals.alerts.SparkleWarping.configurationChanged()
        }
        val changes = listOf(ConfigManager::safariEntryAutoEnter to warp.autoEnter, ConfigManager::sparklingCatchCount to safari.alerts.catchCount, ConfigManager::specialCatchAlert to safari.alerts.caught,
            ConfigManager::alwaysCelebrateSparkling to safari.alerts.alwaysCelebrate,
            ConfigManager::doomspiralThrowTimer to safari.capture.doomTimer, ConfigManager::autoParty to warp.autoParty, ConfigManager::autoPartyOnlineOnly to warp.autoPartyOnlineOnly, ConfigManager::autoBiomeReports to party.biomeReports,
            ConfigManager::uniqueHideCritterEsp to modes.unique.hideCritters, ConfigManager::uniqueHideFloorDropEsp to modes.unique.hideDrops,
            ConfigManager::fullClearHideCritterEsp to modes.fullClear.hideCritters, ConfigManager::fullClearHideFloorDropEsp to modes.fullClear.hideDrops,
            ConfigManager::sparklingHideCritterEsp to modes.sparkling.hideCritters, ConfigManager::sparklingHideFloorDropEsp to modes.sparkling.hideDrops, ConfigManager::announceStartingItems to party.startingItems, ConfigManager::huntingBoxValue to skyblock.helpers.huntingBoxValue, ConfigManager::shardSaleGuidance to skyblock.helpers.shardSaleGuidance, ConfigManager::skillOverflow to skyblock.helpers.skillOverflow, ConfigManager::skillOverflowChat to skyblock.helpers.skillOverflowChat, ConfigManager::skillOverflowNumbers to skyblock.helpers.skillOverflowNumbers, ConfigManager::estimatedItemValue to skyblock.helpers.estimatedItemValue, ConfigManager::estimatedChestValue to skyblock.helpers.estimatedChestValue, ConfigManager::phantomleafSolver to miscEndgame.phantomleafSolver, ConfigManager::highlightTeammates to safari.highlightTeammates, ConfigManager::showTeammateNames to safari.showTeammateNames, ConfigManager::profitableShardEsp to modes.unique.profitable, ConfigManager::sparklingProfitableShardEsp to modes.sparkling.profitable, ConfigManager::partyCommandsEnabled to party.enabled, ConfigManager::downtimeCommand to party.downtime, ConfigManager::useCloudApi to api.cloud, ConfigManager::hideHauntedPaintings to safari.paintings,
            ConfigManager::shinyDetection to safari.shiny, ConfigManager::hideyhoAutoAccept to safari.hideyhoAutoAccept, ConfigManager::autoSelectSafariTicket to safari.autoSelectTicket,
            ConfigManager::progressHud to tracking.progress, ConfigManager::progressProfit to tracking.progressHud.profit, ConfigManager::progressHotspot to tracking.progressHud.hotspot, ConfigManager::catchCountPanel to tracking.captures,
            ConfigManager::experimentalCapsulePath to safari.experimentalCapsulePath,
            ConfigManager::gemzieGemAutoSelect to safari.gemzieGemAutoSelect,
            ConfigManager::showCapsuleGuideThroughTerrain to safari.showCapsuleGuideThroughTerrain,
            ConfigManager::missingPanel to tracking.missing, ConfigManager::biomeObjectives to tracking.objectives,
            ConfigManager::removeDarkness to safari.darkness,
            ConfigManager::clearWater to safari.visibility.clearWater,
            ConfigManager::sparklingAlert to safari.alert, ConfigManager::sparklingPartyAnnouncer to safari.announce,
            ConfigManager::autoClicker to safari.autoClicker, ConfigManager::candleHitbox to safari.candleHitbox, ConfigManager::holdManagerOnKick to safari.holdManagerOnKick, ConfigManager::honeyHives to miria.honeyHives, ConfigManager::hideCaptureChat to safari.hideCaptureChat, ConfigManager::runHistory to history.runs, ConfigManager::achievements to history.achievements, ConfigManager::achievementSound to history.sound, ConfigManager::achievementsToParty to history.toParty, ConfigManager::runCoinsInstaSell to history.instaSell,
            ConfigManager::allGemsAlert to safari.alerts.gems, ConfigManager::allBirdFoodAlert to safari.alerts.birdFood,
            ConfigManager::allCoinsAlert to safari.alerts.coins, ConfigManager::allIncenseAlert to safari.alerts.incense, ConfigManager::runCompleteAlert to safari.alerts.runComplete,
            ConfigManager::hideGroundCapsules to safari.hideGroundCapsules, ConfigManager::hideFlyingCapsules to safari.hideFlyingCapsules)
        val espChanged = esp.apply()
        val modWideCrittersChanged = modWideCritters.apply(SafariEspConfig.modWideToggles)
        val profitableCrittersChanged = profitableCritters.apply(SafariEspConfig.profitableToggles)
        val customizationChanged = customization.apply()
        val distance = ConfigManager.validCapsuleDistance(safari.capsuleHideDistance)
        val pathWidth = ConfigManager.validCapsulePathWidth(safari.capsulePathWidth)
        val waterOpacity = safari.visibility.waterOpacity.values()
        val alwaysOnCrittersChanged = alwaysOnCritters.apply(SafariEspConfig.alwaysOnToggles)
        val alwaysOnFloorDropsChanged = alwaysOnFloorDrops.apply()
        val chatTogglesChanged = safari.visibility.chatToggles.apply()
        val generalChanged = espChanged || modWideCrittersChanged || profitableCrittersChanged || alwaysOnCrittersChanged || alwaysOnFloorDropsChanged || chatTogglesChanged ||
            bannerChanged || ConfigManager.specialCatchStyle != safari.alerts.caughtStyle || whitelistChanged || warpModesChanged || warpConfigurationChanged || ConfigManager.doomspiralEmergenceTicks != safari.capture.doomTicks.coerceIn(1, 600) ||
            ConfigManager.doomspiralThrowLeadTicks != safari.capture.doomLead.coerceIn(0, 40) || ConfigManager.biomeWaterOpacity != waterOpacity || ConfigManager.capsulePathWidth != pathWidth || customizationChanged || changes.any { (property, value) -> property.get() != value } ||
            ConfigManager.safariTicketPreference != safari.ticketPreference || ConfigManager.showWhere != tracking.where || ConfigManager.capsuleHideDistance != distance ||
            ConfigManager.runCompleteVolume != safari.alerts.runVolume.coerceIn(0, 100) || ConfigManager.sparklingSoundVolume != safari.alerts.volume.coerceIn(0, 100)
        changes.forEach { (property, value) -> property.set(value) }
        ConfigManager.safariTicketPreference = safari.ticketPreference.coerceIn(0, 1)
        ConfigManager.showWhere = tracking.where.coerceIn(0, 2)
        ConfigManager.capsuleHideDistance = distance
        ConfigManager.biomeWaterOpacity = waterOpacity
        ConfigManager.capsulePathWidth = pathWidth
        ConfigManager.doomspiralEmergenceTicks = safari.capture.doomTicks.coerceIn(1, 600)
        ConfigManager.doomspiralThrowLeadTicks = safari.capture.doomLead.coerceIn(0, 40)
        ConfigManager.specialCatchStyle = safari.alerts.caughtStyle.coerceIn(0,40)
        ConfigManager.runCompleteVolume = safari.alerts.runVolume.coerceIn(0, 100)
        ConfigManager.sparklingSoundVolume = safari.alerts.volume.coerceIn(0, 100)
        if (generalChanged) ConfigManager.save()
        val warnings = listOfNotNull(5.takeIf { miria.five }, 3.takeIf { miria.three }, 1.takeIf { miria.one })
        val contestChanged = ContestConfig.showBracket != tracking.contestHud.bracket || ContestConfig.showScore != tracking.contestHud.score || ContestConfig.showContestTimer != tracking.contestTimer || ContestConfig.ticketMode != miria.ticketMode || ContestConfig.autoContestDowntime != miria.autoContestDowntime || ContestConfig.trackContest != miria.enabled || ContestConfig.contestWarnTitle != miria.titles ||
            ContestConfig.contestSoundVolume != miria.volume || ContestConfig.warningMinutes() != warnings
        ContestConfig.showBracket=tracking.contestHud.bracket;ContestConfig.showScore=tracking.contestHud.score
        ContestConfig.showContestTimer = tracking.contestTimer
        ContestConfig.trackContest = miria.enabled
        ContestConfig.ticketMode = miria.ticketMode.coerceIn(0, 1)
        ContestConfig.autoContestDowntime = miria.autoContestDowntime
        ContestConfig.contestWarnTitle = miria.titles
        ContestConfig.contestSoundVolume = miria.volume.coerceIn(0, 100)
        ContestConfig.contestWarnMinutes = warnings.joinToString(", ")
        ContestConfig.contestWarnEnabled = warnings.isNotEmpty()
        if (contestChanged) ContestConfig.save()
        val teammateColor = SafariEspConfig.validColor(safari.teammates.color, SafariEspConfig.teammateColor)
        val teammateScale = SafariEspConfig.validTextScale(safari.teammates.textScale)
        if (teammateColor != SafariEspConfig.teammateColor || teammateScale != SafariEspConfig.teammateTextScale) {
            SafariEspConfig.teammateColor = teammateColor
            SafariEspConfig.teammateTextScale = teammateScale
            ConfigManager.save()
        }
    }
}
