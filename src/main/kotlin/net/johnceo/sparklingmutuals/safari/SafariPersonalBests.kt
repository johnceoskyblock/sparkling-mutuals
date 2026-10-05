package net.johnceo.sparklingmutuals.safari

import java.util.Properties

/** Only the player's successful CAPTURE messages can improve a personal best. */
class SafariPersonalBests {
    @Volatile private var doom: Long? = null
    @Volatile private var wumpa: Long? = null
    private val capture = Regex("^CAPTURE! You caught a (Doomspiral|Wumpa)(?![A-Za-z]).*$")
    fun time(species: String) = when (species) { "Doomspiral" -> doom; "Wumpa" -> wumpa; else -> null }
    fun record(raw: String, run: SafariRun, now: Long): Boolean {
        val species = capture.matchEntire(SafariRules.strip(raw))?.groupValues?.get(1) ?: return false
        if (run.endedAt != null || now < run.startedAt) return false
        val elapsed = now - run.startedAt
        val best = time(species)
        if (best != null && elapsed >= best) return false
        if (species == "Doomspiral") doom = elapsed else wumpa = elapsed
        return true
    }
    fun load(properties: Properties) {
        fun read(key: String) = properties.getProperty("pb.${key}Millis")?.toLongOrNull()?.takeIf { it >= 0 }
        doom = read("doomspiral"); wumpa = read("wumpa")
    }
    fun save(properties: Properties) {
        doom?.let { properties.setProperty("pb.doomspiralMillis", it.toString()) }
        wumpa?.let { properties.setProperty("pb.wumpaMillis", it.toString()) }
    }
    fun response(player: String, species: String): String {
        val formatted = time(species)?.let { "%d:%02d.%03d".format(it / 60000, it / 1000 % 60, it % 1000) } ?: "Not recorded yet"
        return "$player's $species PB: $formatted"
    }
}
