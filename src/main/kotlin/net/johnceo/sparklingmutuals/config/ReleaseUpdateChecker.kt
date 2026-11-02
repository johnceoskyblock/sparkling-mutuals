package net.johnceo.sparklingmutuals.config

import com.google.gson.JsonParser

/** Refreshable public metadata, independent of Minecraft and network scheduling for regression tests. */
class ReleaseUpdateChecker(
    private val installed: String,
    private val minecraft: String,
    private val fetch: () -> String,
    private val execute: (() -> Unit) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    data class Release(val tag: String, val name: String, val notes: String)
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
                if (row.getAsJsonArray("assets")?.any {
                        it.asJsonObject.get("name")?.asString == "sparkling-mutuals-$version+mc$minecraft.jar"
                    } != true) return@mapNotNull null
                Release(tag, row.get("name")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    row.get("body")?.takeUnless { it.isJsonNull }?.asString.orEmpty())
            }.maxWithOrNull { a, b -> compareVersions(a.tag, b.tag)!! }

        fun compareVersions(a: String, b: String): Int? {
            fun parts(value: String) = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[+-].*)?$")
                .matchEntire(value.trim())?.groupValues?.drop(1)?.map(String::toIntOrNull)
                ?.takeIf { it.all { n -> n != null } }?.filterNotNull()
            val x = parts(a) ?: return null
            val y = parts(b) ?: return null
            return x.indices.map { x[it].compareTo(y[it]) }.firstOrNull { it != 0 } ?: 0
        }
    }
}
