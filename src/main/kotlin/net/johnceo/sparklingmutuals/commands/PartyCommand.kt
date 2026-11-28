package net.johnceo.sparklingmutuals.commands

import java.security.MessageDigest
import java.util.Locale
import net.johnceo.sparklingmutuals.config.BiomeObjectiveConfig
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

enum class PartyCommandKind { MUTUALS, MISSING, TICKETS, CAPTURES, SPARKLING, DYE, PROFIT, RATES, DOOM_CHECKS, WUMPA_CHECKS, GEMZIE_CHECKS, HELP, PB_DOOM, PB_WUMPA, PB_FOREST, PB_HAUNTED, PB_ICY, PB_CAVERN, MISSING_RUN, SINCE_SPARKLING, SINCE_UNIQUE, SINCE_DYE }

data class PartyCommand(val kind: PartyCommandKind, val ign: String = "", val duo: Boolean = false, val trackedPlayer: Boolean = false,
    val biome: SafariBiome? = null, val sinceCritter: String? = null, val profitPeriod: SafariProfitPeriod = SafariProfitPeriod.SESSION) {
    val isSince get() = kind == PartyCommandKind.SINCE_SPARKLING || kind == PartyCommandKind.SINCE_UNIQUE || kind == PartyCommandKind.SINCE_DYE
    internal val requiresHistoryAccount get() = encounter != null || kind == PartyCommandKind.DYE || kind == PartyCommandKind.SINCE_DYE
    val personalResult get() = isSince || encounter != null || kind == PartyCommandKind.DYE || kind == PartyCommandKind.PROFIT || kind == PartyCommandKind.RATES
    internal val encounter get() = when (kind) {
        PartyCommandKind.DOOM_CHECKS -> SafariEncounter.DOOMSPIRAL
        PartyCommandKind.WUMPA_CHECKS -> SafariEncounter.WUMPA
        PartyCommandKind.GEMZIE_CHECKS -> SafariEncounter.GEMZIE
        else -> null
    }
    internal fun encounterResult() = encounter?.let { "$ign has checked ${SafariEncounterHistory.count(it)} ${it.label}s!" }
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
        PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE, PartyCommandKind.SINCE_DYE, PartyCommandKind.PROFIT, PartyCommandKind.RATES,
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
                Regex("^${Regex.escape(ign)} has checked \\d+ ${encounter!!.label}'?s!$", RegexOption.IGNORE_CASE).matches(body)
            PartyCommandKind.DYE -> Regex("^Emerald Dyes: \\d+$", RegexOption.IGNORE_CASE).matches(body)
            PartyCommandKind.PROFIT -> body.startsWith(SafariProfitReply.PREFIX + profitPeriod.label + ", ", true)
            PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE, PartyCommandKind.SINCE_DYE -> SafariSinceReply.matches(this, body)
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
        PartyCommandKind.SINCE_DYE -> "!since dye"
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
        internal const val LOOKUP_NAMES = "missing|tickets?|captures?|sparklings?"
        internal const val PB_BOSSES = "doom(?:spiral)?|wumpa"
        internal const val PB_TARGETS = "$PB_BOSSES|forest|haunted|icy|cavern"
        private val bossLookup = Regex("""^!pbs? ($PB_TARGETS)(?: ([A-Za-z0-9_]{1,16}))?(?: (duo|party))?$""", RegexOption.IGNORE_CASE)
        private val lookup = Regex("""^!($LOOKUP_NAMES)(?: ([A-Za-z0-9_]{1,16}))?$""", RegexOption.IGNORE_CASE)
        private val since = Regex("""^!since(?:\s+(.+))?$""", RegexOption.IGNORE_CASE)
        private val profit = Regex("""^!profits?(?: (session|lifetime|week|day))?$""", RegexOption.IGNORE_CASE)
        private val spaces = Regex("""\s+""")
        private val sparklingCheck = Regex("""!sparkle (.+) (on|off)""")
        private val sinceAliases = mapOf("doom" to "Doomspiral", "goat" to "Billygoat",
            "shrimp" to "Mantis Shrimp", "nozzle" to "Nozzlenose", "squid" to "Shuddersquid")
        private val missingRun = Regex("""^!m(?:\s+(\S+))?$""", RegexOption.IGNORE_CASE)
        internal fun requestedMode(body: String): SafariMode? = when (normalize(body).lowercase(Locale.ROOT)) {
            "!unique", "!uniques", "!unique mode" -> SafariMode.UNIQUE
            "!fc", "!fullclear", "!full clear", "!full clear mode" -> SafariMode.FULL_CLEAR
            "!sparkle", "!sparkle mode", "!sparkling mode" -> SafariMode.SPARKLING
            else -> null
        }
        internal const val POST_CLEAR_CHECKS = "after clear"

        internal fun requestedSparklingCheck(body: String): Pair<String, Boolean>? {
            val match = sparklingCheck.matchEntire(normalize(body).lowercase(Locale.ROOT)) ?: return null
            val target = when (val name = match.groupValues[1]) {
                "doom", "dooms" -> "doomspiral"
                else -> name
            }
            val check = if (target == POST_CLEAR_CHECKS) POST_CLEAR_CHECKS else
                BiomeObjectiveConfig.optionalSpecies.firstOrNull { it.equals(target, ignoreCase = true) } ?: return null
            return check to (match.groupValues[2] == "on")
        }
        internal fun normalize(body: String) = body.trim().replace(spaces, " ")

        fun fromChat(text: String): PartyRequest? {
            val chat = PartyChat.parse(text) ?: return null
            val body = normalize(chat.body)
            val command = when (body.lowercase(Locale.ROOT)) {
                "!mutual", "!mutuals" -> PartyCommand(PartyCommandKind.MUTUALS)
                "!command", "!commands", "!help" -> PartyCommand(PartyCommandKind.HELP)
                "!dye", "!dyes", "!emerald dye", "!emerald dyes" -> PartyCommand(PartyCommandKind.DYE)
                "!rate", "!rates" -> PartyCommand(PartyCommandKind.RATES)
                "!doom", "!dooms", "!doomspiral", "!doomspirals" -> PartyCommand(PartyCommandKind.DOOM_CHECKS)
                "!wumpa", "!wumpas" -> PartyCommand(PartyCommandKind.WUMPA_CHECKS)
                "!gemzie", "!gemzies" -> PartyCommand(PartyCommandKind.GEMZIE_CHECKS)
                else -> profit.matchEntire(body)?.let { match ->
                    val period = SafariProfitPeriod.entries.firstOrNull { it.name.equals(match.groupValues[1], true) }
                        ?: SafariProfitPeriod.SESSION
                    PartyCommand(PartyCommandKind.PROFIT, profitPeriod = period)
                } ?: since.matchEntire(body)?.let { match ->
                    val argument = match.groupValues[1].lowercase(Locale.ROOT)
                    when (argument) {
                        "" -> PartyCommand(PartyCommandKind.SINCE_SPARKLING)
                        "unique", "uniques" -> PartyCommand(PartyCommandKind.SINCE_UNIQUE)
                        "dye", "dyes", "emerald dye", "emerald dyes" -> PartyCommand(PartyCommandKind.SINCE_DYE)
                        else -> {
                            val normalized = sinceAliases[argument] ?: argument
                            val critter = SafariRoster.all.firstOrNull { it.name.equals(normalized, true) } ?: return null
                            PartyCommand(PartyCommandKind.SINCE_SPARKLING, sinceCritter = critter.name)
                        }
                    }
                } ?: missingRun.matchEntire(body)?.let { match ->
                    val word = match.groupValues[1]
                    if (word.isEmpty()) PartyCommand(PartyCommandKind.MISSING_RUN)
                    else PartyCommand(PartyCommandKind.MISSING_RUN, biome = SafariMissingReply.biome(word) ?: return null)
                } ?: run {
                    val boss = bossLookup.matchEntire(body)
                    if (boss != null) {
                        val target = boss.groupValues[1].lowercase(Locale.ROOT)
                        val name = boss.groupValues[2]
                        val localDuo = boss.groupValues[3].isEmpty() && name.equals("duo", true) && target.matches(Regex(PB_BOSSES))
                        PartyCommand(when (target) {
                        "doom", "doomspiral" -> PartyCommandKind.PB_DOOM; "wumpa" -> PartyCommandKind.PB_WUMPA
                        "forest" -> PartyCommandKind.PB_FOREST; "haunted" -> PartyCommandKind.PB_HAUNTED
                        "icy" -> PartyCommandKind.PB_ICY; else -> PartyCommandKind.PB_CAVERN
                        }, if (localDuo) "" else name, duo = localDuo || boss.groupValues[3].isNotEmpty(),
                            trackedPlayer = name.isNotEmpty() && !localDuo)
                    } else {
                        val match = lookup.matchEntire(body) ?: return null
                        val kind = when (match.groupValues[1].lowercase()) {
                            "missing" -> PartyCommandKind.MISSING
                            "capture", "captures" -> PartyCommandKind.CAPTURES
                            "sparkling", "sparklings" -> PartyCommandKind.SPARKLING
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
