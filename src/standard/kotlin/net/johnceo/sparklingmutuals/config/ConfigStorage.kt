package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader

/** The legit edition never imports, moves or deletes another edition's configuration. */
internal object ConfigStorage {
    private const val folder = "sparkling-mutuals-legit"
    val directory: Path get() = FabricLoader.getInstance().configDir.resolve(folder)
    fun migrate(root: Path): Path = Files.createDirectories(root.resolve(folder))
}
