package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariFullClear

/** Help lists actual commands available in the selected mode. */
object CommandHelp {
    private val party = listOf(
        PartyCommandKind.MUTUALS to "Shared sparkling discoveries",
        PartyCommandKind.MISSING to "Missing sparkling discoveries",
        PartyCommandKind.TICKETS to "Safari ticket counts",
        PartyCommandKind.PB_DOOM to "Doomspiral PB",
        PartyCommandKind.PB_WUMPA to "Wumpa PB",
        PartyCommandKind.PB_FOREST to "Forest biome PB",
        PartyCommandKind.PB_CAVERN to "Cavern biome PB",
        PartyCommandKind.PB_ICY to "Icy biome PB",
        PartyCommandKind.PB_HAUNTED to "Haunted biome PB",
        PartyCommandKind.HELP to "Command list"
    )
    private val local = listOf(
        "/sparkling" to "Settings",
        "/sparkling gui" to "Move and resize HUDs",
        "/captures" to "Current or last run captures",
        "/sparkling debug" to "Save entity diagnostics",
        "/fc" to "Full clear mode",
        "/unique" to "Unique run mode",
        "/sparkle" to "Sparkling run mode",
        "/alert" to "Toggle warp reminders",
        "/alertdelay <seconds>" to "Warp reminder delay",
        "/apikey <key>" to "Set API key"
    )
    private fun available(mode: SafariMode) = party.map { (kind, description) -> PartyCommand(kind, "<IGN>") to description }
        .filter { it.first.allowed(mode) }
    fun localLines(mode: SafariMode = SafariFullClear.mode) = listOf("§6Sparkling Mutuals — Commands", "§eParty chat commands") +
        available(mode).map { (command, description) -> "§b${command.text} §7— $description" } +
        listOf("§eLocal commands") + local.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§7<IGN> = Minecraft username. Party commands use /pc.")
    fun partyReply(mode: SafariMode = SafariFullClear.mode) = "[SM] Commands: Party: " +
        available(mode).joinToString(", ") { it.first.text } +
        ". Local: /sparkling, /sparkling gui, /fc, /unique, /sparkle, /captures, /alert, /alertdelay <s>, /apikey <key>"
}
