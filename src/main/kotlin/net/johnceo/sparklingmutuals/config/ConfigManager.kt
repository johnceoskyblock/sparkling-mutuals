package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import net.johnceo.sparklingmutuals.safari.completion.SafariPersonalBests
import net.minecraft.resources.Identifier

object ConfigManager {
    private lateinit var configPath: Path
    private lateinit var personalBestConfig: PersonalBestConfig
    @Volatile var apiKey = ""
    var highlightTeammates = true
    var showTeammateNames = true
    var partyCommandsEnabled = true
    var announceStartingItems = false
    var autoBiomeReports = true
    var uniqueHideCritterEsp = false
    var uniqueHideFloorDropEsp = false
    var fullClearHideCritterEsp = false
    var fullClearHideFloorDropEsp = false
    var sparklingHideCritterEsp = false
    var sparklingHideFloorDropEsp = false
    fun hideCritterEsp(mode: SafariMode = SafariFullClear.mode) = when(mode) {
        SafariMode.UNIQUE -> uniqueHideCritterEsp
        SafariMode.FULL_CLEAR -> fullClearHideCritterEsp
        SafariMode.SPARKLING -> sparklingHideCritterEsp
    }
    fun hideFloorDropEsp(mode: SafariMode = SafariFullClear.mode) = when(mode) {
        SafariMode.UNIQUE -> uniqueHideFloorDropEsp
        SafariMode.FULL_CLEAR -> fullClearHideFloorDropEsp
        SafariMode.SPARKLING -> sparklingHideFloorDropEsp
    }
    var useCloudApi = true
    var fullClearMode = false
    var sparklingMode = false
    var sparklingProfitableShardEsp = true
    var profitableShardEsp = true
    var uniqueHotspotAnnouncer = false
    var fullClearHotspotAnnouncer = false
    var sparklingHotspotAnnouncer = false
    var timesaveOnly = true
    var warpAlertsEnabled = false
    var warpDelaySeconds = 23
    var autoParty = false
    var doomspiralThrowTimer = true
    var doomspiralEmergenceTicks = 98
    var doomspiralThrowLeadTicks = 4
    var hideyhoAutoAccept = true
    var autoSelectSafariTicket = true
    var safariTicketPreference = 0
    var profitsTicketMode = 0
    var hideyhoInsideOutside = true
    var hideHauntedPaintings = true
    var shinyDetection = true
    var progressHud = true
    var countUniqueOnly = false
    var missingPanel = true
    var highlightBeeNests = true
    var highlightSnooperWalls = true
    var showBeeNests = true
    var showMoundCount = true
    var showSnooperWalls = true
    var showMoundStats = false
    var removeDarkness = true
    var clearWater = true
    var waterOpacity = 60
        set(value) { field = value.coerceIn(0, 100) }
    var sparklingAlert = true
    var sparklingSound = "minecraft:block.amethyst_block.chime"
    var sparklingSoundVolume = 100
    var allGemsAlert = true
    var allBirdFoodAlert = true
    var allIncenseAlert = true
    var runCompleteAlert = true
    var runCompleteTitle = "Run Complete"
    var runCompleteSound = "minecraft:block.bell.use"
    var runCompleteVolume = 100
    var sparklingPartyAnnouncer = true
    var catchCountPanel = false
    var autoClicker = true
    var gemzieGemAutoSelect = true
    var candleHitbox = true
    var holdManagerOnKick = true
    var downtimeCommand = true
    var honeyHives = true
    var phantomleafSolver = true
    var runHistory = true
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
    private val flags = mapOf(::doomspiralThrowTimer to true, ::autoParty to false, ::autoBiomeReports to true, ::uniqueHideCritterEsp to false, ::uniqueHideFloorDropEsp to false,
        ::fullClearHideCritterEsp to false, ::fullClearHideFloorDropEsp to false,
        ::sparklingHideCritterEsp to false, ::sparklingHideFloorDropEsp to false, ::announceStartingItems to false, ::uniqueHotspotAnnouncer to false, ::fullClearHotspotAnnouncer to false, ::sparklingHotspotAnnouncer to false, ::highlightTeammates to true, ::showTeammateNames to true, ::partyCommandsEnabled to true, ::useCloudApi to true, ::fullClearMode to false, ::sparklingMode to false, ::sparklingProfitableShardEsp to true, ::profitableShardEsp to true, ::timesaveOnly to true, ::warpAlertsEnabled to false,
        ::hideyhoAutoAccept to true, ::autoSelectSafariTicket to true, ::hideyhoInsideOutside to true, ::hideHauntedPaintings to true, ::shinyDetection to true,
        ::progressHud to true, ::countUniqueOnly to false, ::missingPanel to true,
        ::highlightBeeNests to true, ::removeDarkness to true, ::clearWater to true, ::sparklingAlert to true,
        ::sparklingPartyAnnouncer to true, ::catchCountPanel to false, ::autoClicker to true, ::candleHitbox to true, ::holdManagerOnKick to true, ::downtimeCommand to true, ::honeyHives to true, ::phantomleafSolver to true, ::runHistory to true, ::achievements to true, ::achievementSound to true, ::achievementsToParty to false, ::runCoinsInstaSell to false,
        ::hideCaptureChat to true, ::hideGroundCapsules to true, ::hideFlyingCapsules to true,
        ::experimentalCapsulePath to true, ::showCapsuleGuideThroughTerrain to true, ::gemzieGemAutoSelect to true,
        ::highlightSnooperWalls to true, ::showBeeNests to true, ::showMoundCount to true,
        ::showSnooperWalls to true, ::showMoundStats to false,
        ::allGemsAlert to true, ::allBirdFoodAlert to true, ::allIncenseAlert to true, ::runCompleteAlert to true)

    fun validCapsuleDistance(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(.5f, 6f) ?: 2f
    fun validCapsulePathWidth(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(1f,5f) ?: 3f
    fun validSparklingSound(value: String?) = value?.trim()?.let(Identifier::tryParse)?.toString() ?: "minecraft:block.amethyst_block.chime"

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")
        val properties = if (Files.exists(configPath)) Properties() else FreshInstallDefaults.main()
        if (Files.exists(configPath)) Files.newInputStream(configPath).use(properties::load)
        apiKey = properties.getProperty("apiKey", "")
        sparklingSound = validSparklingSound(properties.getProperty("sparklingSound"))
        runCompleteTitle = properties.getProperty("runCompleteTitle", "Run Complete").take(120).ifBlank { "Run Complete" }
        runCompleteSound = properties.getProperty("runCompleteSound")?.trim()?.let(Identifier::tryParse)?.toString() ?: "minecraft:block.bell.use"
        runCompleteVolume = properties.getProperty("runCompleteVolume")?.toIntOrNull()?.coerceIn(0, 100) ?: 100
        sparklingSoundVolume = properties.getProperty("sparklingSoundVolume")?.toIntOrNull()?.coerceIn(0, 100) ?: 100
        flags.forEach { (property, default) -> property.set(properties.getProperty(property.name)?.toBooleanStrictOrNull() ?: default) }
        if (uniqueHideCritterEsp || uniqueHideFloorDropEsp) profitableShardEsp=false
        if (sparklingHideCritterEsp || sparklingHideFloorDropEsp) sparklingProfitableShardEsp=false
        warpDelaySeconds = properties.getProperty("warpDelaySeconds", "23").toIntOrNull()?.coerceIn(1, 86400) ?: 23
        safariTicketPreference = properties.getProperty("safariTicketPreference")?.toIntOrNull()?.takeIf { it in 0..1 } ?: 0
        profitsTicketMode = properties.getProperty("profitsTicketMode")?.toIntOrNull()?.takeIf { it in 0..3 } ?: 0
        showWhere = properties.getProperty("showWhere")?.toIntOrNull()?.takeIf { it in 0..2 } ?: 1
        waterOpacity = properties.getProperty("waterOpacity")?.toIntOrNull() ?: 60
        capsuleHideDistance = validCapsuleDistance(properties.getProperty("capsuleHideDistance")?.toFloatOrNull())
        capsulePathWidth = validCapsulePathWidth(properties.getProperty("capsulePathWidth")?.toFloatOrNull())
        net.johnceo.sparklingmutuals.alerts.WarpList.load(properties)
        doomspiralEmergenceTicks = properties.getProperty("doomspiralEmergenceTicks")?.toIntOrNull()?.coerceIn(1, 600) ?: 98
        doomspiralThrowLeadTicks = properties.getProperty("doomspiralThrowLeadTicks")?.toIntOrNull()?.coerceIn(0, 40) ?: 4
        SafariHud.load(properties)
        personalBestConfig = PersonalBestConfig(configPath)
        personalBestConfig.load(properties, personalBests)
        SafariFullClear.syncMode()
        SafariEspConfig.load(properties)
        SafariFullClear.applyEspPreset()
        AppearanceConfig.load(properties)
    }

    fun save() {
        Files.createDirectories(configPath.parent)
        personalBestConfig.save(personalBests)
        Properties().apply {
            setProperty("apiKey", apiKey)
            setProperty("runCompleteTitle", runCompleteTitle)
            setProperty("runCompleteSound", runCompleteSound)
            setProperty("runCompleteVolume", runCompleteVolume.coerceIn(0, 100).toString())
            setProperty("sparklingSound", validSparklingSound(sparklingSound))
            setProperty("sparklingSoundVolume", sparklingSoundVolume.coerceIn(0, 100).toString())
            flags.forEach { (property, _) -> setProperty(property.name, property.get().toString()) }
            setProperty("warpDelaySeconds", warpDelaySeconds.toString())
            setProperty("doomspiralEmergenceTicks", doomspiralEmergenceTicks.coerceIn(1, 600).toString())
            setProperty("doomspiralThrowLeadTicks", doomspiralThrowLeadTicks.coerceIn(0, 40).toString())
            setProperty("safariTicketPreference", safariTicketPreference.coerceIn(0, 1).toString())
            setProperty("profitsTicketMode", profitsTicketMode.coerceIn(0, 3).toString())
            setProperty("showWhere", showWhere.coerceIn(0, 2).toString())
            setProperty("waterOpacity", waterOpacity.toString())
            setProperty("capsuleHideDistance", validCapsuleDistance(capsuleHideDistance).toString())
            setProperty("capsulePathWidth", validCapsulePathWidth(capsulePathWidth).toString())
            net.johnceo.sparklingmutuals.alerts.WarpList.save(this)
            SafariHud.save(this)
            SafariEspConfig.save(this)
            AppearanceConfig.save(this)
            PersonalBestConfig.write(configPath, this, "Sparkling Mutuals configuration")
        }
    }
}
