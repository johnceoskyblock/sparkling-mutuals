package net.johnceo.sparklingmutuals.commands

/** Local help uses one described command per line; the party reply stays a single coordinated message. */
object CommandHelp {
    private val party = listOf(
        "!mutual / !mutuals" to "Shared sparklings (timesaves or all species)",
        "!missing <IGN>" to "A player's missing sparklings (timesaves or all species)",
        "!ticket <IGN> / !tickets <IGN>" to "A player's Safari tickets",
        "!pb doom" to "Each mod user's Doomspiral best time",
        "!pb wumpa" to "Each mod user's Wumpa best time",
        "!pb forest" to "Each mod user's Forest full clear best time",
        "!pb haunted" to "Each mod user's Haunted full clear best time",
        "!pb icy" to "Each mod user's Icy full clear best time",
        "!pb cavern" to "Each mod user's Cavern full clear best time",
        "!commands" to "This command list"
    )
    private val local = listOf(
        "/sparkling" to "Open settings",
        "/sparkling gui" to "Move and resize HUDs",
        "/sparkling catches" to "View current or last run captures",
        "/sparkling full clear" to "Toggle the full clear preset",
        "/sparkling timesave" to "Toggle timesaves versus all species for party lookups",
        "/alert" to "Toggle warp reminders",
        "/alertdelay <seconds>" to "Set warp reminder delay",
        "/apikey <key>" to "Save your Hypixel API key"
    )
    fun localLines() = listOf("§6Sparkling Mutuals — Commands", "§eParty chat commands") +
        party.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§eLocal commands") + local.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§7<IGN> means Minecraft username. Use /pc for party commands.")
    fun partyReply() = "[SM] Commands: Party: !mutual(s), !missing <IGN>, !ticket(s) <IGN>, !pb doom/wumpa/forest/haunted/icy/cavern, !commands | " +
        "Local: /sparkling [gui|full clear=preset|timesave=timesaves/all], /sparkling catches, /alert, /alertdelay <s>, /apikey <key>"
}
