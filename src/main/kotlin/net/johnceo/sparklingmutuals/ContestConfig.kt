package net.johnceo.sparklingmutuals

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object ContestConfig {

    private lateinit var configPath: Path

    var hudX: Int = 10
    var hudY: Int = 10
    var hudScale: Float = 1.0f

    var trackContest: Boolean = true
    var contestWarnEnabled: Boolean = true
    var contestWarnMinutes: String = "5, 3, 1"
    var contestWarnTitle: Boolean = true
    var contestSound: String = "minecraft:block.bell.use"
    var contestSoundVolume: Int = 100

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals-contest.properties")

        if (Files.exists(configPath)) {
            load()
        } else {
            save()
        }
    }

    private fun load() {
        val properties = Properties()

        Files.newInputStream(configPath).use {
            properties.load(it)
        }

        hudX = properties.getProperty("hudX", "10").toIntOrNull() ?: 10
        hudY = properties.getProperty("hudY", "10").toIntOrNull() ?: 10
        hudScale =
            properties.getProperty("hudScale", "1.0")
                .toFloatOrNull()
                ?.coerceIn(0.5f, 2.0f)
                ?: 1.0f

        trackContest =
            properties.getProperty("trackContest", "true").toBoolean()

        contestWarnEnabled =
            properties.getProperty("contestWarnEnabled", "true").toBoolean()

        contestWarnMinutes =
            properties.getProperty("contestWarnMinutes", "5, 3, 1")

        contestWarnTitle =
            properties.getProperty("contestWarnTitle", "true").toBoolean()

        contestSound =
            properties.getProperty(
                "contestSound",
                "minecraft:block.bell.use"
            )

        contestSoundVolume =
            properties.getProperty("contestSoundVolume", "100")
                .toIntOrNull() ?: 100
    }

    fun save() {
        val properties = Properties()

        properties.setProperty("hudX", hudX.toString())
        properties.setProperty("hudY", hudY.toString())
        properties.setProperty("hudScale", hudScale.toString())

        properties.setProperty("trackContest", trackContest.toString())
        properties.setProperty(
            "contestWarnEnabled",
            contestWarnEnabled.toString()
        )
        properties.setProperty(
            "contestWarnMinutes",
            contestWarnMinutes
        )
        properties.setProperty(
            "contestWarnTitle",
            contestWarnTitle.toString()
        )
        properties.setProperty(
            "contestSound",
            contestSound
        )
        properties.setProperty(
            "contestSoundVolume",
            contestSoundVolume.toString()
        )

        Files.createDirectories(configPath.parent)

        Files.newOutputStream(configPath).use {
            properties.store(
                it,
                "Sparkling Mutuals contest configuration"
            )
        }
    }
}