package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

/** Help lists actual commands available in the selected mode. */
object CommandHelp {
    private val party = listOf(
        PartyCommandKind.MUTUALS to "Shared sparkling discoveries",
        PartyCommandKind.MISSING to "Your missing sparkling discoveries (optional IGN for another player)",
        PartyCommandKind.TICKETS to "Your Safari ticket counts (optional IGN; !ticket also works)",
        PartyCommandKind.SINCE_SPARKLING to "Recorded runs since your last credited party sparkling",
        PartyCommandKind.SINCE_UNIQUE to "Recorded runs since your last credited unique party sparkling",
        PartyCommandKind.PB_DOOM to "Solo Doomspiral PB (add duo, IGN, or IGN duo)",
        PartyCommandKind.PB_WUMPA to "Solo Wumpa PB (add duo, IGN, or IGN duo)",
        PartyCommandKind.PB_FOREST to "Forest biome PB (IGN for observed unique; IGN party for shared)",
        PartyCommandKind.PB_CAVERN to "Cavern biome PB (IGN for observed unique; IGN party for shared)",
        PartyCommandKind.PB_ICY to "Icy biome PB (IGN for observed unique; IGN party for shared)",
        PartyCommandKind.PB_HAUNTED to "Haunted biome PB (IGN for observed unique; IGN party for shared)",
        PartyCommandKind.MISSING_RUN to "Known tasks for your biome at 30s (or c/f/i/h)",
        PartyCommandKind.HELP to "Command list"
    )
    private val local = listOf(
        "/sm" to "Settings",
        "/sm help" to "List local and party commands for the active mode",
        "/sm gui" to "Move and resize HUDs",
        "/rates" to "Expected runs per Sparkling for the active mode (hover for details; cached data)",
        "/profits <basic|economy|premium|first-class>" to "Top ten by shards + essence/run; /profit also works (cached prices)",
        "/captures" to "Current or last run captures",
        "/sparkling [IGN|party|IGNs]" to "Sparkling card, or shared sparklings",
        "/sm history" to "Runs, bests, sparklings, achievements, stats",
        "/sm history import" to "Rebuild past runs from your logs",
        "/sm debug" to "Save entity diagnostics",
        "/fc" to "Full clear mode",
        "/unique" to "Unique run mode",
        "/sparkle" to "Sparkling run mode",
        "/alert" to "Toggle warp alert and automatic /p warp",
        "/alertdelay <seconds>" to "Warp alert / auto-warp delay",
        "/apikey <key>" to "Set API key"
    )
    /** Party chat words other mods' users can say that this mod reacts to, without a lookup reply. */
    private val partyWords = listOf("!dt [reason]" to "Downtime after this run; say r when ready")
    private fun available(mode: SafariMode) = party.map { (kind, description) -> PartyCommand(kind, if (kind in listOf(PartyCommandKind.MISSING, PartyCommandKind.TICKETS)) "[IGN]" else "") to description }
        .filter { it.first.allowed(mode) }
    fun localLines(mode: SafariMode = SafariFullClear.mode) = listOf("§6Sparkling Mutuals — Commands", "§eParty chat commands") +
        available(mode).map { (command, description) -> "§6${command.text} §7— $description" } +
        partyWords.map { (command, description) -> "§6$command §7— $description" } +
        listOf("§eLocal commands") + local.map { (command, description) -> "§6$command §7— $description" } +
        listOf("§7<IGN> = Minecraft username. Party commands use /pc.")
    fun partyReply(mode: SafariMode = SafariFullClear.mode) = "[SM] Commands: Party: " +
        available(mode).joinToString(", ") { it.first.text } + if (mode != SafariMode.FULL_CLEAR) ", !pb doom duo, !pb wumpa duo, !pb doom <IGN>, !pb wumpa <IGN>" else ""
}
