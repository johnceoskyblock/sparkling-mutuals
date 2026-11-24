package net.johnceo.sparklingmutuals.config

/** Display order only. Field names remain stable for saved values, search and HUD edit links. */
internal object SettingsOrder {
    private val fields = mapOf(
        "General" to listOf("edit", "font", "help"),
        "Modes" to listOf("unique", "fullClear", "sparkling", "sparklingChecks"),
        "Unique" to listOf("profitable", "hideCritters", "hideDrops"),
        "Sparkling" to listOf("allScannable", "profitable", "hideCritters", "hideDrops"),
        "Party" to listOf("hotspotPreferences", "biomeReports", "startingItems", "downtime", "enabled", "pbChat"),
        "Miria" to listOf("enabled", "ticketMode", "announceTicket", "ticketMessage", "autoContestDowntime", "honeyHives"),
        "History" to listOf("open", "instaSell", "importLogs", "achievements", "sound", "toParty"),
        "Warp" to listOf("description", "enabled", "mode", "duo", "reparty", "delay", "reset",
            "autoParty", "autoPartyOnlineOnly", "list", "refresh", "leech", "whitelist", "autoReconnectHypixel", "autoJoinSkyBlock", "autoEnter"),
        "Safari" to listOf("alerts", "capture", "interactions", "entry", "entryCooldown", "teammates", "visibility", "gambling"),
        "SafariCaptureSettings" to listOf("experimentalCapsulePath", "capsulePathWidth", "showCapsuleGuideThroughTerrain",
            "autoClicker", "doomTimer", "doomTicks", "doomLead", "doomAppearance"),
        "SafariEntrySettings" to listOf("autoSelectTicket", "ticketPreference", "holdManagerOnKick"),
        "SafariVisibilitySettings" to listOf("hideCaptureChat", "chatToggles", "clearWater", "waterOpacity",
            "darkness", "hideGroundCapsules", "hideFlyingCapsules", "capsuleHideDistance", "paintings"),
        "Alerts" to listOf("shiny", "alert", "appearance", "sound", "volume", "saveSound", "announce", "nearbyAppearance",
            "caught", "alwaysCelebrate", "caughtStyle", "previewCatch", "catchCount"),
        "Tracking" to listOf("where", "progressHud", "splitsHud", "missingHud", "objectivesHud", "capturesHud",
            "contestHud", "hotspotHud", "profitableHud", "lakeClearedHud"),
        "HotspotHudOptions" to listOf("enabled", "showAllPlayers", "appearance", "placement"),
        "SkyBlockHelpers" to listOf("shardSaleGuidance", "huntingBoxValue", "skillOverflow", "skillOverflowChat", "skillOverflowNumbers", "ubiksCubeTimer", "phantomleafSolver"),
        "BannerSettings" to listOf("enabled", "safari", "encounters", "contest", "appearance", "placement", "preview"),
        "SafariBannerOptions" to listOf("complete", "biome", "fullParty", "warp", "partyKicked"),
        "EncountersBanners" to listOf("hideyho", "gems", "food", "feeder", "incense", "coins"),
        "ContestBanners" to listOf("ticket", "start", "five", "three", "one", "end"),
        "BannerOption" to listOf("enabled", "playback", "text", "color", "duration", "sound", "volume", "pitch", "preview"),
        "PanelAppearanceSettings" to listOf("text", "background", "transparency", "borderEnabled", "border", "expansion"),
        "BannerAppearance" to listOf("font", "shadow", "background", "color", "gradient", "tint", "border", "thickness", "topBar", "bottomBar", "expansion"),
        "NameplateSettings" to listOf("enabled", "display", "distance", "pity", "eagle", "critters", "colors"),
        "NameplateColors" to listOf("uniqueColors", "rarityColors", "fixedColor", "color", "entityColorOverride"),
        "EspGeneralSettings" to listOf("critterEsp", "floorDropEsp", "glow", "boxes", "biomes", "rewardDrops", "floorVisibility")
    )

    fun guide(field: String): String? = when(field) {
        "modWideCritters" -> if (SafariSettings.standardEdition) "Choose which visible critters your current mode can highlight."
            else "Choose the critters your current mode can highlight. Turning one off removes it from mode-based ESP."
        "profitableCritters" -> if (SafariSettings.standardEdition) "Choose which visible critters the profitable highlights setting can show."
            else "Choose which critters can use profitable ESP. Enable Profitable critter ESP in the mode you use."
        "alwaysOnCritters" -> if (SafariSettings.standardEdition) "Highlight these critters whenever you can see them, even after your mode is done with them."
            else "Highlight these critters even when your mode does not need them. Global ESP visibility still applies."
        "alwaysOnFloorDrops" -> "Highlight these floor drops even after their objective is complete. Global ESP visibility still applies."
        else -> null
    }

    fun rows(owner: Any, rows: List<SettingsCatalog.Row>): List<SettingsCatalog.Row> {
        val order = fields[owner.javaClass.simpleName] ?: if (owner is SafariSettings.HudOptions)
            listOf("enabled", "timeFormat", "showSession", "tracks", "profit", "hotspot", "captureReminder", "shinyReminder", "rows", "bracket", "score", "appearance", "placement")
        else return rows
        // Unknown/new fields keep their declared order after the explicitly arranged controls.
        return rows.sortedBy { order.indexOf(it.navigationName).takeIf { index -> index >= 0 } ?: order.size }
    }
}

/** Context shared by the repeated critter/item switches, without a description under every name. */
internal class SettingsPageGuide {
    @JvmField @StandaloneDescription @ConfigOption(name="",desc="") val guide=false
}
