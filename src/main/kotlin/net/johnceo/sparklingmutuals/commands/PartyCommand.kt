package net.johnceo.sparklingmutuals.commands

import java.security.MessageDigest
import net.johnceo.sparklingmutuals.history.SafariEncounter
import net.johnceo.sparklingmutuals.history.SafariEncounterHistory
import net.johnceo.sparklingmutuals.history.SafariProfitPeriod
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

data class PartyChat(val sender: String, val body: String) {
    companion object {
        private val envelope = Regex("""^Party\s*>\s*(?:\[[^\]]+]\s*)*([A-Za-z0-9_]{1,16}):\s*(.*)$""")
        fun parse(text: String): PartyChat? {
            val match = envelope.matchEntire(text.replace(Regex("§."), "").trim()) ?: return null
            return PartyChat(match.groupValues[1], match.groupValues[2].trim())
        }
    }
}

enum class PartyCommandKind { MUTUALS, MISSING, TICKETS, CAPTURES, SPARKLING, DYE, PROFIT, RATES, DOOM_CHECKS, WUMPA_CHECKS, GEMZIE_CHECKS, HELP, PB_DOOM, PB_WUMPA, PB_FOREST, PB_HAUNTED, PB_ICY, PB_CAVERN, MISSING_RUN, SINCE_SPARKLING, SINCE_UNIQUE }

data class PartyCommand(val kind: PartyCommandKind, val ign: String = "", val duo: Boolean = false, val trackedPlayer: Boolean = false,
    val biome: SafariBiome? = null, val sinceCritter: String? = null, val profitPeriod: SafariProfitPeriod = SafariProfitPeriod.SESSION) {
    val isSince get() = kind == PartyCommandKind.SINCE_SPARKLING || kind == PartyCommandKind.SINCE_UNIQUE
    val personalResult get() = isSince || encounter != null || kind == PartyCommandKind.DYE || kind == PartyCommandKind.PROFIT || kind == PartyCommandKind.RATES
    internal val encounter get() = when (kind) {
        PartyCommandKind.DOOM_CHECKS -> SafariEncounter.DOOMSPIRAL
        PartyCommandKind.WUMPA_CHECKS -> SafariEncounter.WUMPA
        PartyCommandKind.GEMZIE_CHECKS -> SafariEncounter.GEMZIE
        else -> null
    }
    internal fun encounterResult() = encounter?.let { "$ign has checked ${SafariEncounterHistory.count(it)} ${it.label}'s!" }
    val requiresApiKey get() = kind in listOf(PartyCommandKind.MUTUALS, PartyCommandKind.MISSING, PartyCommandKind.TICKETS, PartyCommandKind.CAPTURES, PartyCommandKind.SPARKLING)
    val pbName get() = when (kind) {
        PartyCommandKind.PB_DOOM -> "Doomspiral"
        PartyCommandKind.PB_WUMPA -> "Wumpa"
        PartyCommandKind.PB_FOREST -> "Forest"
        PartyCommandKind.PB_HAUNTED -> "Haunted"
        PartyCommandKind.PB_ICY -> "Icy"
        PartyCommandKind.PB_CAVERN -> "Cavern"
        else -> null
    }
    fun allowed(mode: SafariMode = SafariFullClear.mode) = when {
        pbName == null -> true
        kind == PartyCommandKind.PB_DOOM || kind == PartyCommandKind.PB_WUMPA -> mode != SafariMode.FULL_CLEAR
        else -> mode != SafariMode.SPARKLING
    }
    /** `!m` describes the run in progress, so only clients inside the Safari answer it. */
    fun answerable(inSafari: Boolean = SafariAssist.inSafari, currentBiome: SafariBiome? = SafariAssist.biome, responderBiome: SafariBiome? = net.johnceo.sparklingmutuals.safari.SafariTracking.ledger.current?.biomeAtThirtySeconds) =
        kind != PartyCommandKind.MISSING_RUN || inSafari && responderBiome != null && (biome == null || biome == responderBiome)
    // Personal replies describe each responding client; explicit PB queries retain their requested IGN.
    fun forResponder(name: String) = if (personalResult || pbName != null && !trackedPlayer)
        copy(ign = name) else this
    private val failurePrefix: String get() = when (kind) {
        PartyCommandKind.MUTUALS -> "Mutual Timesave Shiny Critters: "
        PartyCommandKind.MISSING -> "Missing Timesave Shinies for $ign: "
        PartyCommandKind.TICKETS -> "Safari Tickets for $ign: "
        PartyCommandKind.CAPTURES -> "Safari Captures for $ign: "
        PartyCommandKind.SPARKLING -> "$ign: "
        PartyCommandKind.DYE -> "$ign: Emerald Dyes: "
        PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE, PartyCommandKind.PROFIT, PartyCommandKind.RATES,
        PartyCommandKind.DOOM_CHECKS, PartyCommandKind.WUMPA_CHECKS, PartyCommandKind.GEMZIE_CHECKS -> ""
        PartyCommandKind.HELP -> "Commands: "
        PartyCommandKind.MISSING_RUN -> SafariMissingReply.prefix(biome)
        PartyCommandKind.PB_DOOM -> "Doomspiral PB: "
        else -> "$pbName PB: "
    }

    fun failureResult(message: String): String = failurePrefix + message
    fun dyeResult(amount: Int): String = "Emerald Dyes: $amount"

    fun matchesResponse(body: String): Boolean {
        return when (kind) {
            PartyCommandKind.MISSING, PartyCommandKind.MUTUALS -> {
                val prefixes = if (kind == PartyCommandKind.MISSING)
                    listOf("Missing Timesave Shinies for $ign: ", "Missing Shinies for $ign: ",
                        "Missing Timesave Sparklings for $ign: ", "Missing Sparklings for $ign: ")
                else listOf("Mutual Timesave Shiny Critters: ", "Mutual Shiny Critters: ",
                    "Mutual Timesave Sparkling Critters: ", "Mutual Sparkling Critters: ")
                prefixes.any { prefix ->
                    if (body.startsWith(prefix, true)) lookupResult.matches(body.substring(prefix.length))
                    else {
                        val chunk = Regex("^${Regex.escape(prefix.removeSuffix(": "))} \\[([1-9][0-9]*)/([1-9][0-9]*)\\]: (.+)$", RegexOption.IGNORE_CASE).matchEntire(body)
                        chunk != null && (chunk.groupValues[1].toIntOrNull() ?: Int.MAX_VALUE) <=
                            (chunk.groupValues[2].toIntOrNull() ?: 0) && lookupResult.matches(chunk.groupValues[3])
                    }
                }
            }
            PartyCommandKind.TICKETS -> Regex(
                "^${Regex.escape(ign)}: Basic \\(\\d+\\), Economy \\(\\d+\\), Premium \\(\\d+\\), First-Class \\(\\d+\\)(?:, Total \\(\\d+\\))?$",
                RegexOption.IGNORE_CASE
            ).matches(body)
            PartyCommandKind.CAPTURES -> SafariCaptureReply.matches(ign, body)
            PartyCommandKind.SPARKLING -> SafariSparklingReply.matchesCaptures(ign, body)
            PartyCommandKind.RATES -> SafariSparklingReply.matchesRates(body)
            PartyCommandKind.DOOM_CHECKS, PartyCommandKind.WUMPA_CHECKS, PartyCommandKind.GEMZIE_CHECKS ->
                Regex("^${Regex.escape(ign)} has checked \\d+ ${encounter!!.label}'s!$", RegexOption.IGNORE_CASE).matches(body)
            PartyCommandKind.DYE -> Regex("^Emerald Dyes: \\d+$", RegexOption.IGNORE_CASE).matches(body)
            PartyCommandKind.PROFIT -> body.startsWith(SafariProfitReply.PREFIX + profitPeriod.label + ", ", true)
            PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE -> SafariSinceReply.matches(this, body)
            PartyCommandKind.HELP -> helpReply.matches(body)
            PartyCommandKind.MISSING_RUN -> body.startsWith(SafariMissingReply.prefix(biome), true)
            else -> Regex(
                "^${if (ign.isEmpty()) "[A-Za-z0-9_]{1,16}" else Regex.escape(ign)}'s ${pbName}${if (trackedPlayer) "${if (kind in listOf(PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA)) "(?: Observed)?" else " Observed"}${if (duo && kind in listOf(PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA)) " Duo" else ""}" else if (duo) " Duo" else ""}${if (kind in listOf(PartyCommandKind.PB_FOREST, PartyCommandKind.PB_HAUNTED, PartyCommandKind.PB_ICY, PartyCommandKind.PB_CAVERN)) { if (trackedPlayer) " Unique Run${if (duo) " Party" else ""}" else "(?: (?:Full Clear|Unique Run))?" } else ""} PB: (?:\\d+:\\d{2}\\.\\d{3}|Not recorded yet)(?: \\| Party PB: \\d+:\\d{2}\\.\\d{3})?$",
                RegexOption.IGNORE_CASE).matches(body)
        }
    }

    val text: String get() = when (kind) {
        PartyCommandKind.MUTUALS -> "!mutuals"
        PartyCommandKind.MISSING -> "!missing $ign"
        PartyCommandKind.TICKETS -> "!tickets $ign"
        PartyCommandKind.CAPTURES -> "!captures $ign"
        PartyCommandKind.SPARKLING -> "!sparkling $ign"
        PartyCommandKind.RATES -> "!rates"
        PartyCommandKind.DOOM_CHECKS -> "!doom"
        PartyCommandKind.WUMPA_CHECKS -> "!wumpa"
        PartyCommandKind.GEMZIE_CHECKS -> "!gemzie"
        PartyCommandKind.DYE -> "!dye"
        PartyCommandKind.PROFIT -> "!profit ${profitPeriod.name.lowercase()}"
        PartyCommandKind.SINCE_SPARKLING -> sinceCritter?.let { "!since $it" } ?: "!since"
        PartyCommandKind.SINCE_UNIQUE -> "!since unique"
        PartyCommandKind.HELP -> "!commands"
        PartyCommandKind.MISSING_RUN -> if (biome == null) "!m" else "!m ${biome.label.lowercase()}"
        PartyCommandKind.PB_DOOM -> "!pb doom${if (trackedPlayer) " $ign${if (duo) " duo" else ""}" else if (duo) " duo" else ""}"
        PartyCommandKind.PB_WUMPA -> "!pb wumpa${if (trackedPlayer) " $ign${if (duo) " duo" else ""}" else if (duo) " duo" else ""}"
        else -> "!pb ${pbName!!.lowercase()}${if (trackedPlayer) " $ign${if (duo) " party" else ""}" else ""}"
    }

    companion object {
        private val names = (SafariRoster.all.map { Regex.escape(it.name) } + "All Birds").joinToString("|")
        private val helpReply = Regex("""\[SM\] Commands(?: \[[1-9][0-9]*/[1-9][0-9]*\])?: .+""")
        private val lookupResult = Regex("^(?:None|None\\. :skull:|All\\. :skull:|(?:$names)(?:, (?:$names))*)$", RegexOption.IGNORE_CASE)
        private val bossLookup = Regex("""^!pb\s+(doom|wumpa|forest|haunted|icy|cavern)\s+([A-Za-z0-9_]{1,16})(?:\s+(duo|party))?$""", RegexOption.IGNORE_CASE)
        private val lookup = Regex("""^!(missing|tickets?|captures|sparkling)(?:\s+([A-Za-z0-9_]{1,16}))?$""", RegexOption.IGNORE_CASE)
        private val since = Regex("""^!since(?:\s+(.+))?$""", RegexOption.IGNORE_CASE)
        private val profit = Regex("""^!profit(?:\s+(session|lifetime|week|day))?$""", RegexOption.IGNORE_CASE)
        private val sinceAliases = mapOf("doom" to "Doomspiral", "goat" to "Billygoat",
            "shrimp" to "Mantis Shrimp", "nozzle" to "Nozzlenose", "squid" to "Shuddersquid")
        private val missingRun = Regex("""^!m(?:\s+(\S+))?$""", RegexOption.IGNORE_CASE)
        fun fromChat(text: String): PartyRequest? {
            val chat = PartyChat.parse(text) ?: return null
            val command = when (chat.body.lowercase()) {
                "!mutual", "!mutuals" -> PartyCommand(PartyCommandKind.MUTUALS)
                "!commands" -> PartyCommand(PartyCommandKind.HELP)
                "!dye" -> PartyCommand(PartyCommandKind.DYE)
                "!rates" -> PartyCommand(PartyCommandKind.RATES)
                "!doom", "!dooms" -> PartyCommand(PartyCommandKind.DOOM_CHECKS)
                "!wumpa", "!wumpas" -> PartyCommand(PartyCommandKind.WUMPA_CHECKS)
                "!gemzie" -> PartyCommand(PartyCommandKind.GEMZIE_CHECKS)
                "!pb doom" -> PartyCommand(PartyCommandKind.PB_DOOM)
                "!pb doom duo" -> PartyCommand(PartyCommandKind.PB_DOOM, duo = true)
                "!pb wumpa" -> PartyCommand(PartyCommandKind.PB_WUMPA)
                "!pb wumpa duo" -> PartyCommand(PartyCommandKind.PB_WUMPA, duo = true)
                "!pb forest" -> PartyCommand(PartyCommandKind.PB_FOREST)
                "!pb haunted" -> PartyCommand(PartyCommandKind.PB_HAUNTED)
                "!pb icy" -> PartyCommand(PartyCommandKind.PB_ICY)
                "!pb cavern" -> PartyCommand(PartyCommandKind.PB_CAVERN)
                else -> profit.matchEntire(chat.body)?.let { match ->
                    val period = SafariProfitPeriod.entries.firstOrNull { it.name.equals(match.groupValues[1], true) }
                        ?: SafariProfitPeriod.SESSION
                    PartyCommand(PartyCommandKind.PROFIT, profitPeriod = period)
                } ?: since.matchEntire(chat.body)?.let { match ->
                    val argument = match.groupValues[1]
                    when {
                        argument.isEmpty() -> PartyCommand(PartyCommandKind.SINCE_SPARKLING)
                        argument.equals("unique", true) -> PartyCommand(PartyCommandKind.SINCE_UNIQUE)
                        else -> {
                            val words = argument.replace(Regex("""\s+"""), " ")
                            val normalized = sinceAliases[words.lowercase(java.util.Locale.ROOT)] ?: words
                            val critter = SafariRoster.all.firstOrNull { it.name.equals(normalized, true) } ?: return null
                            PartyCommand(PartyCommandKind.SINCE_SPARKLING, sinceCritter = critter.name)
                        }
                    }
                } ?: missingRun.matchEntire(chat.body)?.let { match ->
                    val word = match.groupValues[1]
                    if (word.isEmpty()) PartyCommand(PartyCommandKind.MISSING_RUN)
                    else PartyCommand(PartyCommandKind.MISSING_RUN, biome = SafariMissingReply.biome(word) ?: return null)
                } ?: run {
                    val boss = bossLookup.matchEntire(chat.body)
                    if (boss != null) PartyCommand(when (boss.groupValues[1].lowercase()) {
                        "doom" -> PartyCommandKind.PB_DOOM; "wumpa" -> PartyCommandKind.PB_WUMPA
                        "forest" -> PartyCommandKind.PB_FOREST; "haunted" -> PartyCommandKind.PB_HAUNTED
                        "icy" -> PartyCommandKind.PB_ICY; else -> PartyCommandKind.PB_CAVERN
                    }, boss.groupValues[2], duo = boss.groupValues[3].isNotEmpty(), trackedPlayer = true)
                    else {
                        val match = lookup.matchEntire(chat.body) ?: return null
                        val kind = when (match.groupValues[1].lowercase()) {
                            "missing" -> PartyCommandKind.MISSING
                            "captures" -> PartyCommandKind.CAPTURES
                            "sparkling" -> PartyCommandKind.SPARKLING
                            else -> PartyCommandKind.TICKETS
                        }
                        PartyCommand(kind, match.groupValues[2].ifBlank { chat.sender })
                    }
                }
            }
            return PartyRequest(chat.sender, command)
        }
    }
}

data class PartyRequest(val sender: String, val command: PartyCommand) {
    val token: String = MessageDigest.getInstance("SHA-256")
        .digest("${sender.lowercase()}:${command.text.lowercase()}".toByteArray(Charsets.UTF_8))
        .take(6).joinToString("") { "%02x".format(it.toInt() and 255) }
}
