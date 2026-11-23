package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SplitTimeFormat
import net.johnceo.sparklingmutuals.safari.completion.BossPbCategory
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.completion.SafariPersonalBests
import net.minecraft.resources.Identifier

object ConfigManager {
    private lateinit var configPath: Path
    private lateinit var personalBestConfig: PersonalBestConfig
    @Volatile var apiKey = ""
    var partyCommandsEnabled = true
    var doomSoloPbChat = true
    var doomDuoPbChat = true
    var doomObservedSoloPbChat = true
    var doomObservedDuoPbChat = true
    var wumpaSoloPbChat = true
    var wumpaDuoPbChat = true
    var wumpaObservedSoloPbChat = true
    var wumpaObservedDuoPbChat = true
    var fullClearPbChat = true
    var uniquePbChat = true
    var partyUniquePbChat = true
    fun showBossPbChat(species: String, category: BossPbCategory): Boolean {
        val wumpa = species == "Wumpa"
        return when (category) {
            BossPbCategory.SOLO -> if (wumpa) wumpaSoloPbChat else doomSoloPbChat
            BossPbCategory.DUO -> if (wumpa) wumpaDuoPbChat else doomDuoPbChat
            BossPbCategory.OBSERVED_SOLO -> if (wumpa) wumpaObservedSoloPbChat else doomObservedSoloPbChat
            BossPbCategory.OBSERVED_DUO -> if (wumpa) wumpaObservedDuoPbChat else doomObservedDuoPbChat
        }
    }
    var announceStartingItems = false
    var autoBiomeReports = true
    var hudSnapping = true
    var uniqueHideCritterEsp = false
    var uniqueHideFloorDropEsp = false
    var fullClearHideCritterEsp = false
    var fullClearHideFloorDropEsp = false
    var sparklingHideCritterEsp = false
    var sparklingHideFloorDropEsp = false
    fun hideCritterEsp(mode: SafariMode = SafariFullClear.mode) = !critterEspEnabled || when(mode) {
        SafariMode.UNIQUE -> uniqueHideCritterEsp
        SafariMode.FULL_CLEAR -> fullClearHideCritterEsp
        SafariMode.SPARKLING -> sparklingHideCritterEsp
    }
    fun hideFloorDropEsp(mode: SafariMode = SafariFullClear.mode) = !floorDropEspEnabled || when(mode) {
        SafariMode.UNIQUE -> uniqueHideFloorDropEsp
        SafariMode.FULL_CLEAR -> fullClearHideFloorDropEsp
        SafariMode.SPARKLING -> sparklingHideFloorDropEsp
    }
    var useCloudApi = true
    var fullClearMode = false
    var sparklingMode = false
    var allScannableCritterChecks = false
    var sparklingProfitableShardEsp = true
    var profitableShardEsp = true
    var hotspotAnnouncer = false
    var hotspotShowAllPlayers = true
    var uniqueHotspotAnnouncer = false
    var fullClearHotspotAnnouncer = false
    var sparklingHotspotAnnouncer = false
    var timesaveOnly = true
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var warpAlertsEnabled: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var warpDelaySeconds = 27
    var repartyDelaySeconds = 22
    fun validRepartyDelay(value: Int?,warpSeconds: Int=warpDelaySeconds) =
        (value ?: 22).coerceIn(1,minOf(29,(warpSeconds-1).coerceAtLeast(1)))
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoParty: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoPartyOnlineOnly: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var sparkleWarpMode = 0
    var warpDuo = ""
    var settingsTheme = 4
    var sparklingCatchCount = true
    var specialCatchAlert = true
    var alwaysCelebrateSparkling = false
    var specialCatchStyle = 0
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var leechMode: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoReconnectHypixel: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoJoinSkyBlock: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var leechWhitelist = "JohnCEO _cj Rqach johnkuudra"
    var safariEntryCooldown = true
    var doomspiralThrowTimer = true
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var doomspiralLottery = true
    var doomspiralEmergenceTicks = 98
    var doomspiralThrowLeadTicks = 4
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var hideyhoAutoAccept: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoSelectSafariTicket: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var safariEntryAutoEnter: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var safariTicketPreference = 0
    var profitsTicketMode = 0
    var hideyhoInsideOutside = true
    var hideHauntedPaintings: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var shinyDetection = true
    var critterEspEnabled = true
    var floorDropEspEnabled = true
    var customCritterNames = true
        set(value) {
            if (field != value) net.johnceo.sparklingmutuals.safari.ui.CritterNames.invalidateDisplay()
            field = value
        }
    var hotspotHudEnabled = true
    var progressHud = true
    var progressTracks = true
    internal var progressRows = emptyMap<SafariBiome?, ProgressRowDisplay>()
    internal fun progressRow(biome: SafariBiome?) = progressRows[biome] ?: ProgressRowDisplay.BAR_AND_COUNT
    private fun progressRowKey(biome: SafariBiome?) = "progressRow." + (biome?.name?.lowercase() ?: "overall")
    var splitsHud = true
    var splitsShowSession = true
    internal var splitsTimeFormat = SplitTimeFormat.MILLISECONDS
    var progressProfit = true
    var progressHotspot = true
    var countUniqueOnly = false
    var profitableChecklist = true
    var lakeClearAlert = true
    var missingPanel = true
    var biomeObjectives = true
    var objectiveCaptureReminder = true
    var objectiveShinyReminder = true
    internal var installedVersionHistory = InstalledVersionHistory()
        private set
    var highlightBeeNests = true
    var highlightSnooperWalls = true
    var showBeeNests = true
    var showMoundCount = true
    var showSnooperWalls = true
    var showMoundStats = false
    var removeDarkness = true
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var clearWater: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var waterOpacity = 60
        set(value) { field = value.coerceIn(0, 100) }
    var biomeWaterOpacity: Map<SafariBiome, Int> = emptyMap()
    fun waterOpacityFor(biome: SafariBiome) = biomeWaterOpacity[biome] ?: waterOpacity

    var sparklingAlert = true
    var sparklingSound = "minecraft:block.amethyst_block.chime"
    var sparklingSoundVolume = 100
    var allGemsAlert = true
    var allBirdFoodAlert = true
    var allCoinsAlert = true
    var allIncenseAlert = true
    var runCompleteAlert = true
    var runCompleteTitle = "Run Complete"
    var runCompleteSound = "minecraft:block.bell.use"
    var runCompleteVolume = 100
    var sparklingPartyAnnouncer = true
    var catchCountPanel = false
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var autoClicker: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var gemzieGemAutoSelect: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var candleHitbox: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    // Inert compatibility binding: retired settings cannot enable code in this edition.
    var holdManagerOnKick: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var downtimeCommand = true
    var honeyHives = true
    var skillOverflow = true
    var shardSaleGuidance = true
    var skillOverflowChat = true
    var skillOverflowNumbers = false
    var estimatedItemValue = true
    var estimatedChestValue = true
    var huntingBoxValue = true
    var ubiksCubeTimer = false
    var phantomleafSolver: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {}
    var achievements = true
    var achievementSound = true
    var achievementsToParty = false
    var runCoinsInstaSell = false
    var hideCaptureChat = true
    var hideGroundCapsules = true
    var hideFlyingCapsules = true
    var experimentalCapsulePath = true
    var capsulePathWidth = 3f
    var showCapsuleGuideThroughTerrain = true
    var capsuleHideDistance = 2f
    var showWhere = 1
    val personalBests = SafariPersonalBests()
    private val fullOnlySettings = setOf(
        "highlightTeammates", "showTeammateNames", "esp.critterBoxes", "esp.scrappy.textScale",
        "autoClicker", "autoJoinSkyBlock", "autoParty", "autoPartyOnlineOnly", "autoReconnectHypixel", "autoSelectSafariTicket",
        "candleHitbox", "clearWater", "gemzieGemAutoSelect", "esp.floor.rewards.enabled",
        "hideHauntedPaintings", "hideyhoAutoAccept", "holdManagerOnKick", "leechMode", "leechWhitelist", "phantomleafSolver",
        "repartyDelaySeconds", "safariEntryAutoEnter", "safariTicketPreference", "sparkleWarpMode",
        "warpAlertsEnabled", "warpDelaySeconds", "warpDuo", "warpListRefresh", "waterOpacity", "migration.sparkleWarpTimers22_27"
    )

    private fun preserveFullSettings(saved: Properties, output: Properties, preserveInviteList: Boolean = false) {
        for (key in saved.stringPropertyNames()) {
            val automatedInvite = key.startsWith("warpList.player.") &&
                (key.endsWith(".alwaysInvite") || key.endsWith(".discoveryCheckedAt"))
            val floorAppearance = key.startsWith("esp.floor.") &&
                (key.endsWith(".alwaysOn") || key.endsWith(".onlyInBiome") ||
                    key.endsWith(".textScale") || key.endsWith(".color") && key != "esp.floor.generic.color")
            if (key in fullOnlySettings || key.startsWith("waterOpacity.") || key.startsWith("esp.nameplate.") ||
                key.startsWith("esp.nameplates.") || key.startsWith("esp.teammate.") || floorAppearance || automatedInvite ||
                preserveInviteList && key.startsWith("warpList.")) {
                output.setProperty(key, saved.getProperty(key))
            }
        }
    }

    private fun readSavedSettings() = Properties().apply {
        if (Files.exists(configPath)) Files.newInputStream(configPath).use(::load)
    }

    private val flags = mapOf(::safariEntryCooldown to true, ::hudSnapping to true, ::doomspiralThrowTimer to true, ::doomspiralLottery to true, ::autoParty to false, ::autoPartyOnlineOnly to false, ::autoBiomeReports to true, ::uniqueHideCritterEsp to false, ::uniqueHideFloorDropEsp to false,
        ::fullClearHideCritterEsp to false, ::fullClearHideFloorDropEsp to false, ::allScannableCritterChecks to false,
        ::sparklingHideCritterEsp to false, ::sparklingHideFloorDropEsp to false, ::announceStartingItems to false, ::hotspotAnnouncer to false, ::uniqueHotspotAnnouncer to false, ::fullClearHotspotAnnouncer to false, ::sparklingHotspotAnnouncer to false, ::partyCommandsEnabled to true, ::useCloudApi to true, ::fullClearMode to false, ::sparklingMode to false, ::sparklingProfitableShardEsp to true, ::profitableShardEsp to true, ::timesaveOnly to true, ::warpAlertsEnabled to false,
        ::hideyhoAutoAccept to true, ::autoSelectSafariTicket to true, ::safariEntryAutoEnter to false, ::hideyhoInsideOutside to true, ::hideHauntedPaintings to true, ::shinyDetection to true,
        ::critterEspEnabled to true, ::floorDropEspEnabled to true, ::customCritterNames to true, ::hotspotHudEnabled to true, ::progressHud to true, ::progressTracks to true, ::splitsHud to true, ::splitsShowSession to true, ::progressProfit to true, ::progressHotspot to true, ::hotspotShowAllPlayers to true, ::countUniqueOnly to false, ::profitableChecklist to true, ::lakeClearAlert to true, ::missingPanel to true, ::biomeObjectives to true,
        ::objectiveCaptureReminder to true, ::objectiveShinyReminder to true,
        ::doomSoloPbChat to true, ::doomDuoPbChat to true, ::doomObservedSoloPbChat to true, ::doomObservedDuoPbChat to true,
        ::wumpaSoloPbChat to true, ::wumpaDuoPbChat to true, ::wumpaObservedSoloPbChat to true, ::wumpaObservedDuoPbChat to true,
        ::fullClearPbChat to true, ::uniquePbChat to true, ::partyUniquePbChat to true,
        ::autoReconnectHypixel to false, ::autoJoinSkyBlock to false,
        ::highlightBeeNests to true, ::removeDarkness to true, ::clearWater to true, ::sparklingAlert to true, ::sparklingCatchCount to true,
        ::sparklingPartyAnnouncer to true, ::catchCountPanel to false, ::autoClicker to true, ::candleHitbox to true, ::holdManagerOnKick to true, ::downtimeCommand to true, ::honeyHives to true, ::huntingBoxValue to true, ::ubiksCubeTimer to false, ::shardSaleGuidance to true, ::skillOverflow to true, ::skillOverflowChat to true, ::skillOverflowNumbers to false, ::estimatedItemValue to true, ::estimatedChestValue to true, ::phantomleafSolver to true, ::achievements to true, ::achievementSound to true, ::achievementsToParty to false, ::runCoinsInstaSell to false,
        ::hideCaptureChat to true, ::hideGroundCapsules to true, ::hideFlyingCapsules to true,
        ::experimentalCapsulePath to true, ::showCapsuleGuideThroughTerrain to true, ::gemzieGemAutoSelect to true,
        ::highlightSnooperWalls to true, ::showBeeNests to true, ::showMoundCount to true,
        ::showSnooperWalls to true, ::showMoundStats to false,
        ::allGemsAlert to true, ::allBirdFoodAlert to true, ::allCoinsAlert to true, ::allIncenseAlert to true, ::runCompleteAlert to true)

    fun validCapsuleDistance(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(.5f, 6f) ?: 2f
    fun validCapsulePathWidth(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(1f,5f) ?: 3f
    fun validSparklingSound(value: String?) = value?.trim()?.let(Identifier::tryParse)?.toString() ?: "minecraft:block.amethyst_block.chime"

    fun init(configDirectory: Path, installedVersion: String? = null, recoverPreviousVersion: () -> String? = { null }) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")
        val existing = Files.exists(configPath)
        val properties = if (existing) readSavedSettings() else FreshInstallDefaults.main()
        val savedVersions = InstalledVersionHistory.load(properties)
        installedVersionHistory = savedVersions.launch(installedVersion, !existing, recoverPreviousVersion)
        val versionChanged = installedVersionHistory != savedVersions
        if (versionChanged) installedVersionHistory.save(properties)
        // Introduce the redesigned guidance once, then respect every subsequent saved choice.
        val introduceShardGuidance = properties.getProperty("migration.shardSaleGuidanceV2") != "true"
        if (introduceShardGuidance) {
            properties.setProperty("shardSaleGuidance", "true")
            properties.setProperty("migration.shardSaleGuidanceV2", "true")
        }
        personalBestConfig = PersonalBestConfig(configPath)
        personalBestConfig.load(properties, personalBests)
        loadSettings(properties)
        if (introduceShardGuidance || versionChanged) {
            PersonalBestConfig.write(configPath, properties, "Sparkling Mutuals configuration")
        }
    }

    private fun loadSettings(properties: Properties) {
        progressRows = (listOf(null) + SafariBiome.entries).associateWith { biome ->
            ProgressRowDisplay.entries.firstOrNull { it.name == properties.getProperty(progressRowKey(biome)) }
                ?: ProgressRowDisplay.BAR_AND_COUNT
        }
        settingsTheme = properties.getProperty("settingsTheme")?.toIntOrNull()?.coerceIn(0,33) ?: 4
        splitsTimeFormat = SplitTimeFormat.entries.firstOrNull { it.name == properties.getProperty("splitsTimeFormat") }
            ?: SplitTimeFormat.MILLISECONDS
        specialCatchAlert = properties.getProperty("specialCatchAlert")?.toBooleanStrictOrNull() ?: true
        alwaysCelebrateSparkling = properties.getProperty("alwaysCelebrateSparkling")?.toBooleanStrictOrNull() ?: false
        specialCatchStyle = properties.getProperty("specialCatchStyle")?.toIntOrNull()?.coerceIn(0,40) ?: 0
        apiKey = properties.getProperty("apiKey", "")
        sparklingSound = validSparklingSound(properties.getProperty("sparklingSound"))
        runCompleteTitle = properties.getProperty("runCompleteTitle", "Run Complete").take(120).ifBlank { "Run Complete" }
        runCompleteSound = properties.getProperty("runCompleteSound")?.trim()?.let(Identifier::tryParse)?.toString() ?: "minecraft:block.bell.use"
        runCompleteVolume = properties.getProperty("runCompleteVolume")?.toIntOrNull()?.coerceIn(0, 100) ?: 100
        sparklingSoundVolume = properties.getProperty("sparklingSoundVolume")?.toIntOrNull()?.coerceIn(0, 100) ?: 100
        flags.filterKeys { it.name !in fullOnlySettings }.forEach { (property, default) -> property.set(properties.getProperty(property.name)?.toBooleanStrictOrNull() ?: default) }
        if (!properties.containsKey("hotspotAnnouncer")) hotspotAnnouncer = uniqueHotspotAnnouncer || fullClearHotspotAnnouncer || sparklingHotspotAnnouncer
        if (uniqueHideCritterEsp || uniqueHideFloorDropEsp) profitableShardEsp=false
        if (sparklingHideCritterEsp || sparklingHideFloorDropEsp) sparklingProfitableShardEsp=false
        profitsTicketMode = properties.getProperty("profitsTicketMode")?.toIntOrNull()?.takeIf { it in 0..3 } ?: 0
        showWhere = properties.getProperty("showWhere")?.toIntOrNull()?.takeIf { it in 0..2 } ?: 1
        capsuleHideDistance = validCapsuleDistance(properties.getProperty("capsuleHideDistance")?.toFloatOrNull())
        capsulePathWidth = validCapsulePathWidth(properties.getProperty("capsulePathWidth")?.toFloatOrNull())
        net.johnceo.sparklingmutuals.alerts.WarpList.load(properties, automation = false)
        doomspiralEmergenceTicks = properties.getProperty("doomspiralEmergenceTicks")?.toIntOrNull()?.coerceIn(1, 600) ?: 98
        doomspiralThrowLeadTicks = properties.getProperty("doomspiralThrowLeadTicks")?.toIntOrNull()?.coerceIn(0, 40) ?: 4
        SafariHud.load(properties)
        SafariFullClear.syncMode()
        SafariEspConfig.load(properties, includeFullPresentation = false)
        SafariChatConfig.load(properties)
        BiomeObjectiveConfig.load(properties)
        SafariFullClear.applyEspPreset()
        BannerConfig.load(properties)
        AppearanceConfig.load(properties)
    }

    internal fun resetSettings() {
        val previous = readSavedSettings()
        val defaults = FreshInstallDefaults.main().apply {
            installedVersionHistory.save(this)
            setProperty("apiKey", apiKey)
            setProperty("profitsTicketMode", profitsTicketMode.toString())
            setProperty("runCoinsInstaSell", runCoinsInstaSell.toString())
            setProperty("hotspotAnnouncer", hotspotAnnouncer.toString())
            setProperty("migration.shardSaleGuidanceV2", "true")
            preserveFullSettings(previous, this, preserveInviteList = true)
        }
        PersonalBestConfig.write(configPath, defaults, "Sparkling Mutuals configuration")
        try {
            ContestConfig.resetSettings()
        } catch (error: java.io.IOException) {
            runCatching { PersonalBestConfig.write(configPath, previous, "Sparkling Mutuals configuration") }
                .onFailure(error::addSuppressed)
            throw error
        }
        loadSettings(defaults)
    }

    internal fun acknowledgeReleaseHighlights(version: String) {
        val acknowledged = installedVersionHistory.acknowledge(version)
        if (acknowledged == installedVersionHistory) return
        installedVersionHistory = acknowledged
        // Keep the rest of the saved config untouched when the player finishes reading.
        runCatching {
            val properties = Properties()
            Files.newInputStream(configPath).use(properties::load)
            acknowledged.save(properties)
            PersonalBestConfig.write(configPath, properties, "Sparkling Mutuals configuration")
        }.onFailure { org.slf4j.LoggerFactory.getLogger("sparkling-mutuals/updates").warn("Could not save release highlights state", it) }
    }

    fun save() {
        personalBestConfig.save(personalBests)
        saveSettings()
    }

    internal fun saveSettings() {
        val previous = readSavedSettings()
        Properties().apply {
            preserveFullSettings(previous, this)
            installedVersionHistory.save(this)
            setProperty("apiKey", apiKey)
            setProperty("runCompleteTitle", runCompleteTitle)
            setProperty("runCompleteSound", runCompleteSound)
            setProperty("runCompleteVolume", runCompleteVolume.coerceIn(0, 100).toString())
            setProperty("sparklingSound", validSparklingSound(sparklingSound))
            setProperty("sparklingSoundVolume", sparklingSoundVolume.coerceIn(0, 100).toString())
            flags.filterKeys { it.name !in fullOnlySettings }.forEach { (property, _) -> setProperty(property.name, property.get().toString()) }
            setProperty("migration.shardSaleGuidanceV2", "true")

            setProperty("settingsTheme", settingsTheme.toString())
            setProperty("splitsTimeFormat", splitsTimeFormat.name)
            progressRows.forEach { (biome, display) -> setProperty(progressRowKey(biome), display.name) }
            setProperty("specialCatchAlert", specialCatchAlert.toString())
            setProperty("alwaysCelebrateSparkling", alwaysCelebrateSparkling.toString())
            setProperty("specialCatchStyle", specialCatchStyle.toString())


            setProperty("doomspiralEmergenceTicks", doomspiralEmergenceTicks.coerceIn(1, 600).toString())
            setProperty("doomspiralThrowLeadTicks", doomspiralThrowLeadTicks.coerceIn(0, 40).toString())

            setProperty("profitsTicketMode", profitsTicketMode.coerceIn(0, 3).toString())
            setProperty("showWhere", showWhere.coerceIn(0, 2).toString())
            setProperty("capsuleHideDistance", validCapsuleDistance(capsuleHideDistance).toString())
            setProperty("capsulePathWidth", validCapsulePathWidth(capsulePathWidth).toString())
            net.johnceo.sparklingmutuals.alerts.WarpList.save(this, automation = false)

            SafariHud.save(this)
            SafariEspConfig.save(this, includeFullPresentation = false)
            SafariChatConfig.save(this)
            BiomeObjectiveConfig.save(this)
            AppearanceConfig.save(this)
            BannerConfig.save(this)
            PersonalBestConfig.write(configPath, this, "Sparkling Mutuals configuration")
        }
    }
}
