package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object ConfigManager {
    private lateinit var configPath: Path
    @Volatile var apiKey = ""
    var partyCommandsEnabled = true
    var warpAlertsEnabled = false
    var warpDelaySeconds = 25
    var hideyhoQuestClicks = false
    var hideHauntedPaintings = true
    var shinyDetection = true

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")
        val properties = Properties()
        if (Files.exists(configPath)) Files.newInputStream(configPath).use(properties::load)
        apiKey = properties.getProperty("apiKey", "")
        partyCommandsEnabled = properties.getProperty("partyCommandsEnabled", "true").toBoolean()
        warpAlertsEnabled = properties.getProperty("warpAlertsEnabled", "false").toBoolean()
        warpDelaySeconds = properties.getProperty("warpDelaySeconds", "25").toIntOrNull()?.coerceIn(1, 86400) ?: 25
        hideyhoQuestClicks = properties.getProperty("hideyhoQuestClicks", "false").toBoolean()
        hideHauntedPaintings = properties.getProperty("hideHauntedPaintings", "true").toBoolean()
        shinyDetection = properties.getProperty("shinyDetection", "true").toBoolean()
    }

    fun save() {
        Files.createDirectories(configPath.parent)
        Properties().apply {
            setProperty("apiKey", apiKey)
            setProperty("partyCommandsEnabled", partyCommandsEnabled.toString())
            setProperty("warpAlertsEnabled", warpAlertsEnabled.toString())
            setProperty("warpDelaySeconds", warpDelaySeconds.toString())
            setProperty("hideyhoQuestClicks", hideyhoQuestClicks.toString())
            setProperty("hideHauntedPaintings", hideHauntedPaintings.toString())
            setProperty("shinyDetection", shinyDetection.toString())
            Files.newOutputStream(configPath).use { store(it, "Sparkling Mutuals configuration") }
        }
    }
}
