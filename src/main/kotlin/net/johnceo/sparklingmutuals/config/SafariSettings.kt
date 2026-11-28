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
import net.johnceo.sparklingmutuals.safari.SplitTimeFormat
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.sparkling.SparklingEncounters
import net.johnceo.sparklingmutuals.safari.ui.CatchCountScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.Identifier

/** Settings view model; the existing properties files remain the saved configuration. */
class SafariSettings {
    internal companion object { const val standardEdition = false }
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
    @JvmField @Category(name="ESP General",desc="") val espGeneral=EspGeneralSettings(esp.biomes,esp.floorDrops)
    @JvmField @Category(name="Reset",desc="") val reset=Reset()
    init {
        esp.critterColors = customization.critters
        esp.teammates = TeammateAppearanceSettings(safari.teammates)
        esp.waypointColors = customization.waypoints
        safari.alerts.appearance = customization.alert
        safari.alerts.nearbyAppearance = customization.huds.sparklings
        safari.capture.doomAppearance = customization.huds.doomThrow
        safari.capture.doomAppearance = customization.huds.doomThrow
        tracking.hotspotHud.appearance=customization.huds.hotspot
        tracking.progressHud.appearance=customization.huds.progress
        tracking.splitsHud.appearance=customization.huds.splits
        tracking.missingHud.appearance=customization.huds.missing
        tracking.objectivesHud.appearance=customization.huds.objectives
        tracking.capturesHud.appearance=customization.huds.captures
        tracking.contestHud.appearance=customization.huds.miria
    }
    class SkyBlock {
        @JvmField @ConfigOption(name = "Helpers", desc = "") @Accordion val helpers = SkyBlockHelpers()
    }
    class SkyBlockHelpers {
        @JvmField @ConfigOption(name = "Shard sale guidance", desc = "Compare sell offers, instant sales and salvaging for Safari shards.")
        @ConfigEditorBoolean var shardSaleGuidance = ConfigManager.shardSaleGuidance
        @JvmField @ConfigOption(name = "Skill overflow levels", desc = "Show levels beyond the normal skill cap in Skills menu tooltips.")
        @ConfigEditorBoolean var skillOverflow = ConfigManager.skillOverflow
        @JvmField @ConfigOption(name = "Overflow level-up messages", desc = "Show a chat message when you gain an overflow skill level.")
        @ConfigEditorBoolean var skillOverflowChat = ConfigManager.skillOverflowChat
        @JvmField @ConfigOption(name = "Use numbers for skill levels", desc = "Show 61 instead of LXI.")
        @ConfigEditorBoolean var skillOverflowNumbers = ConfigManager.skillOverflowNumbers
        @JvmField var estimatedItemValue = ConfigManager.estimatedItemValue
        @JvmField var estimatedChestValue = ConfigManager.estimatedChestValue
        @JvmField @ConfigOption(name = "Hunting Box value", desc = "Estimate the total value of shards on the pages you have opened.")
        @ConfigEditorBoolean var huntingBoxValue = ConfigManager.huntingBoxValue
        @JvmField @ConfigOption(name = "Ubik's Cube Cooldown", desc = "Show when you can play Split or Steal again in the Rift.") @Accordion
        val ubiksCubeTimer = HudOptions(ConfigManager.ubiksCubeTimer, "ubiks_cube")
        @JvmField @ConfigOption(name = "Phantomleaf solver", desc = "Stand still to hear two notes. Move when the indicator turns yellow.")
        @ConfigEditorBoolean var phantomleafSolver = ConfigManager.phantomleafSolver
    }
    class General {
        @JvmField @ConfigOption(name = "HUD font", desc = "") @Accordion
        val font = HudFontSettings()
        @JvmField @ConfigOption(name = "Command help", desc = "")
        @ConfigEditorButton(runnableId = 3, buttonText = "Show help") var help = false
        @JvmField @ConfigOption(name = "Move and resize HUDs", desc = "Drag HUDs to move them; scroll to resize them.")
        @ConfigEditorButton(runnableId = 1, buttonText = "Edit HUDs") var edit = false
    }
    class HudFontSettings {
        @JvmField @ConfigOption(name = "Use resource pack font", desc = "Use your active resource pack's font in HUDs and alerts.")
        @ConfigEditorBoolean var resourcePackFont = AppearanceConfig.resourcePackFont
        @JvmField @ConfigOption(name = "HUD text shadow", desc = "")
        @ConfigEditorBoolean var hudTextShadow = AppearanceConfig.hudTextShadow
        @JvmField @ConfigOption(name = "Sharper HUD scaling", desc = "Align text to screen pixels. HUD sizes may adjust slightly.")
        @ConfigEditorBoolean var pixelAlignedHuds = AppearanceConfig.pixelAlignedHuds
    }
    class Modes {
        @JvmField @ConfigOption(name = "Unique mode", desc = "") @Accordion val unique = Unique()
        @JvmField @ConfigOption(name = "Full clear mode", desc = "") @Accordion val fullClear = FullClear()
        @JvmField @ConfigOption(name = "Sparkling mode", desc = "") @Accordion val sparkling = Sparkling()
        @JvmField @ConfigOption(name = "Sparkling Checks Options", desc =
            "In Unique mode, enable checks for critters your party asks you to check for sparkling. For faster runs, leave the other checks off and catch one of each critter. Automatic biome reports (fd, hd, cd) follow your selected checks.") @Accordion
        val sparklingChecks = BiomeObjectiveToggles.checks()
    }

    class FullClear {
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.fullClearHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.fullClearHideFloorDropEsp
    }
    class Sparkling {
        @JvmField @ConfigOption(name = "Check all scannable critters", desc = "Show easy-to-scan critters in ESP and the missing panel, even if the party already has their sparkling variant.")
        @ConfigEditorBoolean var allScannable = ConfigManager.allScannableCritterChecks
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.sparklingHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.sparklingHideFloorDropEsp
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "Also highlight the ten most profitable critters, including their walls and mounds.")
        @ConfigEditorBoolean var profitable = ConfigManager.sparklingProfitableShardEsp
    }
    class Unique : UniqueRemainingCheckSettings() {
        @JvmField @ConfigOption(name = "Hide critter ESP", desc = "")
        @ConfigEditorBoolean var hideCritters = ConfigManager.uniqueHideCritterEsp
        @JvmField @ConfigOption(name = "Hide floor-drop ESP", desc = "")
        @ConfigEditorBoolean var hideDrops = ConfigManager.uniqueHideFloorDropEsp
        @JvmField @ConfigOption(name = "Profitable critter ESP", desc = "Also highlight the ten most profitable critters, including their walls and mounds.")
        @ConfigEditorBoolean var profitable = ConfigManager.profitableShardEsp
    }
    class Party {
        @JvmField @ConfigOption(name = "Biome Hotspot Preferences", desc = "Rank the biomes to break ties between players with the same hotspot and win count.")
        @ConfigEditorButton(buttonText = "Edit priorities")
        val hotspotPreferences = Runnable {
            val client = Minecraft.getInstance()
            (client.smScreen as? SafariConfigScreen)?.let { client.smSetScreen(HotspotPreferencesScreen(it)) }
        }
        @JvmField @ConfigOption(name = "Announce biome completion", desc = "Send fd, cd, id or hd in party chat when a biome is complete. Completion still syncs when this is off.")
        @ConfigEditorBoolean var biomeReports = ConfigManager.autoBiomeReports
        @JvmField @ConfigOption(name = "Announce starting quest items", desc = "Tell the party which quest items you started with and remind you to use them.")
        @ConfigEditorBoolean var startingItems = ConfigManager.announceStartingItems
        @JvmField @ConfigOption(name = "Respond to party commands", desc = "Answer party commands such as !since and !dye.")
        @ConfigEditorBoolean var enabled = ConfigManager.partyCommandsEnabled
        @JvmField @ConfigOption(name = "Respect !dt requests", desc = "Pause Safari entry when a party member requests downtime. Hold Shift to bypass.")
        @ConfigEditorBoolean var downtime = ConfigManager.downtimeCommand
        @JvmField @ConfigOption(name = "Local PB messages", desc = "Choose which personal best messages appear in your chat.")
        @Accordion val pbChat = PbChatSettings()
    }
    class PbChatSettings {
        @JvmField @ConfigOption(name = "Doomspiral", desc = "") @Accordion
        val doom = BossPbChatSettings(ConfigManager.doomSoloPbChat, ConfigManager.doomDuoPbChat,
            ConfigManager.doomObservedSoloPbChat, ConfigManager.doomObservedDuoPbChat)
        @JvmField @ConfigOption(name = "Wumpa", desc = "") @Accordion
        val wumpa = BossPbChatSettings(ConfigManager.wumpaSoloPbChat, ConfigManager.wumpaDuoPbChat,
            ConfigManager.wumpaObservedSoloPbChat, ConfigManager.wumpaObservedDuoPbChat)
        @JvmField @ConfigOption(name = "Full Clear biome PBs", desc = "") @ConfigEditorBoolean
        var fullClear = ConfigManager.fullClearPbChat
        @JvmField @ConfigOption(name = "Unique Run biome PBs", desc = "") @ConfigEditorBoolean
        var unique = ConfigManager.uniquePbChat
        @JvmField @ConfigOption(name = "Party Unique Run biome PBs", desc = "") @ConfigEditorBoolean
        var partyUnique = ConfigManager.partyUniquePbChat
    }
    class BossPbChatSettings(
        @JvmField @ConfigOption(name = "Your solo catches", desc = "") @ConfigEditorBoolean var solo: Boolean,
        @JvmField @ConfigOption(name = "Your duo catches", desc = "") @ConfigEditorBoolean var duo: Boolean,
        @JvmField @ConfigOption(name = "Observed solo catches", desc = "Solo catches by another player that you loot share.") @ConfigEditorBoolean var observedSolo: Boolean,
        @JvmField @ConfigOption(name = "Observed duo catches", desc = "Duo catches by another player that you loot share.") @ConfigEditorBoolean var observedDuo: Boolean
    )
    class Miria {
        @JvmField @ConfigOption(name = "Ticket target", desc = "Basic requires Uncommon; First-Class requires Legendary.")
        @ConfigEditorDropdown(values = ["Basic", "First-Class"])
        var ticketMode = ContestConfig.ticketMode
        @JvmField @ConfigOption(name = "Announce ticket earned", desc = "Send your ticket message in party chat when you reach the target bracket.") @ConfigEditorBoolean
        var announceTicket=ContestConfig.announceTicket
        @JvmField @ConfigOption(name = "Ticket earned message", desc = "Each ticket target keeps its own message.") @ConfigEditorText
        var ticketMessage=ContestConfig.ticketMessage()
        private var messageMode=ContestConfig.ticketMode
        fun applyTicketMessage(): Boolean {
            val previous=ContestConfig.ticketMessage(messageMode==1)
            ContestConfig.setTicketMessage(messageMode==1,ticketMessage)
            val changed=previous!=ContestConfig.ticketMessage(messageMode==1)
            if(messageMode!=ticketMode) {
                messageMode=ticketMode.coerceIn(0,1)
                ticketMessage=ContestConfig.ticketMessage(messageMode==1)
            }
            return changed
        }
        @JvmField @ConfigOption(name = "Track Miria's contest", desc = "Track your score, bracket and ticket progress.")
        @ConfigEditorBoolean var enabled = ContestConfig.trackContest
        @JvmField @ConfigOption(name = "Party contest entry guard", desc = "As party leader, wait for everyone to confirm their ticket in chat before entering Safari. Hold Shift to bypass for this contest.")
        @ConfigEditorBoolean var autoContestDowntime = ContestConfig.autoContestDowntime
        @JvmField @ConfigOption(name = "Honeyhive beacons", desc = "Mark ready hives until you reach your ticket target.")
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
        @JvmField @ConfigOption(name = "5-minute warning", desc = "Warn if you are still below your ticket target.")
        @ConfigEditorBoolean var five = 5 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "3-minute warning", desc = "Warn if you are still below your ticket target.")
        @ConfigEditorBoolean var three = 3 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "1-minute warning", desc = "Warn if you are still below your ticket target.")
        @ConfigEditorBoolean var one = 1 in ContestConfig.warningMinutes()
        @JvmField @ConfigOption(name = "Disable all contest warnings", desc = "")
        @ConfigEditorButton(runnableId = 2, buttonText = "None") var none = false
        @JvmField @ConfigOption(name = "Show warning titles", desc = "")
        @ConfigEditorBoolean var titles = ContestConfig.contestWarnTitle
        @JvmField @ConfigOption(name = "Sound volume", desc = "")
        @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f) var volume = ContestConfig.contestSoundVolume.coerceIn(0, 100)
        @JvmField @ConfigOption(name = "Warning sound", desc = "Minecraft sound ID, such as minecraft:block.note_block.pling.")
        @ConfigEditorText var sound = ContestConfig.contestSound
        @JvmField @ConfigOption(name = "Preview sound", desc = "")
        @ConfigEditorButton(runnableId = 7, buttonText = "Preview") var saveSound = false
    }
    class History {
        @JvmField @ConfigOption(name = "Open history", desc = "")
        @ConfigEditorButton(runnableId = 10, buttonText = "History") var open = false
        @JvmField @ConfigOption(name = "Import history from logs", desc = "Put all logs from your old Safari runs in this Minecraft instance's logs folder, then click Import logs. Recovers missing runs and sparkling catches for your History UUID.")
        @ConfigEditorButton(runnableId = 11, buttonText = "Import logs") var importLogs = false
        @JvmField @ConfigOption(name = "History UUID", desc = "Track Safari history for this Minecraft account. Defaults to the first account you log in with after updating.") @ConfigEditorText
        var ownerUuid = ConfigManager.historyOwnerUuid
        @JvmField @ConfigOption(name = "Use instant-sell prices", desc = "Value run profits at instant-sell prices instead of sell-offer prices.")
        @ConfigEditorBoolean var instaSell = ConfigManager.runCoinsInstaSell
        @JvmField @ConfigOption(name = "Achievements", desc = "Track and unlock Safari achievements.")
        @ConfigEditorBoolean var achievements = ConfigManager.achievements
        @JvmField @ConfigOption(name = "Achievement sounds", desc = "")
        @ConfigEditorBoolean var sound = ConfigManager.achievementSound
        @JvmField @ConfigOption(name = "Announce achievements to party", desc = "Share unlocked achievements in party chat.")
        @ConfigEditorBoolean var toParty = ConfigManager.achievementsToParty
    }
    class Warp {
        @JvmField @ConfigOption(name = "Refresh warp list daily", desc = "Remove sparklings that players on your warp list have already caught.")
        @ConfigEditorBoolean var refresh = ConfigManager.warpListRefresh
        @JvmField @ConfigOption(name = "Sparkle Warping", desc = "Warp players into a run before the 30-second window closes so they can check late-spawning sparklings without spending a ticket. Run /start with the party you want to bring back: it saves the roster and starts the cycle. Run it again to replace the saved party. Solo disbands everyone; Duo keeps your runner. Each run, saved players are invited at the reparty cutoff and warped at the warp time. Auto-party uses your warp list to invite players who need a detected sparkling, in priority order, before the cutoff.")
        @StandaloneDescription var description = false
        @JvmField @ConfigOption(name = "Warping setup", desc = "Solo disbands everyone. Duo keeps your runner in the party.")
        @ConfigEditorDropdown(values = ["Solo", "Duo"]) var mode = ConfigManager.sparkleWarpMode
        @JvmField @ConfigOption(name = "Duo runner", desc = "The player who stays with you in Duo mode. Also set with /duo <IGN>.")
        @ConfigEditorText var duo = ConfigManager.warpDuo
        @JvmField @ConfigOption(name = "Auto-party from warp list", desc = "While warping is on, invite players who need a detected sparkling, in priority order, until the reparty cutoff.")
        @ConfigEditorBoolean var autoParty = ConfigManager.autoParty
        @JvmField @ConfigOption(name = "Invite online players only", desc = net.johnceo.sparklingmutuals.alerts.WarpPlayerStatus.DESCRIPTION)
        @ConfigEditorBoolean var autoPartyOnlineOnly = ConfigManager.autoPartyOnlineOnly
        @JvmField @ConfigOption(name = "Warp list", desc = "Choose players and sparklings to invite for. Refresh their sparkling needs every 24 hours when you join Hypixel.")
        @ConfigEditorButton(runnableId = 12, buttonText = "Warp List") var list = false
        @JvmField @ConfigOption(name = "Sparkle warping", desc = "Run /start to save and disband the current party, then reparty and warp them each run.")
        @ConfigEditorBoolean var enabled = ConfigManager.warpAlertsEnabled
        @JvmField @ConfigOption(name = "Leech mode", desc = "Accept invites from whitelisted players. Stay put if your current party leader is already whitelisted.")
        @ConfigEditorBoolean var leech = ConfigManager.leechMode
        @JvmField @ConfigOption(name = "Leech whitelist", desc = "Player names allowed to invite you. An empty list accepts nobody.")
        @ConfigEditorText var whitelist = ConfigManager.leechWhitelist
        @JvmField @ConfigOption(name = "Auto-reconnect to Hypixel", desc = "Retry after an unexpected disconnect. Leave the disconnect screen to cancel.")
        @ConfigEditorBoolean var autoReconnectHypixel = ConfigManager.autoReconnectHypixel
        @JvmField @ConfigOption(name = "Auto-join SkyBlock", desc = "Return from the Prototype Lobby or Limbo. Also join SkyBlock after an automatic reconnect.")
        @ConfigEditorBoolean var autoJoinSkyBlock = ConfigManager.autoJoinSkyBlock
        @JvmField @ConfigOption(name = "Reparty cutoff (seconds)", desc = "Stop auto-party invites and invite your saved party at this run time.")
        @ConfigEditorSlider(minValue=1f,maxValue=29f,minStep=1f) var reparty = ConfigManager.repartyDelaySeconds
        @JvmField @ConfigOption(name = "Auto-warp time (seconds)", desc = "Warp the party at this run time. Must be later than the reparty cutoff.")
        @ConfigEditorSlider(minValue=2f,maxValue=30f,minStep=1f) var delay = ConfigManager.warpDelaySeconds
        @JvmField var saveDelay = false
        @JvmField @ConfigOption(name = "Reset warping timers", desc = "Restore the 22-second reparty cutoff and 27-second warp time.")
        @ConfigEditorButton(runnableId = 5, buttonText = "Reset timers") var reset = false
        @JvmField @ConfigOption(name = "Sparkling Auto-entry", desc = "Automatically use your lowest-tier ticket when the party detects a sparkling.\nUse at your own risk.")
        @DescriptionColor(rgb = 0xFF5555)
        @ConfigEditorBoolean var autoEnter = ConfigManager.safariEntryAutoEnter
    }
    class Safari {
        @JvmField @ConfigOption(name = "Safari re-entry cooldown", desc = "Show the 30-second wait after a failed attempt to rejoin the instance.")
        @Accordion val entryCooldown = HudOptions(ConfigManager.safariEntryCooldown,"entry_cooldown")
        @JvmField @ConfigOption(name = "Safari entry", desc = "") @Accordion val entry = SafariEntrySettings()
        @JvmField @ConfigOption(name = "Sparkling detection and alerts", desc = "") @Accordion val alerts = Alerts()
        @JvmField @ConfigOption(name = "Capture tools", desc = "") @Accordion val capture = SafariCaptureSettings()
        @JvmField @ConfigOption(name = "Biome interactions", desc = "") @Accordion val interactions = SafariInteractionSettings()
        @JvmField @ConfigOption(name = "Party visibility", desc = "") @Accordion val teammates = SafariTeammateSettings()
        @JvmField @ConfigOption(name = "Visual cleanup", desc = "") @Accordion val visibility = SafariVisibilitySettings()
        @JvmField @ConfigOption(name = "Gambling", desc = "") @Accordion val gambling = SafariGamblingSettings()
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
        @JvmField @ConfigOption(name = "Wait for party before entry", desc = "Block entry while party members are missing or a kick is being handled. Hold Shift to bypass.")
        @ConfigEditorBoolean var holdManagerOnKick = ConfigManager.holdManagerOnKick
        @JvmField @ConfigOption(name = "Auto-select Safari ticket", desc = "Choose a ticket for non-leaders after they enter the Safari instance.")
        @ConfigEditorBoolean var autoSelectTicket = ConfigManager.autoSelectSafariTicket
        @JvmField @ConfigOption(name = "Preferred ticket", desc = "Use the closest available tier if your preferred ticket is missing.")
        @ConfigEditorDropdown(values = ["First Class", "Basic"])
        var ticketPreference = ConfigManager.safariTicketPreference
    }
    class SafariCaptureSettings {
        @JvmField @ConfigOption(name = "Recatch waypoint", desc = "Mark where your capsule caught a critter so you can find it again if it escapes.")
        @ConfigEditorBoolean var recatch = ConfigManager.recatchWaypoint
        @JvmField @ConfigOption(name = "Left-click to leave Gazer beds", desc = "Left-click the sleep screen in Haunted to get out of bed.")
        @ConfigEditorBoolean var gazerBed = ConfigManager.leftClickGazerBed
        @JvmField @ConfigOption(name = "Doomspiral throw timer", desc = "Show when to throw a capsule while within 10 blocks of Doomspiral.")
        @ConfigEditorBoolean var doomTimer = ConfigManager.doomspiralThrowTimer
        @JvmField @ConfigOption(name = "Doomspiral emergence delay (ticks)", desc = "Time from the spawn cue until Doomspiral can be caught.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 600f, minStep = 1f) var doomTicks = ConfigManager.doomspiralEmergenceTicks
        @JvmField @ConfigOption(name = "Throw early by (ticks)", desc = "How many ticks before emergence to throw the capsule.")
        @ConfigEditorSlider(minValue = 0f, maxValue = 40f, minStep = 1f) var doomLead = ConfigManager.doomspiralThrowLeadTicks
        @JvmField @ConfigOption(name="Throw timer appearance",desc="") @Accordion
        var doomAppearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("doom_throw")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Capsule trajectory guide", desc = "Preview your capsule's flight path. Works best with view bobbing off.")
        @ConfigEditorBoolean var experimentalCapsulePath = ConfigManager.experimentalCapsulePath
        @JvmField @ConfigOption(name = "Guide line width", desc = "")
        @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f) var capsulePathWidth = ConfigManager.capsulePathWidth
        @JvmField @ConfigOption(name = "Show guide through blocks", desc = "")
        @ConfigEditorBoolean var showCapsuleGuideThroughTerrain = ConfigManager.showCapsuleGuideThroughTerrain
        @JvmField @ConfigOption(name = "Auto-click Rockmite mounds", desc = "Automatically left-click Rockmite mounds.")
        @ConfigEditorBoolean var autoClicker = ConfigManager.autoClicker
    }
    class SafariInteractionSettings {
        @JvmField @ConfigOption(name = "Auto-select podium gems", desc = "Select the matching gem while you hold right-click over a podium.")
        @ConfigEditorBoolean var gemzieGemAutoSelect = ConfigManager.gemzieGemAutoSelect
        @JvmField @ConfigOption(name = "Larger candle hitboxes", desc = "Use a full-block hitbox while holding Soothing Incense.")
        @ConfigEditorBoolean var candleHitbox = ConfigManager.candleHitbox
        @JvmField @ConfigOption(name = "Auto-accept Hideyho quest", desc = "Choose the accept option in Hideyho's dialogue.")
        @ConfigEditorBoolean var hideyhoAutoAccept = ConfigManager.hideyhoAutoAccept
    }
    class SafariTeammateSettings {
        @JvmField @ConfigOption(name = "Highlight teammates", desc = "Show party members through walls in Safari and Torrhus Canyon.")
        @ConfigEditorBoolean var highlightTeammates = ConfigManager.highlightTeammates
        @JvmField @ConfigOption(name = "Show teammate names", desc = "Make party members' names visible through walls.")
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
    class SafariGamblingSettings {
        @JvmField @ConfigOption(name = "Doomspiral Gambling", desc = "Play the Doomspiral lottery when the ritual starts within 15 blocks.")
        @ConfigEditorBoolean var doomspiral = ConfigManager.doomspiralLottery
    }
    class SafariVisibilitySettings {
        @JvmField @ConfigOption(name = "Clear water", desc = "Apply the water opacity settings below in Safari.")
        @ConfigEditorBoolean var clearWater = ConfigManager.clearWater
        @JvmField @ConfigOption(name = "Water opacity by biome", desc = "0% is invisible; 100% is solid.") @Accordion
        val waterOpacity = WaterOpacitySettings()
        @JvmField @ConfigOption(name = "Hide Safari chat", desc = "")
        @ConfigEditorBoolean var hideCaptureChat = ConfigManager.hideCaptureChat
        @JvmField @ConfigOption(name = "Hide chat toggles", desc = "Choose which server messages to hide when Hide Safari chat is on.") @Accordion
        val chatToggles = SafariChatToggleSettings()
        @JvmField @ConfigOption(name = "Hide grounded capsules", desc = "")
        @ConfigEditorBoolean var hideGroundCapsules = ConfigManager.hideGroundCapsules
        @JvmField @ConfigOption(name = "Hide nearby flying capsules", desc = "Hide capsules within the camera distance below.")
        @ConfigEditorBoolean var hideFlyingCapsules = ConfigManager.hideFlyingCapsules
        @JvmField @ConfigOption(name = "Capsule hiding distance (blocks)", desc = "")
        @ConfigEditorSlider(minValue = .5f, maxValue = 6f, minStep = .5f)
        var capsuleHideDistance = ConfigManager.capsuleHideDistance
        @JvmField @ConfigOption(name = "Hide Haunted paintings", desc = "")
        @ConfigEditorBoolean var paintings = ConfigManager.hideHauntedPaintings
        @JvmField @ConfigOption(name = "Hide Icy frost overlay", desc = "Hide the freezing vignette in the Icy Biome.")
        @ConfigEditorBoolean var icyFrost = ConfigManager.removeIcyFrost
        @JvmField @ConfigOption(name = "Remove Safari darkness", desc = "Remove darkness from Haunted encounters.")
        @ConfigEditorBoolean var darkness = ConfigManager.removeDarkness
    }
    class Alerts {
        @JvmField @ConfigOption(name = "Include sparkling catch count", desc = "Show the total recorded catches of that sparkling critter in party chat.")
        @ConfigEditorBoolean var catchCount = ConfigManager.sparklingCatchCount
        @JvmField @ConfigOption(name = "Unique sparkling catch alert", desc = "Celebrate when you catch a sparkling type you did not already have.")
        @ConfigEditorBoolean var caught = ConfigManager.specialCatchAlert
        @JvmField @ConfigOption(name = "Celebrate duplicate sparklings", desc = "Also play the catch celebration for sparkling types you already have.")
        @ConfigEditorBoolean var alwaysCelebrate = ConfigManager.alwaysCelebrateSparkling
        @JvmField @ConfigOption(name = "Celebration style", desc = "")
        @ConfigEditorDropdown(values = ["Prismatic Whisper", "Prismatic Gleam", "Starlight Drift", "Starlight Waltz", "Aurora Rise", "Aurora Cascade", "Comet Shower", "Comet Tempest", "Firework Bloom", "Firework Symphony", "Celestial Chorus", "Celestial Carnival", "Nebula Surge", "Nebula Rhapsody", "Galactic Pulse", "Galactic Tempest", "Astral Reverie", "Astral Ascension", "Supernova", "Hypernova", "Cosmic Jubilee", "Cosmic Overdrive", "Radiant Singularity", "Celestial Transcendence", "Event Horizon", "Infinite Radiance", "Endless Cosmos", "Chromatic Zenith", "Ethereal Genesis", "Ethereal Ascendance", "Mythic Dominion", "Mythic Eternity", "Celestial Genesis", "Celestial Ascendance", "Infinite Transcendence", "Infinite Apotheosis", "Eternal Dawn", "Eternal Resonance", "Eternal Dominion", "Eternal Transcendence", "Eternal Apotheosis", "Custom"]) var caughtStyle = ConfigManager.specialCatchStyle
        @JvmField @ConfigOption(name = "Preview catch celebration", desc = "")
        @ConfigEditorButton(runnableId = 13, buttonText = "Preview") var previewCatch = false
        @JvmField @ConfigOption(name = "Build a Sparkling celebration", desc = "Mix effects, text, and music, then preview or save your design.")
        @ConfigEditorButton(runnableId = 16, buttonText = "Customize") var customCatch = false

        @JvmField @ConfigOption(name = "Detect nearby sparklings", desc = "Look for sparkling critters within 80 blocks in your biome.")
        @ConfigEditorBoolean var shiny = ConfigManager.shinyDetection
        @JvmField @ConfigOption(name = "Nearby sparklings HUD appearance", desc = "") @Accordion
        var nearbyAppearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("sparklings")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Show sparkling detection alert", desc = "Show an alert when you or a connected party member finds a sparkling.")
        @ConfigEditorBoolean var alert = ConfigManager.sparklingAlert
        @JvmField @ConfigOption(name="Detection alert appearance",desc="") @Accordion
        var appearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("alert")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name = "Announce sparklings to party", desc = "Share detected sparklings in party chat.")
        @ConfigEditorBoolean var announce = ConfigManager.sparklingPartyAnnouncer
        @JvmField @ConfigOption(name = "Detection sound", desc = "Minecraft sound ID, such as minecraft:block.note_block.pling.")
        @ConfigEditorText var sound = ConfigManager.sparklingSound
        @JvmField @ConfigOption(name = "Detection sound volume", desc = "0% mutes the sound.")
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
        @JvmField @ConfigOption(name = "Hotspot assignments", desc = "Show party hotspots, assigned biomes and wins while /hotspot is on.") @Accordion val hotspotHud=HotspotHudOptions()
        @JvmField @ConfigOption(name = "Run progress", desc = "Track overall and per-biome completion, time and profit.") @Accordion val progressHud=ProgressHudOptions()
        @JvmField @ConfigOption(name = "Safari splits", desc = "Show when each biome was completed in Unique and Full Clear runs.") @Accordion val splitsHud=SplitsHudOptions()
        @JvmField @ConfigOption(name = "Missing critters", desc = "Show critters still needed for the current mode.") @Accordion val missingHud=HudOptions(ConfigManager.missingPanel,"missing")
        @JvmField @ConfigOption(name = "Profitable critters", desc = "Show the current biome's ten most profitable critters and captures remaining. Unique and Sparkling modes only.") @Accordion val profitableHud=HudOptions(ConfigManager.profitableChecklist,"profitable")
        @JvmField @ConfigOption(name = "Lake cleared alert", desc = "Catch one Tepid, Nozzlenose and Mantis Shrimp in Unique or Sparkling mode. In Full Clear, catch them all.") @Accordion val lakeClearedHud=HudOptions(ConfigManager.lakeClearAlert,"lake_cleared")
        @JvmField @ConfigOption(name = "Biome objectives", desc = "Track food, nests, mounds and other remaining biome tasks.") @Accordion val objectivesHud=ObjectivesHudOptions()
        @JvmField @ConfigOption(name = "Biome captures", desc = "Show capture counts and completion checks for each critter.") @Accordion val capturesHud=HudOptions(ConfigManager.catchCountPanel,"captures")
        @JvmField @ConfigOption(name = "Miria's contest", desc = "Show contest time, ticket progress and optional bracket and score rows.") @Accordion val contestHud=ContestHudOptions()
        @JvmField @ConfigOption(name = "Show Safari HUDs", desc = "Choose where to display the Safari tracking panels.")
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
    class HotspotHudOptions {
        @JvmField @ConfigOption(name = "Enable HUD", desc = "") @ConfigEditorBoolean var enabled = ConfigManager.hotspotHudEnabled
        @JvmField @ConfigOption(name = "Show all party members", desc = "Turn off to show only your own assignment.") @ConfigEditorBoolean
        var showAllPlayers=ConfigManager.hotspotShowAllPlayers
        @JvmField @ConfigOption(name="Appearance",desc="") @Accordion
        var appearance=PanelAppearanceSettings(AppearanceConfig.panels.getValue("hotspot")) { AppearanceConfig.selectedHex }
        @JvmField @ConfigOption(name="Position and size",desc="") @ConfigEditorButton(buttonText="Edit HUDs")
        val placement=Runnable { val client=Minecraft.getInstance();client.smSetScreen(ContestGui(client.smScreen)) }
    }
    class ObjectivesHudOptions: HudOptions(ConfigManager.biomeObjectives,"objectives") {
        @JvmField @ConfigOption(name = "Show capture reminder", desc = "Show the Capture all uniques or Capture all critters line while captures are missing.") @ConfigEditorBoolean var captureReminder=ConfigManager.objectiveCaptureReminder
        @JvmField @ConfigOption(name = "Show sparkling-check reminder", desc = "Show a reminder while sparkling checks are still needed.") @ConfigEditorBoolean var shinyReminder=ConfigManager.objectiveShinyReminder
        @JvmField @ConfigOption(name = "HUD rows", desc = "Choose what appears on this HUD.") @Accordion
        val rows = BiomeObjectiveToggles.rows()
    }
    class ProgressRows {
        @JvmField @ConfigOption(name = "Overall progress", desc = "")
        @ConfigEditorDropdown(values = ["Bar and count", "Count only", "Hidden"])
        var overall = ConfigManager.progressRow(null).ordinal
        @JvmField @ConfigOption(name = "Forest", desc = "")
        @ConfigEditorDropdown(values = ["Bar and count", "Count only", "Hidden"])
        var forest = ConfigManager.progressRow(SafariBiome.FOREST).ordinal
        @JvmField @ConfigOption(name = "Cavern", desc = "")
        @ConfigEditorDropdown(values = ["Bar and count", "Count only", "Hidden"])
        var cavern = ConfigManager.progressRow(SafariBiome.CAVERN).ordinal
        @JvmField @ConfigOption(name = "Icy", desc = "")
        @ConfigEditorDropdown(values = ["Bar and count", "Count only", "Hidden"])
        var icy = ConfigManager.progressRow(SafariBiome.ICY).ordinal
        @JvmField @ConfigOption(name = "Haunted", desc = "")
        @ConfigEditorDropdown(values = ["Bar and count", "Count only", "Hidden"])
        var haunted = ConfigManager.progressRow(SafariBiome.HAUNTED).ordinal

        internal fun values() = mapOf(null to overall, SafariBiome.FOREST to forest, SafariBiome.CAVERN to cavern,
            SafariBiome.ICY to icy, SafariBiome.HAUNTED to haunted).mapValues { (_, selection) ->
                ProgressRowDisplay.entries.getOrElse(selection) { ProgressRowDisplay.BAR_AND_COUNT }
            }
    }
    class ProgressHudOptions: HudOptions(ConfigManager.progressHud,"progress") {
        @JvmField @ConfigOption(name = "Show progress bars", desc = "Show the graphical bars beside the progress counts.")
        @ConfigEditorBoolean var tracks = ConfigManager.progressTracks
        @JvmField @ConfigOption(name = "Progress rows", desc = "Choose which rows to show. Hide all rows, profit and hotspot to show only the timer.")
        @Accordion val rows = ProgressRows()
        @JvmField @ConfigOption(name = "Show profit", desc = "Show estimated profit from shards, feathers and essence.") @ConfigEditorBoolean var profit=ConfigManager.progressProfit
        @JvmField @ConfigOption(name = "Show hotspot", desc = "Show the hotspot Hypixel gave you for this run.") @ConfigEditorBoolean var hotspot=ConfigManager.progressHotspot
    }
    class SplitsHudOptions: HudOptions(ConfigManager.splitsHud, "splits") {
        @JvmField @ConfigOption(name = "Time format", desc = "Choose how precisely the HUD shows splits and the session average.")
        @ConfigEditorDropdown(values = ["Milliseconds (1:48.150)", "Hundredths (1:48.15)", "Tenths (1:48.1)", "Seconds (1:48)"])
        var timeFormat = ConfigManager.splitsTimeFormat.ordinal
        @JvmField @ConfigOption(name = "Show session stats", desc = "Show average run time, run count and credited sparklings for the current session.")
        @ConfigEditorBoolean var showSession = ConfigManager.splitsShowSession
    }
    class ContestHudOptions: HudOptions(ContestConfig.showContestTimer,"miria") {
        @JvmField @ConfigOption(name = "Show bracket", desc = "") @ConfigEditorBoolean var bracket=ContestConfig.showBracket
        @JvmField @ConfigOption(name = "Show score", desc = "") @ConfigEditorBoolean var score=ContestConfig.showScore
    }
    class Reset {
        @JvmField @ConfigOption(name = "Restore default settings", desc = "Restore fresh-install settings and HUD layouts. This does not reset saved Safari records.")
        @ConfigEditorButton(runnableId = 14, buttonText = "Reset to Defaults") var defaults = false
        @JvmField @ConfigOption(name = "Turn off features", desc = "Turn off all feature switches, including HUDs and automation. You can turn them back on individually.")
        @ConfigEditorButton(runnableId = 15, buttonText = "Disable All Features") var disable = false
    }

    class Api {
        @JvmField @ConfigOption(name = "Use Shiny Hunter API", desc = "Fetch player data without your own API key. A personal key takes priority.")
        @ConfigEditorBoolean var cloud = ConfigManager.useCloudApi
        @JvmField @ConfigOption(name = "Hypixel API key", desc = "Add your own key for online-status checks or as an alternative to Shiny Hunter.")
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
            5 -> { warp.reparty=22;warp.delay=27;saveDelay() }
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
            16 -> (client.smScreen as? SafariConfigScreen)?.let {
                apply()
                client.smSetScreen(CustomCatchCelebrationScreen(it))
            }
            12 -> { apply(); client.execute { client.smSetScreen(net.johnceo.sparklingmutuals.alerts.WarpListScreen(client.smScreen,
                autoPartyChanged = { warp.autoParty = it }, onlineOnlyChanged = { warp.autoPartyOnlineOnly = it })) } }
            14 -> (client.smScreen as? SafariConfigScreen)?.confirmBulkAction(resetDefaults = true)
            15 -> (client.smScreen as? SafariConfigScreen)?.confirmBulkAction(resetDefaults = false)
            11 -> { apply(); client.execute { LogImporter.runImport() } }
        }
    }
    fun isValidRunnable(id: Int) = id in 1..15
    private fun saveDelay() {
        val seconds=warp.delay.coerceIn(1,86400)
        warp.reparty=ConfigManager.validRepartyDelay(warp.reparty,seconds)
        AlertManager.setTimings(warp.reparty,seconds)
    }
    private fun saveSound() {
        val sound = miria.sound.trim()
        if (Identifier.tryParse(sound) == null) feedback("Enter a valid Minecraft sound identifier.")
        else if (ContestConfig.contestSound != sound) { ContestConfig.contestSound = sound; ContestConfig.save() }
    }
    private fun saveSparklingSound(): Boolean {
        val sound = safari.alerts.sound.trim()
        if (Identifier.tryParse(sound) == null) { feedback("Enter a valid Minecraft sound identifier."); return false }
        if (ConfigManager.sparklingSound != sound) { ConfigManager.sparklingSound = sound; ConfigManager.saveSettings() }
        return true
    }
    fun saveTextFields() {
        val historyUuid = net.johnceo.sparklingmutuals.history.HistoryAccountId.normalize(history.ownerUuid)
        if (historyUuid != null) ConfigManager.historyOwnerUuid = historyUuid
        else if (history.ownerUuid.isNotBlank()) {
            feedback("Enter a valid Minecraft UUID.")
            history.ownerUuid = ConfigManager.historyOwnerUuid
        }
        saveDelay(); saveSound(); saveSparklingSound()
        val sound = Identifier.tryParse(safari.alerts.runSound.trim())
        if (sound == null) feedback("Enter a valid Minecraft sound identifier for run completion.")
        else ConfigManager.runCompleteSound = sound.toString()
        ConfigManager.runCompleteTitle = safari.alerts.runTitle.take(120).ifBlank { "Run Complete" }
        ConfigManager.saveSettings()
    }
    private var lastAppliedMode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
    fun apply(notifyMode: Boolean = false) {
        saveDelay()
        val currentMode = net.johnceo.sparklingmutuals.safari.completion.SafariFullClear.mode
        if (currentMode != lastAppliedMode) {
            tracking.captures = ConfigManager.catchCountPanel
            lastAppliedMode = currentMode
        }
        val fontChanged = AppearanceConfig.resourcePackFont != general.font.resourcePackFont ||
            AppearanceConfig.hudTextShadow != general.font.hudTextShadow || AppearanceConfig.pixelAlignedHuds != general.font.pixelAlignedHuds
        AppearanceConfig.resourcePackFont = general.font.resourcePackFont
        AppearanceConfig.hudTextShadow = general.font.hudTextShadow
        AppearanceConfig.pixelAlignedHuds = general.font.pixelAlignedHuds
        val bannerChanged = banner.apply() || fontChanged
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
        val changes = listOf(ConfigManager::objectiveCaptureReminder to tracking.objectivesHud.captureReminder,ConfigManager::objectiveShinyReminder to tracking.objectivesHud.shinyReminder,
            ConfigManager::doomSoloPbChat to party.pbChat.doom.solo, ConfigManager::doomDuoPbChat to party.pbChat.doom.duo,
            ConfigManager::doomObservedSoloPbChat to party.pbChat.doom.observedSolo, ConfigManager::doomObservedDuoPbChat to party.pbChat.doom.observedDuo,
            ConfigManager::wumpaSoloPbChat to party.pbChat.wumpa.solo, ConfigManager::wumpaDuoPbChat to party.pbChat.wumpa.duo,
            ConfigManager::wumpaObservedSoloPbChat to party.pbChat.wumpa.observedSolo, ConfigManager::wumpaObservedDuoPbChat to party.pbChat.wumpa.observedDuo,
            ConfigManager::fullClearPbChat to party.pbChat.fullClear, ConfigManager::uniquePbChat to party.pbChat.unique,
            ConfigManager::partyUniquePbChat to party.pbChat.partyUnique,
            ConfigManager::autoReconnectHypixel to warp.autoReconnectHypixel,
            ConfigManager::autoJoinSkyBlock to warp.autoJoinSkyBlock,
            ConfigManager::safariEntryAutoEnter to warp.autoEnter, ConfigManager::sparklingCatchCount to safari.alerts.catchCount, ConfigManager::specialCatchAlert to safari.alerts.caught,
            ConfigManager::alwaysCelebrateSparkling to safari.alerts.alwaysCelebrate,
            ConfigManager::allScannableCritterChecks to modes.sparkling.allScannable,
            ConfigManager::safariEntryCooldown to safari.entryCooldown.enabled,
            ConfigManager::doomspiralThrowTimer to safari.capture.doomTimer, ConfigManager::doomspiralLottery to safari.gambling.doomspiral,
            ConfigManager::autoParty to warp.autoParty, ConfigManager::autoPartyOnlineOnly to warp.autoPartyOnlineOnly, ConfigManager::autoBiomeReports to party.biomeReports,
            ConfigManager::uniqueHideCritterEsp to modes.unique.hideCritters, ConfigManager::uniqueHideFloorDropEsp to modes.unique.hideDrops,
            ConfigManager::fullClearHideCritterEsp to modes.fullClear.hideCritters, ConfigManager::fullClearHideFloorDropEsp to modes.fullClear.hideDrops,
            ConfigManager::sparklingHideCritterEsp to modes.sparkling.hideCritters, ConfigManager::sparklingHideFloorDropEsp to modes.sparkling.hideDrops, ConfigManager::announceStartingItems to party.startingItems, ConfigManager::huntingBoxValue to skyblock.helpers.huntingBoxValue, ConfigManager::shardSaleGuidance to skyblock.helpers.shardSaleGuidance, ConfigManager::skillOverflow to skyblock.helpers.skillOverflow, ConfigManager::skillOverflowChat to skyblock.helpers.skillOverflowChat, ConfigManager::skillOverflowNumbers to skyblock.helpers.skillOverflowNumbers, ConfigManager::estimatedItemValue to skyblock.helpers.estimatedItemValue, ConfigManager::estimatedChestValue to skyblock.helpers.estimatedChestValue, ConfigManager::phantomleafSolver to skyblock.helpers.phantomleafSolver, ConfigManager::highlightTeammates to safari.highlightTeammates, ConfigManager::showTeammateNames to safari.showTeammateNames, ConfigManager::profitableShardEsp to modes.unique.profitable, ConfigManager::sparklingProfitableShardEsp to modes.sparkling.profitable, ConfigManager::partyCommandsEnabled to party.enabled, ConfigManager::downtimeCommand to party.downtime, ConfigManager::useCloudApi to api.cloud, ConfigManager::hideHauntedPaintings to safari.paintings,
            ConfigManager::shinyDetection to safari.shiny, ConfigManager::hideyhoAutoAccept to safari.hideyhoAutoAccept, ConfigManager::autoSelectSafariTicket to safari.autoSelectTicket,
            ConfigManager::progressHud to tracking.progress, ConfigManager::splitsHud to tracking.splitsHud.enabled, ConfigManager::progressProfit to tracking.progressHud.profit, ConfigManager::progressHotspot to tracking.progressHud.hotspot, ConfigManager::hotspotShowAllPlayers to tracking.hotspotHud.showAllPlayers, ConfigManager::catchCountPanel to tracking.captures,
            ConfigManager::experimentalCapsulePath to safari.experimentalCapsulePath,
            ConfigManager::gemzieGemAutoSelect to safari.gemzieGemAutoSelect,
            ConfigManager::showCapsuleGuideThroughTerrain to safari.showCapsuleGuideThroughTerrain,
            ConfigManager::profitableChecklist to tracking.profitableHud.enabled, ConfigManager::lakeClearAlert to tracking.lakeClearedHud.enabled, ConfigManager::missingPanel to tracking.missing, ConfigManager::biomeObjectives to tracking.objectives,
            ConfigManager::removeIcyFrost to safari.visibility.icyFrost,
            ConfigManager::recatchWaypoint to safari.capture.recatch,
            ConfigManager::leftClickGazerBed to safari.capture.gazerBed,
            ConfigManager::removeDarkness to safari.darkness,
            ConfigManager::clearWater to safari.visibility.clearWater,
            ConfigManager::sparklingAlert to safari.alert, ConfigManager::sparklingPartyAnnouncer to safari.announce,
            ConfigManager::ubiksCubeTimer to skyblock.helpers.ubiksCubeTimer.enabled,
            ConfigManager::progressTracks to tracking.progressHud.tracks,
            ConfigManager::critterEspEnabled to espGeneral.critterEsp,
            ConfigManager::floorDropEspEnabled to espGeneral.floorDropEsp,
            ConfigManager::warpListRefresh to warp.refresh,
            ConfigManager::customCritterNames to esp.names.enabled,
            ConfigManager::hotspotHudEnabled to tracking.hotspotHud.enabled,
            ConfigManager::splitsShowSession to tracking.splitsHud.showSession,
            ConfigManager::autoClicker to safari.autoClicker, ConfigManager::candleHitbox to safari.candleHitbox, ConfigManager::holdManagerOnKick to safari.holdManagerOnKick, ConfigManager::honeyHives to miria.honeyHives, ConfigManager::hideCaptureChat to safari.hideCaptureChat, ConfigManager::achievements to history.achievements, ConfigManager::achievementSound to history.sound, ConfigManager::achievementsToParty to history.toParty, ConfigManager::runCoinsInstaSell to history.instaSell,
            ConfigManager::allGemsAlert to safari.alerts.gems, ConfigManager::allBirdFoodAlert to safari.alerts.birdFood,
            ConfigManager::allCoinsAlert to safari.alerts.coins, ConfigManager::allIncenseAlert to safari.alerts.incense, ConfigManager::runCompleteAlert to safari.alerts.runComplete,
            ConfigManager::hideGroundCapsules to safari.hideGroundCapsules, ConfigManager::hideFlyingCapsules to safari.hideFlyingCapsules)
        val espChanged = listOf(espGeneral.apply(),esp.apply()).any {it}
        val modWideCrittersChanged = modWideCritters.apply(SafariEspConfig.modWideToggles)
        val profitableCrittersChanged = profitableCritters.apply(SafariEspConfig.profitableToggles)
        val hudAppearanceChanged = listOf(safari.entryCooldown.appearance,tracking.profitableHud.appearance,tracking.lakeClearedHud.appearance,
            skyblock.helpers.ubiksCubeTimer.appearance).map {it.apply()}.any {it}
        val customizationChanged = customization.apply() || hudAppearanceChanged
        val distance = ConfigManager.validCapsuleDistance(safari.capsuleHideDistance)
        val pathWidth = ConfigManager.validCapsulePathWidth(safari.capsulePathWidth)
        val waterOpacity = safari.visibility.waterOpacity.values()
        val alwaysOnCrittersChanged = alwaysOnCritters.apply(SafariEspConfig.alwaysOnToggles)
        val alwaysOnFloorDropsChanged = alwaysOnFloorDrops.apply()
        val chatTogglesChanged = safari.visibility.chatToggles.apply()
        val objectivesChanged = BiomeObjectiveConfig.apply(tracking.objectivesHud.rows, modes.sparklingChecks)
        val remainingChecksChanged = modes.unique.applyRemainingChecks()
        val splitTimeFormat = SplitTimeFormat.entries.getOrElse(tracking.splitsHud.timeFormat) { SplitTimeFormat.MILLISECONDS }
        val progressRows = tracking.progressHud.rows.values()
        val generalChanged = ConfigManager.progressRows != progressRows || espChanged || modWideCrittersChanged || profitableCrittersChanged || alwaysOnCrittersChanged || alwaysOnFloorDropsChanged || chatTogglesChanged || objectivesChanged || remainingChecksChanged ||
            ConfigManager.splitsTimeFormat != splitTimeFormat ||
            bannerChanged || ConfigManager.specialCatchStyle != safari.alerts.caughtStyle || whitelistChanged || warpModesChanged || warpConfigurationChanged || ConfigManager.doomspiralEmergenceTicks != safari.capture.doomTicks.coerceIn(1, 600) ||
            ConfigManager.doomspiralThrowLeadTicks != safari.capture.doomLead.coerceIn(0, 40) || ConfigManager.biomeWaterOpacity != waterOpacity || ConfigManager.capsulePathWidth != pathWidth || customizationChanged || changes.any { (property, value) -> property.get() != value } ||
            ConfigManager.safariTicketPreference != safari.ticketPreference || ConfigManager.showWhere != tracking.where || ConfigManager.capsuleHideDistance != distance ||
            ConfigManager.runCompleteVolume != safari.alerts.runVolume.coerceIn(0, 100) || ConfigManager.sparklingSoundVolume != safari.alerts.volume.coerceIn(0, 100)
        val profitPriceChanged = ConfigManager.runCoinsInstaSell != history.instaSell
        changes.forEach { (property, value) -> property.set(value) }
        ConfigManager.safariTicketPreference = safari.ticketPreference.coerceIn(0, 1)
        ConfigManager.showWhere = tracking.where.coerceIn(0, 2)
        ConfigManager.splitsTimeFormat = splitTimeFormat
        ConfigManager.progressRows = progressRows
        ConfigManager.capsuleHideDistance = distance
        ConfigManager.biomeWaterOpacity = waterOpacity
        ConfigManager.capsulePathWidth = pathWidth
        ConfigManager.doomspiralEmergenceTicks = safari.capture.doomTicks.coerceIn(1, 600)
        ConfigManager.doomspiralThrowLeadTicks = safari.capture.doomLead.coerceIn(0, 40)
        ConfigManager.specialCatchStyle = safari.alerts.caughtStyle.coerceIn(0, net.johnceo.sparklingmutuals.ui.SparklingAlertStyle.CUSTOM_INDEX)
        ConfigManager.runCompleteVolume = safari.alerts.runVolume.coerceIn(0, 100)
        ConfigManager.sparklingSoundVolume = safari.alerts.volume.coerceIn(0, 100)
        if (generalChanged) ConfigManager.saveSettings()
        if (profitPriceChanged) net.johnceo.sparklingmutuals.history.RunHistory.reprice()
        val warnings = listOfNotNull(5.takeIf { miria.five }, 3.takeIf { miria.three }, 1.takeIf { miria.one })
        val ticketMessageChanged=miria.applyTicketMessage()
        val contestChanged = ticketMessageChanged || ContestConfig.announceTicket!=miria.announceTicket || ContestConfig.showBracket != tracking.contestHud.bracket || ContestConfig.showScore != tracking.contestHud.score || ContestConfig.showContestTimer != tracking.contestTimer || ContestConfig.ticketMode != miria.ticketMode || ContestConfig.autoContestDowntime != miria.autoContestDowntime || ContestConfig.trackContest != miria.enabled || ContestConfig.contestWarnTitle != miria.titles ||
            ContestConfig.contestSoundVolume != miria.volume || ContestConfig.warningMinutes() != warnings
        ContestConfig.announceTicket=miria.announceTicket
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
            ConfigManager.saveSettings()
        }
    }
}
