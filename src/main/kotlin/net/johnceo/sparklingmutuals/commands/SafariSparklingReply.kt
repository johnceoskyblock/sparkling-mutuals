package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.api.SafariProfile
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.SafariSessionStats

internal object SafariSparklingReply {
    private const val RATES_PREFIX = "Session sparklings: "
    private val captureCounts = Regex("""Total: (?:[0-9]+|N/A), Unique: [0-9]+, Duplicate: (?:[0-9]+|N/A)""", RegexOption.IGNORE_CASE)
    private val sessionRates = Regex("""$RATES_PREFIX[0-9]+(?: \([0-9]+\.[0-9] runs/sparkling\))?""", RegexOption.IGNORE_CASE)

    fun captures(ign: String, profile: SafariProfile): String =
        "$ign: Total: ${profile.totalCaptured ?: "N/A"}, Unique: ${profile.caught.size}, Duplicate: ${profile.duplicateSparklings ?: "N/A"}"

    fun matchesCaptures(ign: String, body: String) = body.substringBefore(": ").equals(ign, true) &&
        captureCounts.matches(body.substringAfter(": ", ""))

    fun currentRates() = rates(RunHistory.currentSessionStats())

    fun rates(stats: SafariSessionStats): String = RATES_PREFIX + stats.sparklings +
        stats.formattedRunsPerSparkling?.let { " ($it runs/sparkling)" }.orEmpty()

    fun matchesRates(body: String): Boolean = sessionRates.matches(body)
}
