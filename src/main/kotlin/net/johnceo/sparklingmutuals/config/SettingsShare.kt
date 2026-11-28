// Settings-transfer workflow adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT.
package net.johnceo.sparklingmutuals.config

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Properties
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal object SettingsShare {
    private const val PREFIX = "SMC1:"
    private const val MAX_CODE_LENGTH = 128_000
    private const val MAX_SETTINGS_BYTES = 256_000
    private val privateKeys = setOf("apiKey", "warpDuo", "leechWhitelist", "warpAlertsEnabled", "leechMode",
        "hotspotAnnouncer", "sparklingMode", "fullClearMode", "timesaveOnly", "autoUpdate")
    private val privatePrefixes = listOf("history.", "updates.", "migration.", "warpList.", "ticketAnnounced.")

    data class Snapshot(val main: Properties, val contest: Properties) {
        val size get() = main.size + contest.size
    }

    private fun portable(key: String) = key !in privateKeys && privatePrefixes.none(key::startsWith)
    private fun snapshot() = Snapshot(ConfigManager.settingsProperties(), ContestConfig.settingsProperties())

    fun exportCode() = encode(snapshot())

    internal fun encode(source: Snapshot): String {
        val payload = Properties()
        for ((scope, properties) in listOf("main." to source.main, "contest." to source.contest)) {
            for (key in properties.stringPropertyNames().filter(::portable)) {
                payload.setProperty(scope + key, properties.getProperty(key))
            }
        }
        val raw = ByteArrayOutputStream().apply { payload.store(this, null) }.toByteArray()
        require(raw.size <= MAX_SETTINGS_BYTES)
        val compressed = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(raw) } }
        return (PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray()))
            .also { require(it.length <= MAX_CODE_LENGTH) }
    }

    fun preview(code: String): Snapshot = decode(code, snapshot())

    internal fun decode(code: String, supported: Snapshot): Snapshot {
        val input = code.trim()
        require(input.startsWith(PREFIX) && input.length <= MAX_CODE_LENGTH)
        val zipped = Base64.getUrlDecoder().decode(input.removePrefix(PREFIX))
        val raw = GZIPInputStream(ByteArrayInputStream(zipped)).use { it.readNBytes(MAX_SETTINGS_BYTES + 1) }
        require(raw.size <= MAX_SETTINGS_BYTES)
        val payload = Properties().apply { ByteArrayInputStream(raw).use(::load) }
        fun select(prefix: String, allowed: Properties) = Properties().apply {
            for (key in allowed.stringPropertyNames().filter(::portable)) {
                payload.getProperty(prefix + key)?.let { setProperty(key, it) }
            }
        }
        return Snapshot(select("main.", supported.main), select("contest.", supported.contest)).also { require(it.size > 0) }
    }

    fun apply(settings: Snapshot) {
        val previous = Snapshot(ConfigManager.settingsProperties(ConfigManager.readSavedSettings()), ContestConfig.readSavedSettings())
        val main = Properties().apply { putAll(previous.main); putAll(settings.main) }
        val contest = Properties().apply { putAll(previous.contest); putAll(settings.contest) }
        ConfigManager.backUpSharedSettings(previous.main)
        ContestConfig.backUpSharedSettings(previous.contest)
        try {
            ConfigManager.applySharedSettings(main)
            ContestConfig.applySharedSettings(contest)
        } catch (failure: Exception) {
            runCatching { ConfigManager.applySharedSettings(previous.main) }.onFailure(failure::addSuppressed)
            runCatching { ContestConfig.applySharedSettings(previous.contest) }.onFailure(failure::addSuppressed)
            throw failure
        }
    }
}
