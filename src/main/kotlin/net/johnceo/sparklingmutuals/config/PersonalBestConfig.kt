package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.util.Properties
import net.johnceo.sparklingmutuals.safari.completion.SafariPersonalBests

/** Write the PB destination successfully before removing any legacy PB keys. */
internal class PersonalBestConfig(private val main: Path) {
    val path: Path = main.resolveSibling("sparkling-mutuals-pbs.properties")
    private val previousPath = main.resolveSibling("Sparkling Mutual's PBs.properties")
    fun load(legacy: Properties, bests: SafariPersonalBests) {
        fun normalized(properties: Properties) = Properties().also { out ->
            SafariPersonalBests().apply { load(properties); save(out) }
        }
        val merged = normalized(legacy)
        for (source in listOf(previousPath, path).filter { Files.exists(it) }) {
            val saved = Properties().apply { Files.newInputStream(source).use(::load) }
            normalized(saved).forEach { key, value ->
                val old = merged.getProperty(key.toString())?.toLongOrNull()
                val current = value.toString().toLongOrNull()
                merged[key] = if (key.toString().endsWith("Millis") && old != null && current != null)
                    minOf(old, current).toString() else value
            }
        }
        bests.load(merged)
        val keys = legacy.stringPropertyNames().filter { it.startsWith("pb.") }
        if (keys.isNotEmpty() || Files.exists(previousPath)) {
            save(bests)
            keys.forEach(legacy::remove)
            if (keys.isNotEmpty()) write(main, legacy, "Sparkling Mutuals configuration")
            Files.deleteIfExists(previousPath)
        }
    }
    fun save(bests: SafariPersonalBests) {
        val out = Properties().also(bests::save)
        write(path, out, "Sparkling Mutual's PBs")
    }
    companion object {
        fun write(path: Path, properties: Properties, comment: String) {
            Files.createDirectories(path.parent)
            val temporary = Files.createTempFile(path.parent, "sparkling-mutuals-", ".tmp")
            try {
                Files.newOutputStream(temporary).use { properties.store(it, comment) }
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally { Files.deleteIfExists(temporary) }
        }
    }
}
