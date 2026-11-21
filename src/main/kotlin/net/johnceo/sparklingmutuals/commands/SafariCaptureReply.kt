package net.johnceo.sparklingmutuals.commands

import java.util.Locale
import net.johnceo.sparklingmutuals.api.ApiFailure
import net.johnceo.sparklingmutuals.api.ApiFailureKind
import net.johnceo.sparklingmutuals.api.SafariProfile
import net.johnceo.sparklingmutuals.safari.SafariBiome

internal object SafariCaptureReply {
    private const val TOTAL = "Total captures"
    private const val NUMBER = "(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)"
    private val counts = Regex("^$TOTAL $NUMBER" + SafariBiome.entries.joinToString("") {
        "(?: \\| ${it.label} $NUMBER)?"
    } + "$", RegexOption.IGNORE_CASE)

    fun amount(count: Long) = String.format(Locale.US, "%,d", count)

    fun party(ign: String, profile: SafariProfile): String {
        val total = profile.totalCritterCaptures ?: throw ApiFailure(ApiFailureKind.DATA_UNAVAILABLE,
            "Safari capture counts unavailable for $ign.")
        val biomes = SafariBiome.entries.mapNotNull { biome ->
            profile.biomeCaptures[biome]?.let { "${biome.label} ${amount(it)}" }
        }
        return (listOf("$ign: $TOTAL ${amount(total)}") + biomes).joinToString(" | ")
    }

    fun matches(ign: String, body: String) = body.substringBefore(": ").equals(ign, true) &&
        counts.matches(body.substringAfter(": ", ""))
}
