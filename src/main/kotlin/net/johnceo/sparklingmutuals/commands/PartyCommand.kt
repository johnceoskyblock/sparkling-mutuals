package net.johnceo.sparklingmutuals.commands

import java.security.MessageDigest

data class PartyChat(val sender: String, val body: String) {
    companion object {
        private val envelope = Regex("""^Party\s*>\s*(?:\[[^\]]+]\s*)*([A-Za-z0-9_]{1,16}):\s*(.*)$""")
        fun parse(text: String): PartyChat? {
            val match = envelope.matchEntire(text.replace(Regex("§."), "").trim()) ?: return null
            return PartyChat(match.groupValues[1], match.groupValues[2].trim())
        }
    }
}

enum class PartyCommandKind { MUTUALS, MISSING, TICKETS, HELP, PB_DOOM, PB_WUMPA }

data class PartyCommand(val kind: PartyCommandKind, val ign: String = "") {
    private val failurePrefix: String get() = when (kind) {
        PartyCommandKind.MUTUALS -> "Mutual Timesave Sparkling Critters: "
        PartyCommandKind.MISSING -> "Missing Timesave Sparklings for $ign: "
        PartyCommandKind.TICKETS -> "Safari Tickets for $ign: "
        PartyCommandKind.HELP -> "Commands: "
        PartyCommandKind.PB_DOOM -> "Doomspiral PB: "
        PartyCommandKind.PB_WUMPA -> "Wumpa PB: "
    }

    fun failureResult(message: String): String = failurePrefix + message

    fun matchesResponse(body: String): Boolean {
        if (body.startsWith(failurePrefix, ignoreCase = true) && body.length > failurePrefix.length) return true
        return when (kind) {
            PartyCommandKind.TICKETS -> Regex(
                "^${Regex.escape(ign)}: Basic \\(\\d+\\), Economy \\(\\d+\\), Premium \\(\\d+\\), First-Class \\(\\d+\\)$",
                RegexOption.IGNORE_CASE
            ).matches(body)
            PartyCommandKind.HELP -> body.startsWith("[SM] Commands: ")
            PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA -> Regex(
                "^[A-Za-z0-9_]{1,16}'s ${if (kind == PartyCommandKind.PB_DOOM) "Doomspiral" else "Wumpa"} PB: (?:\\d+:\\d{2}\\.\\d{3}|Not recorded yet)$",
                RegexOption.IGNORE_CASE).matches(body)
            else -> false
        }
    }

    val text: String get() = when (kind) {
        PartyCommandKind.MUTUALS -> "!mutuals"
        PartyCommandKind.MISSING -> "!missing $ign"
        PartyCommandKind.TICKETS -> "!tickets $ign"
        PartyCommandKind.HELP -> "!commands"
        PartyCommandKind.PB_DOOM -> "!pb doom"
        PartyCommandKind.PB_WUMPA -> "!pb wumpa"
    }

    companion object {
        private val lookup = Regex("""^!(missing|tickets?)\s+([A-Za-z0-9_]{1,16})$""", RegexOption.IGNORE_CASE)
        fun fromChat(text: String): PartyRequest? {
            val chat = PartyChat.parse(text) ?: return null
            val command = when (chat.body.lowercase()) {
                "!mutual", "!mutuals" -> PartyCommand(PartyCommandKind.MUTUALS)
                "!commands" -> PartyCommand(PartyCommandKind.HELP)
                "!pb doom" -> PartyCommand(PartyCommandKind.PB_DOOM)
                "!pb wumpa" -> PartyCommand(PartyCommandKind.PB_WUMPA)
                else -> {
                    val match = lookup.matchEntire(chat.body) ?: return null
                    PartyCommand(if (match.groupValues[1].equals("missing", true))
                        PartyCommandKind.MISSING else PartyCommandKind.TICKETS, match.groupValues[2])
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
