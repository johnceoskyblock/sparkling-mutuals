package net.johnceo.sparklingmutuals.config

import java.util.Properties

/** Curated preferences only. Never seed account credentials, history, timers or statistics. */
object FreshInstallDefaults {
    fun main() = read("sparkling-mutuals.properties")
    fun contest() = read("sparkling-mutuals-contest.properties")
    private fun read(name: String) = Properties().apply {
        checkNotNull(FreshInstallDefaults::class.java.getResourceAsStream("/defaults/$name")) {
            "Missing bundled defaults: $name"
        }.use(::load)
    }
}
