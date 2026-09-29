package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object ConfigManager {

    private lateinit var configPath: Path

    var apiKey = ""

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")

        if (Files.exists(configPath)) {
            load()
        }
    }

    private fun load() {
        Properties().apply {
            Files.newInputStream(configPath).use(::load)
            apiKey = getProperty("apiKey", "")
        }
    }

    fun save() {
        Files.createDirectories(configPath.parent)

        Properties().apply {
            setProperty("apiKey", apiKey)
            Files.newOutputStream(configPath).use {
                store(it, "Sparkling Mutuals configuration")
            }
        }
    }
}