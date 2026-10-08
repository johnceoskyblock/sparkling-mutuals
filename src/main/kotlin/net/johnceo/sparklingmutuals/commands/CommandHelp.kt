package net.johnceo.sparklingmutuals.commands

/** Local help uses one described command per line; the party reply stays a single coordinated message. */
object CommandHelp {
    private val party = listOf(
        "!mutual / !mutuals" to "Shared sparklings (timesaves or all species)",
        "!missing <IGN>" to "A player's missing sparklings (timesaves or all species)",
        "!ticket <IGN> / !tickets <IGN>" to "A player's Safari tickets",
        "!pb doom" to "Each mod user's Doomspiral best time",
        "!pb wumpa" to "Each mod user's Wumpa best time",
        "!pb forest" to "Each mod user's selected Forest biome PB",
        "!pb haunted" to "Each mod user's selected Haunted biome PB",
        "!pb icy" to "Each mod user's selected Icy biome PB",
        "!pb cavern" to "Each mod user's selected Cavern biome PB",
        "!commands" to "This command list"
    )
    private val local = listOf(
        "/sparkling" to "Open settings",
        "/sparkling gui" to "Move and resize HUDs",
        "/sparkling catches" to "View current or last run captures",
        "/sparkling debug" to "Save nearby critter tracking details to latest.log",
        "/sparkling fc" to "Full clear: Full Clear PBs and all-species lookups; off: Unique Run PBs and timesaves",
        "/alert" to "Toggle warp reminders",
        "/alertdelay <seconds>" to "Set warp reminder delay",
        "/apikey <key>" to "Save your Hypixel API key"
    )
    fun localLines() = listOf("§6Sparkling Mutuals — Commands", "§eParty chat commands") +
        party.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§eLocal commands") + local.map { (command, description) -> "§b$command §7— $description" } +
        listOf("§7<IGN> means Minecraft username. Use /pc for party commands.")
    fun partyReply() = "[SM] Commands: Party: !mutual, !missing <IGN>, !ticket <IGN>, !pb doom/wumpa/forest/haunted/icy/cavern, !commands | " +
        "Local: /sparkling [gui|fc=mode/PBs/lookups], /sparkling catches, /alert, /alertdelay <s>, /apikey <key>"
}
