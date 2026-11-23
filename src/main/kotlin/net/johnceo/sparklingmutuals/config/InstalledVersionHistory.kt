package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.GZIPInputStream

internal data class InstalledVersionHistory(
    val installed: String? = null,
    val previous: String? = null,
    val highlightsSeenThrough: String? = null,
) {
    fun launch(version: String?, newConfig: Boolean, recoverPrevious: () -> String?): InstalledVersionHistory {
        val next = canonical(version) ?: return this
        if (next == installed) return this
        if (installed != null) return copy(installed = next, previous = installed)
        val recovered = canonical(recoverPrevious())
        if (newConfig && recovered == null) return InstalledVersionHistory(next, highlightsSeenThrough = next)
        return InstalledVersionHistory(next, recovered?.takeUnless { it == next },
            recovered ?: ReleaseHighlights.preceding(next))
    }

    fun acknowledge(version: String): InstalledVersionHistory {
        if (version != installed || highlightsSeenThrough?.let { compare(version, it) <= 0 } == true) return this
        return copy(highlightsSeenThrough = version)
    }

    fun save(properties: Properties, track: ReleaseTrack = ReleaseTrack.current) {
        val prefix = "updates.${track.id}."
        for ((suffix, version) in listOf(INSTALLED to installed, PREVIOUS to previous, SEEN to highlightsSeenThrough)) {
            val key = prefix + suffix
            if (version != null) properties.setProperty(key, version) else properties.remove(key)
        }
    }

    companion object {
        private const val INSTALLED = "installedVersion"
        private const val PREVIOUS = "previousVersion"
        private const val SEEN = "highlightsSeenThrough"

        fun hasTrack(properties: Properties, track: ReleaseTrack = ReleaseTrack.current): Boolean =
            properties.containsKey("updates.${track.id}.$INSTALLED")

        fun load(properties: Properties, track: ReleaseTrack = ReleaseTrack.current): InstalledVersionHistory {
            val scoped = hasTrack(properties, track)
            val prefix = if (scoped) "updates.${track.id}." else "updates."
            return InstalledVersionHistory(
                canonical(properties.getProperty(prefix + INSTALLED)),
                canonical(properties.getProperty(prefix + PREVIOUS)),
                if (scoped || track == ReleaseTrack.FULL) canonical(properties.getProperty(prefix + SEEN)) else null,
            )
        }

        fun canonical(version: String?): String? = version?.trim()?.takeIf { ReleaseVersion.compareVersions(it, it) == 0 }
            ?.removePrefix("v")?.substringBefore('+')

        fun compare(a: String, b: String) = requireNotNull(ReleaseVersion.compareVersions(a, b))
    }
}

/** One-time recovery for installs from before version tracking. The active log belongs to this launch. */
internal object PreviousModVersion {
    private val archive = Regex("""^(\d{4}-\d{2}-\d{2})-(\d{1,6})\.log(?:\.gz)?$""")
    private val modLine = Regex("""^\s+- sparkling-mutuals (\S+)\s*$""")
    private data class Log(val path: Path, val date: String, val index: Int)

    fun fromLogs(directory: Path): String? = runCatching {
        Files.newDirectoryStream(directory).use { paths ->
            paths.mapNotNull { path ->
                val match = archive.matchEntire(path.fileName.toString()) ?: return@mapNotNull null
                Log(path, match.groupValues[1], match.groupValues[2].toInt())
            }.sortedWith(compareByDescending<Log> { it.date }.thenByDescending { it.index })
                .asSequence().take(12).firstNotNullOfOrNull { read(it.path) }
        }
    }.getOrNull()

    private fun read(path: Path): String? = runCatching {
        Files.newInputStream(path).use { raw ->
            val input = if (path.fileName.toString().endsWith(".gz")) GZIPInputStream(raw) else raw
            input.bufferedReader().useLines { lines ->
                lines.take(2_000).firstNotNullOfOrNull { line ->
                    InstalledVersionHistory.canonical(modLine.matchEntire(line)?.groupValues?.get(1))
                }
            }
        }
    }.getOrNull()
}
