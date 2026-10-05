package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object ContestConfig {

    private lateinit var configPath: Path

    var hudX = 10
    var hudY = 10
    var hudScale = 1.0f

    var trackContest = true
    var contestWarnEnabled = true
    var contestWarnMinutes = "5, 3, 1"
    var contestWarnTitle = true
    var contestSound = "minecraft:block.bell.use"
    var contestSoundVolume = 100

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals-contest.properties")

        val exists = Files.exists(configPath)
        load()
        if (!exists) save()
    }

    private fun load() {
        Properties().apply {
            if (Files.exists(configPath)) Files.newInputStream(configPath).use(::load)

            hudX = getProperty("hudX", "10").toIntOrNull() ?: 10
            hudY = getProperty("hudY", "10").toIntOrNull() ?: 10
            hudScale = getProperty("hudScale", "1.0")
                .toFloatOrNull()
                ?.coerceIn(0.5f, 2.0f)
                ?: 1.0f

            trackContest = getProperty("trackContest", "true").toBoolean()
            contestWarnEnabled = getProperty("contestWarnEnabled", "true").toBoolean()
            contestWarnMinutes = getProperty("contestWarnMinutes", "5, 3, 1")
            contestWarnTitle = getProperty("contestWarnTitle", "true").toBoolean()
            contestSound = getProperty("contestSound", "minecraft:block.bell.use")
            contestSoundVolume = getProperty("contestSoundVolume", "100").toIntOrNull() ?: 100
        }
    }

    fun warningMinutes(): List<Int> = if (!contestWarnEnabled) emptyList() else
        contestWarnMinutes.split(Regex("[,\\s]+"))
            .mapNotNull { it.toIntOrNull() }
            .filter { it in setOf(5, 3, 1) }
            .distinct().sortedDescending()

    fun setWarningMinutes(minutes: Collection<Int>) {
        val selected = minutes.filter { it in setOf(5, 3, 1) }.distinct().sortedDescending()
        contestWarnMinutes = selected.joinToString(", ")
        contestWarnEnabled = selected.isNotEmpty()
        save()
    }

    fun save() {
        Files.createDirectories(configPath.parent)

        Properties().apply {
            setProperty("hudX", hudX.toString())
            setProperty("hudY", hudY.toString())
            setProperty("hudScale", hudScale.toString())
            setProperty("trackContest", trackContest.toString())
            setProperty("contestWarnEnabled", contestWarnEnabled.toString())
            setProperty("contestWarnMinutes", contestWarnMinutes)
            setProperty("contestWarnTitle", contestWarnTitle.toString())
            setProperty("contestSound", contestSound)
            setProperty("contestSoundVolume", contestSoundVolume.toString())

            Files.newOutputStream(configPath).use {
                store(it, "Sparkling Mutuals contest configuration")
            }
        }
    }
}
