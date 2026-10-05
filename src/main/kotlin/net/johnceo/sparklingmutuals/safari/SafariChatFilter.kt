package net.johnceo.sparklingmutuals.safari

/** Display-only suppression; incoming message events still reach capture and PB tracking. */
object SafariChatFilter {
    private val escaped = Regex("\\bThe (?:${SafariRoster.all.joinToString("|") { Regex.escape(it.name) }}) escaped\\b", RegexOption.IGNORE_CASE)
    fun hidden(raw: String, enabled: Boolean) = enabled && SafariMessages.lines(raw).any { line ->
        listOf("You threw a", "CAPTURE!", "LOOT SHARE!").any { line.contains(it, true) } || escaped.containsMatchIn(line)
    }
}
