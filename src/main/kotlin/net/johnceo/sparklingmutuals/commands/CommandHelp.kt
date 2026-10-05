package net.johnceo.sparklingmutuals.commands

/** Local help uses one described command per line; the party reply stays a single coordinated message. */
object CommandHelp {
    private val party = listOf(
        "!mutual / !mutuals" to "Shared timesave sparklings",
        "!missing <IGN>" to "A player's missing timesaves",
        "!ticket <IGN> / !tickets <IGN>" to "A player's Safari tickets",
        "!pb doom" to "Responder's Doomspiral best time",
        "!pb wumpa" to "Responder's Wumpa best time",
        "!commands" to "This command list"
    )
    private val local = listOf(
        "/sparkling" to "Open settings",
        "/sparkling gui" to "Move and resize HUDs",
        "/sparkling catches" to "View current or last run captures",
        "/alert" to "Toggle warp reminders",
        "/alertdelay <seconds>" to "Set warp reminder delay",
        "/apikey <key>" to "Save your Hypixel API key"
    )
    fun localLines() = listOf("§6Sparkling Mutuals — Commands", "§eParty chat commands") +
        party.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§eLocal commands") + local.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§7<IGN> means Minecraft username. Use /pc for party commands.")
    fun partyReply() = "[SM] Commands: Party: !mutual(s), !missing <IGN>, !ticket(s) <IGN>, !pb doom, !pb wumpa, !commands | " +
        "Local: /sparkling, /sparkling gui, /sparkling catches, /alert, /alertdelay <s>, /apikey <key>"
}
