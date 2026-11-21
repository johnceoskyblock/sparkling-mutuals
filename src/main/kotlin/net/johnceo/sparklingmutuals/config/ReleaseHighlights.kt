package net.johnceo.sparklingmutuals.config

import com.google.gson.JsonParser
import java.io.Reader
import net.johnceo.sparklingmutuals.config.InstalledVersionHistory.Companion.compare

/** Curated, offline highlights. A build can only display entries up to its installed release. */
internal object ReleaseHighlights {
    data class Feature(val title: String, val details: String)
    data class Release(val version: String, val features: List<Feature>)
    data class Update(val installed: String, val through: String?, val releases: List<Release>)

    val catalog: List<Release> by lazy {
        checkNotNull(javaClass.getResourceAsStream("/releases/flagship-highlights.json")).bufferedReader().use(::parse)
    }

    fun parse(reader: Reader): List<Release> = JsonParser.parseReader(reader).asJsonObject.entrySet().map { (version, value) ->
        require(InstalledVersionHistory.canonical(version) == version) { "Invalid highlight version: $version" }
        val features = value.asJsonArray.map { row ->
            val feature = row.asJsonObject
            Feature(feature.get("title").asString.trim(), feature.get("details").asString.trim()).also {
                require(it.title.isNotEmpty() && it.details.isNotEmpty()) { "Incomplete feature in $version" }
            }
        }
        require(features.isNotEmpty()) { "No highlights defined for $version" }
        Release(version, features)
    }.sortedWith { a, b -> compare(b.version, a.version) }

    fun preceding(version: String, releases: List<Release> = catalog): String? =
        releases.firstOrNull { compare(it.version, version) < 0 }?.version

    fun pending(history: InstalledVersionHistory, releases: List<Release> = catalog): Update? {
        val installed = history.installed ?: return null
        val through = history.highlightsSeenThrough
        val unread = releases.filter { release ->
            compare(release.version, installed) <= 0 &&
                if (through != null) compare(release.version, through) > 0 else release.version == installed
        }
        return unread.takeIf { it.isNotEmpty() }?.let { Update(installed, through?.takeIf { history.previous != null }, it) }
    }
}
