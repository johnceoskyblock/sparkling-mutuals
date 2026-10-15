package net.johnceo.sparklingmutuals.history

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/** JSON files in the config folder for the history features, each read once and rewritten whole. */
object HistoryStore {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/history")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    /** Overridable for tests. */
    var directory: Path? = null
    private fun path(name: String) = (directory ?: FabricLoader.getInstance().configDir).resolve("sparkling-mutuals-$name.json")

    fun <T> load(name: String, type: Class<T>, empty: () -> T): T {
        val file = path(name)
        if (!Files.exists(file)) return empty()
        return runCatching { gson.fromJson(Files.readString(file), type) ?: empty() }
            .onFailure { logger.warn("Couldn't read {} — starting empty", file.fileName, it) }.getOrElse { empty() }
    }
    /** One recoverable snapshot of each history file before a manual import. */
    fun backup(name: String) {
        val file = path(name)
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.fileName.toString() + ".pre-import.bak"),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
    fun save(name: String, value: Any): Result<Unit> {
        return runCatching {
            val file = path(name); Files.createDirectories(file.parent)
            val temporary = Files.createTempFile(file.parent, file.fileName.toString(), ".tmp")
            try {
                Files.writeString(temporary, gson.toJson(value))
                try { Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
            } finally { Files.deleteIfExists(temporary) }
            Unit
        }.onFailure { logger.warn("Couldn't save {}", name, it) }
    }
}
