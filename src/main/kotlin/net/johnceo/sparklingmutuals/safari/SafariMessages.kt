package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.completion.SparklingChecks

/** Hypixel's entry notice may be inside one multiline component, as documented by v0.9.0. */
object SafariMessages {
    fun forRun(raw: String, run: SafariRun?, inSafari: Boolean): List<String> = lines(raw,
        run?.sparklingChecks?.takeIf { inSafari },
        run?.takeIf { inSafari })
    private val separators = Regex("\\r?\\n|\\\\n")
    private val playerChat = Regex("^(?:(?:Party|Guild|Officer|Co-op|Coop)\\s*>|(?:From|To)\\s+|<\\w{1,16}>|(?:\\[[^]]+]\\s*)*\\w{1,16}(?:\\s*\\[[^]]+])*:)", RegexOption.IGNORE_CASE)
    private val entry = Regex("^(?:\\[[^]]+]\\s*)*(\\w{1,16}) entered Critter Safari!$")
    fun lines(raw: String, partyChecks: SparklingChecks? = null, fullClearRun: SafariRun? = null): List<String> {
        if (playerChat.containsMatchIn(SafariRules.strip(raw))) {
            fullClearRun?.resources?.partyNotice(raw)
            partyChecks?.chat(raw, manualAllowed = true)
            fullClearRun?.partyFullClear(raw)
            return emptyList()
        }
        return raw.split(separators).map(SafariRules::strip).filter(String::isNotEmpty)
    }
    fun enteredBy(line: String, player: String) = entry.matchEntire(line)?.groupValues?.get(1).equals(player, true)
}
