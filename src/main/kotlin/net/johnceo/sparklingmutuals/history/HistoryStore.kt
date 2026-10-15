package net.johnceo.sparklingmutuals.history

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

    private val protectedFiles = mutableSetOf<Path>()
    /** Fail closed for a source that could not be parsed or migrated; all callers share this guard. */
    fun protect(name: String) { protectedFiles.add(path(name).toAbsolutePath().normalize()) }
    fun readObject(name: String): JsonObject? {
        val file = path(name)
        if (!Files.exists(file)) return null
        return JsonParser.parseString(Files.readString(file)).asJsonObject
    }
    /** First schema transition only; never replaces the original recoverable bytes. */
    fun backupOnce(name: String, suffix: String) {
        val file = path(name)
        val backup = file.resolveSibling(file.fileName.toString() + suffix)
        if (Files.exists(backup)) require(Files.isRegularFile(backup) && Files.isReadable(backup)) { "Migration backup is not a readable file" }
        else if (Files.exists(file)) Files.copy(file, backup)
    }

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
            val file = path(name)
            check(file.toAbsolutePath().normalize() !in protectedFiles) { "Source is protected after a failed load or migration" }
            Files.createDirectories(file.parent)
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
