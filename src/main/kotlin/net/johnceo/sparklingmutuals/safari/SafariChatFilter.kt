package net.johnceo.sparklingmutuals.safari

/** Display-only suppression; incoming message events still reach capture and PB tracking. */
object SafariChatFilter {
    private val moundNoise = setOf("Small cracks begin to form in the mound...",
        "The cracks seem to be getting larger, keep hitting it!", "Chunks of the mound begin falling away...",
        "The mound is about to fall to pieces! Keep going!")
    private val escaped = Regex("\\bThe (?:${SafariRoster.all.joinToString("|") { Regex.escape(it.name) }}) escaped\\b", RegexOption.IGNORE_CASE)
    fun hidden(raw: String, enabled: Boolean) = enabled && SafariMessages.lines(raw).any { line ->
        listOf("You threw a", "CAPTURE!", "LOOT SHARE!").any { line.contains(it, true) } || escaped.containsMatchIn(line) || line in moundNoise
    }
}
