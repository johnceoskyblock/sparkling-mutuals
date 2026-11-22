package net.johnceo.sparklingmutuals.config

import net.fabricmc.loader.api.FabricLoader

/** Version information for the welcome message and What's New screen. */
object ModUpdates {
    const val REPOSITORY = "https://github.com/johnceoskyblock/sparkling-mutuals"
    const val canCheck = false
    val installed: String = FabricLoader.getInstance().getModContainer("sparkling-mutuals")
        .map { it.metadata.version.friendlyString }.orElse("dev")
    val minecraftVersion: String = FabricLoader.getInstance().getModContainer("minecraft")
        .map { it.metadata.version.friendlyString }.orElse("")
    data class Release(val tag: String, val name: String, val notes: String)
    val release: Release? = null
    val label = "Updates disabled"
    fun initialize(@Suppress("UNUSED_PARAMETER") configDirectory: java.nio.file.Path) = Unit
    fun check(@Suppress("UNUSED_PARAMETER") force: Boolean = false) = Unit
    fun compareVersions(a: String, b: String) = ReleaseVersion.compareVersions(a, b)
}
