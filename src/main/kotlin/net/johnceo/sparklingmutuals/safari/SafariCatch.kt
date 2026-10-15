package net.johnceo.sparklingmutuals.safari

data class SafariCatch(val critter: SafariCritter, val personal: Boolean = true, val catcherName: String? = null, val sparkling: Boolean = false) {
    companion object {
        private val catcher = Regex("\\bfrom\\s+(?:\\[[^]]+head]\\s*)?(\\w{1,16})\\s+(?:catching|finding)\\b")
        fun parse(raw: String): SafariCatch? {
            val text = SafariRules.strip(raw)
            val own = text.startsWith("CAPTURE!")
            if (!own && !text.startsWith("LOOT SHARE!")) return null
            val species = SafariRoster.findIn(text) ?: return null
            val personal = own && Regex("^CAPTURE! You (?:caught|found)\\b").containsMatchIn(text)
            val from = catcher.find(text)?.groupValues?.get(1)
            return if (personal || !own && from != null) SafariCatch(species, personal, from, Regex("\\bSPARKLING\\s+${Regex.escape(species.name)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) else null
        }
    }
}
