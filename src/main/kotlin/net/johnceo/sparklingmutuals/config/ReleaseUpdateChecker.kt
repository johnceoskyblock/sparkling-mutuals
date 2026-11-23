package net.johnceo.sparklingmutuals.config

import com.google.gson.JsonParser
import java.net.URI

/** Refreshable public metadata, independent of Minecraft and network scheduling for regression tests. */
class ReleaseUpdateChecker(
    private val installed: String,
    private val minecraft: String,
    private val fetch: () -> String,
    private val execute: (() -> Unit) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onUpdate: (Release) -> Unit = {},
) {
    data class Release(val tag: String, val name: String, val notes: String,
        val asset: URI? = null, val manifest: URI? = null)
    data class State(val label: String = "Check updates", val release: Release? = null)
    @Volatile var state = State(); private set
    private var running = false
    private var nextCheck = Long.MIN_VALUE

    @Synchronized fun check(force: Boolean = false) {
        if (running || !force && clock() < nextCheck) return
        running = true
        state = state.copy(label = "Checking…")
        execute {
            var success = false
            try {
                val found = newest(fetch(), minecraft) ?: error("No compatible stable release")
                val comparison = compareVersions(found.tag, installed) ?: error("Unknown installed version")
                state = State(when {
                    comparison > 0 -> "Update available"
                    comparison < 0 -> "Development build"
                    else -> "Up to Date"
                }, found)
                success = true
                if (comparison > 0) onUpdate(found)
            } catch (_: Exception) {
                state = state.copy(label = "Couldn't check")
            } finally {
                synchronized(this) {
                    nextCheck = clock() + if (success) 300_000L else 60_000L
                    running = false
                }
            }
        }
    }

    companion object {
        fun newest(body: String, minecraft: String): Release? = JsonParser.parseString(body).asJsonArray
            .map { it.asJsonObject }.mapNotNull { row ->
                val tag = row.get("tag_name")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                if (compareVersions(tag, tag) == null || row.get("draft")?.asBoolean == true ||
                    row.get("prerelease")?.asBoolean == true) return@mapNotNull null
                val version = tag.removePrefix("v")
                if (!version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) return@mapNotNull null
                val assets = row.getAsJsonArray("assets")?.map { it.asJsonObject }.orEmpty()
                fun candidates(name: String) = assets.filter { it.get("name")?.asString == name }
                fun trustedUrl(name: String): URI? {
                    val asset = candidates(name).singleOrNull() ?: return null
                    val expected = "https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/$tag/$name"
                    val url = asset.get("browser_download_url")?.takeUnless { it.isJsonNull }?.asString ?: return null
                    return URI.create(expected).takeIf { expected == url.replace("%2B", "+", ignoreCase = true) }
                }
                val name = "sparkling-mutuals-$version-full+mc$minecraft.jar"
                val fullAssets = candidates(name)
                if (fullAssets.size > 1) return@mapNotNull null
                if (fullAssets.isEmpty() && (candidates("build-manifest.json").isNotEmpty() ||
                        candidates("sparkling-mutuals-$version+mc$minecraft.jar").size != 1)) return@mapNotNull null
                Release(tag, row.get("name")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    row.get("body")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    trustedUrl(name), trustedUrl("build-manifest.json"))
            }.maxWithOrNull { a, b -> compareVersions(a.tag, b.tag)!! }

        fun compareVersions(a: String, b: String): Int? = ReleaseVersion.compareVersions(a, b)
    }
}
