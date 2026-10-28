package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object ContestConfig {

    private lateinit var configPath: Path

    var hudX = 10
    var hudY = 10
    var hudScale = 1.0f
    val layout = net.johnceo.sparklingmutuals.hud.HudLayout(0f, 0f)
    private var legacyLayout = true
    fun layoutFor(width: Int, height: Int) = layout.also {
        if (legacyLayout) {
            it.x = hudX.toFloat() / width.coerceAtLeast(1)
            it.y = hudY.toFloat() / height.coerceAtLeast(1)
            it.scale = hudScale
        }
    }
    fun editLayout(width: Int, height: Int) { layoutFor(width, height); legacyLayout = false }
    fun resetLayout() { layout.load(FreshInstallDefaults.contest(), "contest"); legacyLayout = false }

    var trackContest = true
    var autoContestDowntime = true
    var ticketMode = 0 // Basic = 0, First-Class = 1; independent of entry ticket preference.
    val firstClassTicket get() = ticketMode == 1
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
            if (!Files.exists(configPath)) putAll(FreshInstallDefaults.contest())
            if (Files.exists(configPath)) Files.newInputStream(configPath).use(::load)

            hudX = getProperty("hudX", "10").toIntOrNull() ?: 10
            hudY = getProperty("hudY", "10").toIntOrNull() ?: 10
            hudScale = getProperty("hudScale", "1.0")
                .toFloatOrNull()
                ?.takeIf(Float::isFinite)
                ?.coerceIn(0.15f, 2.0f)
                ?: 1.0f
            legacyLayout = !containsKey("hud.contest.x") || !containsKey("hud.contest.y")
            layout.load(this, "contest")

            trackContest = getProperty("trackContest", "true").toBoolean()
            autoContestDowntime = getProperty("autoContestDowntime", "true").toBooleanStrictOrNull() ?: true
            ticketMode = getProperty("ticketMode", "0").toIntOrNull()?.takeIf { it in 0..1 } ?: 0
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
            if (!legacyLayout) layout.save(this, "contest")
            setProperty("trackContest", trackContest.toString())
            setProperty("autoContestDowntime", autoContestDowntime.toString())
            setProperty("ticketMode", ticketMode.toString())
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
