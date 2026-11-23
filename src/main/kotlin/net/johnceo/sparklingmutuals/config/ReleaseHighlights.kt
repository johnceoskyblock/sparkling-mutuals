package net.johnceo.sparklingmutuals.config

import com.google.gson.JsonParser
import java.io.Reader
import net.johnceo.sparklingmutuals.config.InstalledVersionHistory.Companion.compare

internal enum class ReleaseTrack(val id: String) {
    STANDARD("standard"), FULL("full"), FULL_NO_AUTO_UPDATE("full-no-auto-update");

    companion object {
        val current: ReleaseTrack
            get() = when {
                SafariSettings.standardEdition -> STANDARD
                ModUpdates.canCheck -> FULL
                else -> FULL_NO_AUTO_UPDATE
            }
    }
}

/** Curated, offline highlights. A build can only display entries up to its installed release. */
internal object ReleaseHighlights {
    enum class Audience {
        SHARED, STANDARD, FULL, UPDATER;

        fun includes(track: ReleaseTrack) = when (this) {
            SHARED -> true
            STANDARD -> track == ReleaseTrack.STANDARD
            FULL -> track != ReleaseTrack.STANDARD
            UPDATER -> track == ReleaseTrack.FULL
        }
    }

    data class Feature(val title: String, val details: String, val audience: Audience)
    data class Release(val version: String, val features: List<Feature>)
    data class Update(val installed: String, val through: String?, val releases: List<Release>)

    val catalog: List<Release> by lazy {
        checkNotNull(javaClass.getResourceAsStream("/releases/flagship-highlights.json")).bufferedReader().use(::parse)
    }

    fun parse(reader: Reader): List<Release> = JsonParser.parseReader(reader).asJsonObject.entrySet().map { (version, value) ->
        require(InstalledVersionHistory.canonical(version) == version) { "Invalid highlight version: $version" }
        val features = value.asJsonArray.map { row ->
            val feature = row.asJsonObject
            val audience = Audience.entries.firstOrNull { it.name.equals(feature.get("audience")?.asString, ignoreCase = true) }
            requireNotNull(audience) { "Missing or invalid highlight audience in $version" }
            Feature(feature.get("title").asString.trim(), feature.get("details").asString.trim(), audience).also {
                require(it.title.isNotEmpty() && it.details.isNotEmpty()) { "Incomplete feature in $version" }
            }
        }
        require(features.isNotEmpty()) { "No highlights defined for $version" }
        Release(version, features)
    }.sortedWith { a, b -> compare(b.version, a.version) }

    fun preceding(version: String, releases: List<Release> = catalog): String? =
        releases.firstOrNull { compare(it.version, version) < 0 }?.version

    fun forCurrentInstall(history: InstalledVersionHistory, fallbackVersion: String = "dev",
        releases: List<Release> = catalog, track: ReleaseTrack = ReleaseTrack.current): Update {
        val installed = history.installed ?: InstalledVersionHistory.canonical(fallbackVersion)
            ?: return Update(fallbackVersion, null, emptyList())
        val previous = history.previous?.takeIf { compare(it, installed) < 0 }
        val highlights = releases.filter { release ->
            if (previous == null) release.version == installed
            else compare(release.version, previous) > 0 && compare(release.version, installed) <= 0
        }.mapNotNull { release ->
            val features = release.features.filter { it.audience.includes(track) }
            features.takeIf { it.isNotEmpty() }?.let { release.copy(features = it) }
        }
        return Update(installed, previous, highlights)
    }

    fun pending(history: InstalledVersionHistory, releases: List<Release> = catalog, track: ReleaseTrack = ReleaseTrack.current): Update? {
        val installed = history.installed ?: return null
        if (history.highlightsSeenThrough?.let { compare(it, installed) >= 0 } == true ||
            history.previous?.let { compare(it, installed) >= 0 } == true) return null
        return forCurrentInstall(history, releases = releases, track = track).takeIf { it.releases.isNotEmpty() }
    }
}
