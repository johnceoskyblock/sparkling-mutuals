package net.johnceo.sparklingmutuals.history

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HistoryArchiveOwnershipTest {
    @TempDir lateinit var folder: Path
    private val owner = "1".repeat(32)
    private val other = "2".repeat(32)
    private fun system(time: String, text: String) = "[$time] [Render thread/INFO]: [System] [CHAT] $text"
    private fun client(time: String, text: String) = "[$time] [Render thread/INFO]: $text"
    private fun log(file: String, vararg lines: String) {
        val path = folder.resolve(file)
        Files.createDirectories(path.parent)
        val content = lines.joinToString("\n")
        if (file.endsWith(".gz")) GZIPOutputStream(Files.newOutputStream(path)).use { it.write(content.toByteArray()) }
        else Files.writeString(path, content)
    }
    private val ticket = "[NPC] Safari Manager: Looks good to me. Have fun out there!"
    private val hotspot = "HOTSPOT! Your Hunting Hotspot is the Forest Biome!"
    private val summary = "SAFARI REWARD SUMMARY"
    private val shiny = "SPARKLING! Former caught a SPARKLING Wumpa!"
    private val receipt = "CAPTURE! You caught a SPARKLING Wumpa and received a Rainbow Feather and 20x Wumpa Shard!"

    @Test fun `verified rollover joins rewards and ownership across gzip and copied logs`() {
        val start = arrayOf(
            client("23:50:00", "Setting user: Former"),
            client("23:50:01", "Connecting to hypixel.net, 25565"),
            system("23:59:55", ticket), system("23:59:58", hotspot))
        val end = arrayOf(
            system("00:00:00", shiny), system("00:00:00", receipt),
            system("00:00:05", SafariEncounter.DOOMSPIRAL.announcement),
            system("00:01:00", summary), system("00:01:01", "+350 Safari Essence"))
        log("2026-10-10-2.log.gz", *start)
        log("2026-10-11-1.log.gz", *end)
        log("backup/2026-10-11-1 (1).log.gz", *end)
        val result = LogImporter.read(folder, "Current", ownerUuid = owner, knownNames = mapOf(owner to setOf("Former"))).join()
        val imported = result.runs.single()
        assertEquals(0, result.unresolvedAccounts)
        assertEquals(0, result.unknownServer)
        assertEquals(1, imported.tickets)
        assertEquals(62_000L, imported.length)
        assertEquals(350, imported.safariEssence)
        assertEquals(true, result.sparklings.single().personal)
        assertEquals(SafariEncounter.DOOMSPIRAL, result.encounters.single().encounter)
        val live = RunRecord(start = imported.start - 2_500, ticketAt = imported.ticketAt + 350, length = 64_500,
            caught = 21, coins = 777, ownerUuid = owner)
        val saved = mutableListOf(live)
        repeat(2) { assertEquals(0, HistoryMerge.merge(saved, result.runs)) }
        assertSame(live, saved.single())
        assertEquals(21, live.caught)
        assertEquals(777L, live.coins)
        assertEquals(-1, live.safariEssence)
    }

    @Test fun `orphan files use same second first person proof and reject ordinary party evidence`() {
        log("2026-10-11-1.log", client("01:00:00", "Connecting to hypixel.net, 25565"),
            system("01:00:01", hotspot), system("01:01:00", shiny), system("01:01:00", receipt),
            system("01:01:10", summary))
        log("2026-10-11-2.log", client("02:00:00", "Connecting to hypixel.net, 25565"),
            system("02:00:01", hotspot), system("02:01:00", shiny), system("02:01:10", summary))
        log("2026-10-11-3.log", client("03:00:00", "[SM History] Account $other Former"),
            client("03:00:01", "Connecting to hypixel.net, 25565"), system("03:00:02", ticket), system("03:01:00", summary))
        val result = LogImporter.read(folder, "Former", ownerUuid = owner).join()
        assertEquals(0, result.runs.size, "An IGN claimed by two UUIDs needs direct account evidence")
        assertEquals(2, result.unresolvedAccounts)
        assertEquals(1, result.skippedAccounts)
        Files.delete(folder.resolve("2026-10-11-3.log"))
        val recovered = LogImporter.read(folder, "Former", ownerUuid = owner).join()
        assertEquals(1, recovered.runs.size)
        assertEquals(1, recovered.unresolvedAccounts)
        assertEquals(true, recovered.sparklings.single().personal)
    }

    @Test fun `main alpha and later main stay distinct and unverified server is reported`() {
        log("2026-10-10-1.log",
            client("13:00:00", "[SM History] Account $owner Former"),
            system("13:00:01", shiny),
            client("13:01:00", "Connecting to hypixel.net, 25565"),
            system("13:01:01", ticket), system("13:01:02", hotspot),
            system("13:01:10", SafariEncounter.WUMPA.announcement), system("13:02:00", summary),
            client("13:03:00", "Connecting to alpha.hypixel.net, 25565"),
            system("13:03:01", hotspot), system("13:03:10", SafariEncounter.WUMPA.announcement),
            system("13:03:11", shiny), system("13:04:00", summary),
            client("13:05:00", "Connecting to proxy.hypixel.net, 25565"),
            system("13:05:01", hotspot), system("13:05:10", SafariEncounter.GEMZIE.announcement), system("13:06:00", summary))
        val result = LogImporter.read(folder, "Former", ownerUuid = owner).join()
        assertEquals(2, result.runs.size)
        assertEquals(1, result.alphaRuns.size)
        assertEquals(1, result.unknownServer)
        assertEquals(0, result.sparklings.size)
        assertEquals(1, result.alphaSparklings.size)
        assertEquals(listOf(SafariEncounter.WUMPA, SafariEncounter.GEMZIE), result.encounters.map { it.encounter })
    }

    @Test fun `verified encounters count without a run or ticket and a chamber adds three checks`() {
        log("2026-10-12-1.log", client("12:00:00", "[SM History] Account $owner Former"),
            client("12:00:01", "Connecting to hypixel.net, 25565"),
            system("12:01:00", SafariEncounter.DOOMSPIRAL.announcement),
            system("12:02:00", SafariEncounter.WUMPA.announcement),
            system("12:03:00", SafariEncounter.GEMZIE.announcement),
            system("12:04:00", SafariEncounter.GEMZIE.announcement),
            system("12:05:00", "Party > Former: ${SafariEncounter.GEMZIE.announcement}"),
            client("12:06:00", "Connecting to alpha.hypixel.net, 25565"),
            system("12:07:00", SafariEncounter.GEMZIE.announcement))
        val result = LogImporter.read(folder, "Former", ownerUuid = owner).join()
        assertTrue(result.runs.isEmpty())
        assertTrue(result.ticketTimes.isEmpty())
        assertEquals(listOf(SafariEncounter.DOOMSPIRAL, SafariEncounter.WUMPA, SafariEncounter.GEMZIE, SafariEncounter.GEMZIE),
            result.encounters.map { it.encounter })

        val previousDirectory = HistoryStore.directory
        HistoryStore.directory = folder.resolve("history")
        try {
            assertEquals(8, SafariEncounterHistory.import(owner, result.encounters))
            assertEquals(0, SafariEncounterHistory.import(owner, result.encounters))
        } finally {
            HistoryStore.directory = previousDirectory
        }
    }

    @Test fun `menu readers pick personal dye total and parse detailed playtime`() {
        assertEquals(4, PlaytimeReader.dyeDrops("§7You've dropped: §64"))
        assertNull(PlaytimeReader.dyeDrops("Global drops: 2,187"))
        assertEquals(2251L, PlaytimeReader.playtimeMinutes("Playtime: 2,251 minutes"))
        assertEquals(1595L, PlaytimeReader.playtimeMinutes("Total Playtime: 1 day 2 hours 35 minutes"))
    }

    @Test fun `reimport corrects a previously counted free admission only on imported rows`() {
        val saved = RunRecord(start = 100_000, length = 60_000, imported = true, tickets = 1)
        val live = RunRecord(start = 200_000, length = 60_000, tickets = 1)
        val rows = mutableListOf(saved, live)
        val logMatches = listOf(RunRecord(start = 100_000, ticketAt = 99_000, imported = true),
            RunRecord(start = 200_000, ticketAt = 199_000, imported = true))
        repeat(2) { assertEquals(0, HistoryMerge.merge(rows, logMatches)) }
        assertEquals(0, saved.tickets)
        assertEquals(1, live.tickets)
        assertEquals(0L, live.ticketAt)
    }
}
