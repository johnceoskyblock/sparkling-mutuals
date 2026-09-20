package net.johnceo.sparklingmutuals

import java.nio.file.Files
import java.nio.file.Path

object ConfigManager {

    private lateinit var configPath: Path

    var apiKey: String = ""

    fun init(configDirectory: Path) {
        configPath = configDirectory.resolve("sparkling-mutuals.properties")

        if (Files.exists(configPath)) {
            load()
        }
    }

    private fun load() {
        val properties = java.util.Properties()

        Files.newInputStream(configPath).use {
            properties.load(it)
        }

        apiKey = properties.getProperty("apiKey", "")
    }

    fun save() {
        val properties = java.util.Properties()
        properties.setProperty("apiKey", apiKey)

        Files.createDirectories(configPath.parent)

        Files.newOutputStream(configPath).use {
            properties.store(it, "Sparkling Mutuals configuration")
        }
    }
}