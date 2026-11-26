package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

/** Local slash-command help and the separate mode-aware party command reply. */
object CommandHelp {
    private val party = listOf(
        PartyCommandKind.MUTUALS, PartyCommandKind.MISSING, PartyCommandKind.TICKETS, PartyCommandKind.CAPTURES, PartyCommandKind.SPARKLING, PartyCommandKind.DYE,
        PartyCommandKind.SINCE_SPARKLING, PartyCommandKind.SINCE_UNIQUE, PartyCommandKind.PROFIT, PartyCommandKind.RATES,
        PartyCommandKind.DOOM_CHECKS, PartyCommandKind.WUMPA_CHECKS, PartyCommandKind.GEMZIE_CHECKS,
        PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA, PartyCommandKind.PB_FOREST,
        PartyCommandKind.PB_CAVERN, PartyCommandKind.PB_ICY, PartyCommandKind.PB_HAUNTED,
        PartyCommandKind.MISSING_RUN, PartyCommandKind.HELP
    )
    private val local = listOf(
        "/sm" to "Settings",
        "/sm help" to "Commands",
        "/sm gui" to "Move and resize HUDs",
        "/unique" to "Unique mode",
        "/fc | /fullclear | /full clear" to "Full clear mode",
        "/sparkle" to "Sparkle mode",
        "/sparkling [IGN|party]" to "Sparkling collections and player capture stats",
        "/captures" to "Run captures",
        "/rates" to "Expected runs per sparkling",
        "/profits [ticket]" to "Top 10 coins/run; /profit also works",
        "/hotspot" to "Toggle Hotspot assigning",
        "/sm history" to "Run history",
        "/sm bests | sparklings | stats | profits" to "Open a history tab",
        "/newsession" to "Start a new profit session",
        "/sm history import" to "Import logs",
        "/apikey <key>" to "Optional personal API key",
        "/sm debug" to "Save diagnostics"
    )
    private fun available(mode: SafariMode) = party.map { kind ->
        PartyCommand(kind, if (kind in listOf(PartyCommandKind.MISSING, PartyCommandKind.TICKETS, PartyCommandKind.CAPTURES, PartyCommandKind.SPARKLING)) "[IGN]" else "")
    }.filter { it.allowed(mode) }

    @Suppress("UNUSED_PARAMETER")
    fun localLines(mode: SafariMode = SafariFullClear.mode) = listOf("§6Sparkling Mutuals — Commands") +
        local.map { (command, description) -> "§6$command §7— $description" }

    fun partyReply(mode: SafariMode = SafariFullClear.mode) = "[SM] Commands: " +
        available(mode).joinToString(", ") { when(it.kind) {
            PartyCommandKind.SINCE_UNIQUE -> "${it.text}, !since <critter>"
            PartyCommandKind.PROFIT -> "!profit"
            PartyCommandKind.PB_DOOM, PartyCommandKind.PB_WUMPA -> "${it.text} [IGN] [duo]"
            else -> it.text
        } } + ", !hotspot on|off"
}
