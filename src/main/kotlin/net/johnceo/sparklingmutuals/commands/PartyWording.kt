package net.johnceo.sparklingmutuals.commands

/** Reserve detection keywords for real Sparkling alerts; local presentation keeps its original text. */
object PartyWording {
    private val words = Regex("sparkling(?:s)?|sparkle(?:s)?", RegexOption.IGNORE_CASE)
    fun shiny(text: String): String = words.replace(text) { match ->
        val original = match.value
        val replacement = if (original.endsWith('s', ignoreCase = true)) "shinies" else "shiny"
        when {
            original.all(Char::isUpperCase) -> replacement.uppercase(java.util.Locale.ROOT)
            original.first().isUpperCase() -> replacement.replaceFirstChar(Char::uppercaseChar)
            else -> replacement
        }
    }
}
