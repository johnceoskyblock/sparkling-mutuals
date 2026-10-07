package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.SafariPersonalBests
import net.johnceo.sparklingmutuals.safari.SafariFullClear

object ConfigManager {
    private lateinit var configPath: Path
    @Volatile var apiKey = ""
    var partyCommandsEnabled = true
    var fullClearMode = false
    var timesaveOnly = true
    var warpAlertsEnabled = false
    var warpDelaySeconds = 25
    var hideyhoQuestClicks = true
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
    var sparklingAlert = true
    var allGemsAlert = true
    var allBirdFoodAlert = true
    var allIncenseAlert = true
    var sparklingPartyAnnouncer = true
    var catchCountPanel = false
    var autoClicker = true
    var hideCaptureChat = false
    var hideGroundCapsules = true
    var hideFlyingCapsules = true
    var capsuleHideDistance = 2f
    var showWhere = 1
    val personalBests = SafariPersonalBests()
    private val flags = mapOf(::partyCommandsEnabled to true, ::fullClearMode to false, ::timesaveOnly to true, ::warpAlertsEnabled to false,
        ::hideyhoQuestClicks to true, ::hideHauntedPaintings to true, ::shinyDetection to true,
        ::progressHud to true, ::countUniqueOnly to false, ::missingPanel to true,
        ::highlightBeeNests to true, ::removeDarkness to true, ::sparklingAlert to true,
        ::sparklingPartyAnnouncer to true, ::catchCountPanel to false, ::autoClicker to true,
        ::hideCaptureChat to false, ::hideGroundCapsules to true, ::hideFlyingCapsules to true,
        ::highlightSnooperWalls to true, ::showBeeNests to true, ::showMoundCount to true,
        ::showSnooperWalls to true, ::showMoundStats to false,
        ::allGemsAlert to true, ::allBirdFoodAlert to true, ::allIncenseAlert to true)

    fun validCapsuleDistance(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(.5f, 6f) ?: 2f

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")
        val properties = Properties()
        if (Files.exists(configPath)) Files.newInputStream(configPath).use(properties::load)
        apiKey = properties.getProperty("apiKey", "")
        flags.forEach { (property, default) -> property.set(properties.getProperty(property.name)?.toBooleanStrictOrNull() ?: default) }
        warpDelaySeconds = properties.getProperty("warpDelaySeconds", "25").toIntOrNull()?.coerceIn(1, 86400) ?: 25
        showWhere = properties.getProperty("showWhere")?.toIntOrNull()?.takeIf { it in 0..2 } ?: 1
        capsuleHideDistance = validCapsuleDistance(properties.getProperty("capsuleHideDistance")?.toFloatOrNull())
        SafariHud.load(properties)
        personalBests.load(properties)
        SafariFullClear.syncMode()
        SafariEspConfig.load(properties)
        AppearanceConfig.load(properties)
    }

    fun save() {
        Files.createDirectories(configPath.parent)
        Properties().apply {
            setProperty("apiKey", apiKey)
            flags.forEach { (property, _) -> setProperty(property.name, property.get().toString()) }
            setProperty("warpDelaySeconds", warpDelaySeconds.toString())
            setProperty("showWhere", showWhere.coerceIn(0, 2).toString())
            setProperty("capsuleHideDistance", validCapsuleDistance(capsuleHideDistance).toString())
            SafariHud.save(this)
            personalBests.save(this)
            SafariEspConfig.save(this)
            AppearanceConfig.save(this)
            Files.newOutputStream(configPath).use { store(it, "Sparkling Mutuals configuration") }
        }
    }
}
