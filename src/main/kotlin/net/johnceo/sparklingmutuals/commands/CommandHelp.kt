package net.johnceo.sparklingmutuals.commands

import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

/** Local slash-command help and the separate mode-aware party command reply. */
object CommandHelp {
    private val party = listOf(
        "!unique | !fc | !sparkle",
        "!sparkle <critter> on/off",
        "!sparkle ${PartyCommand.POST_CLEAR_CHECKS} on/off",
        "!hotspot on/off",
        "!dt [reason]",
        "!m [biome]",
        "!mutuals",
        "(!missing|!tickets|!captures|!sparkling) [IGN]",
        "!since [unique|dye|<critter>]",
        "!profit [session|lifetime|week|day]",
        "!rates | !dye",
        "!doom | !wumpa | !gemzie"
    )
    private val local = buildList {
        add("/sm [help]" to "Open settings or list commands")
        add("/sm gui | /sm objectives" to "Edit HUDs or open Biome Objectives settings")
        add("/unique | /fc | /sparkle" to "Set Safari mode. /fullclear and /full clear also work")
        add("/hotspot [preferences]" to "Toggle Hotspot assigning or rank biomes")
        add("/sparkling [IGN…|party] | /captures" to "Look up collections or view current-run captures")
        add("/rates | /profits [ticket]" to "Estimate runs per sparkling or rank critters by coins per run")
        add("/sm <history|bests|sparklings|stats|profits|achievements>" to "Open a history tab")
        add("/sm history import [folder] | /newsession" to "Import logs or start a profit session")
        if (!SafariSettings.standardEdition) {
            add("/warping | /leech" to "Toggle hosting or automatic invite acceptance")
            add("/start | /autoparty" to "Start the warp cycle or toggle Warp List invites")
            add("/warpingdelay <seconds> | /duo [IGN]" to "Set the warp delay or view and set your duo runner")
        }
        add("/apikey <key>" to "Set a personal API key")
        add("/sm debug" to "Save diagnostics")
    }

    fun localLines() = listOf("§6Sparkling Mutuals — Commands") +
        local.map { (command, description) -> "§6$command §7— $description" }

    fun partyReply(mode: SafariMode = SafariFullClear.mode) = buildList {
        addAll(party)
        if (PartyCommand(PartyCommandKind.PB_DOOM).allowed(mode)) add("!pb <doom|wumpa> [IGN] [duo]")
        if (PartyCommand(PartyCommandKind.PB_FOREST).allowed(mode)) add("!pb <forest|cavern|icy|haunted> [IGN [party]]")
        add("!commands")
    }.joinToString(", ", prefix = "[SM] Commands: ")
}
