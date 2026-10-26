package net.johnceo.sparklingmutuals.history

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.GZIPOutputStream
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.johnceo.sparklingmutuals.history.importing.HistoryEstimates
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.history.importing.SparklingHistoryMerge
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HistoryBackfillImportTest {
    @TempDir lateinit var dir: Path
    private val day = LocalDate.of(2026, 10, 10)
    private val ticket = "[NPC] Safari Manager: Looks good to me. Have fun out there!"
    private val capture = "CAPTURE! You caught a Cavernfish and gained 2x Cavernfish Shard!"
    private fun at(h: Int, m: Int, s: Int = 0) = day.atTime(h,m,s).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun line(time: String, body: String, prefix: String = "[Render thread/INFO] [System] [CHAT]") = "[$time] $prefix $body"
    private fun parse(vararg lines: String) = LogImporter.Parser("Local").apply { read(lines.joinToString("\n").reader().buffered(), day) }

    @Test fun `HTML prefix variants and found broadcasts rebuild both histories with summary duration`() {
        val parser = parse(line("13:00:00", ticket), line("13:00:02", "HOTSPOT! Your Hunting Hotspot is the Forest Biome!"),
            line("13:00:03", capture, "[Render thread/INFO]: [CHAT]"),
            line("13:00:04", "LOOT SHARE! You received a Macaw Shard from [Friend head]Friend catching a Macaw!"),
            line("13:00:05", "SPARKLING! [Local head]Local found a SPARKLING Hideyho!"),
            line("13:00:06", "Since you're in the Critter Safari, the Rainbow Feather was sent to your stash!"),
            line("13:01:00", "SAFARI REWARD SUMMARY"), line("13:10:00", "Unrelated lobby chat"))
        val run = parser.runs.single()
        assertEquals(at(13,0,2), run.start)
        assertEquals(58000, run.length)
        assertEquals(at(13,0), run.ticketAt)
        assertEquals(1, run.tickets); assertEquals(1, run.caught); assertEquals(1, run.lootShares)
        assertEquals(2, run.party); assertEquals(1, run.you); assertEquals(2, run.partySize)
        assertEquals(mapOf("Cavernfish" to 2, "Macaw" to 1), run.shards)
        assertEquals(1, run.macaws); assertEquals(1, run.feathers); assertEquals(1, run.ownSparklings)
        assertTrue(run.modeEstimated); assertEquals("SPARKLING", run.mode)
        val spark = parser.sparklings.single()
        assertEquals("Local", spark.player); assertEquals("Hideyho", spark.critter); assertEquals(1, spark.tickets)
    }
    @Test fun `party guild and private pasted events never become tickets captures or sparks`() {
        val parser = parse(line("13:00:00", "Party > Friend: $ticket"), line("13:00:01", "Guild > Friend: $capture"),
            line("13:00:02", "From Friend: SPARKLING! Friend caught a SPARKLING Macaw!"),
            line("13:00:03", "Friend: HOTSPOT! Your Hunting Hotspot is the Forest Biome!"))
        assertTrue(parser.runs.isEmpty()); assertTrue(parser.sparklings.isEmpty()); assertTrue(parser.ticketTimes.isEmpty())
    }
    @Test fun `each unmatched ticket creates a separate run without inventing an unknown duration`() {
        val parser = parse(line("13:00:00", ticket), line("13:00:01", capture),
            line("13:02:00", ticket), line("13:02:01", capture))
        assertEquals(2, parser.runs.size)
        assertTrue(parser.runs.all { it.length == 0L && it.caught == 1 && it.tickets == 1 })
    }
    @Test fun `hotspots and summaries work even with a missing ticket line`() {
        val parser = parse(line("13:00:00", "HOTSPOT! Your Hunting Hotspot is the Cavern Biome!"),
            line("13:00:05", capture), line("13:01:00", "SAFARI REWARD SUMMARY"))
        assertEquals(0, parser.runs.single().tickets)
        assertEquals(60000L, parser.runs.single().length)
    }
    @Test fun `overlapping files keep same-second event multiplicity and do not duplicate either history`() {
        val lines = listOf(line("13:00:00", ticket), line("13:00:01", capture), line("13:00:01", capture),
            line("13:00:02", "SPARKLING! Local caught a SPARKLING Macaw!"), line("13:01:00", "SAFARI REWARD SUMMARY")).joinToString("\n")
        val parser = LogImporter.Parser("Local")
        parser.read(lines.reader().buffered(), day, source = "archive")
        parser.read(lines.reader().buffered(), day, source = "copy")
        assertEquals(1, parser.runs.size); assertEquals(2, parser.runs.single().caught)
        assertEquals(1, parser.sparklings.size); assertEquals(1, parser.ticketTimes.size)
    }
    @Test fun `file scan accepts plain logs gzip archives latest and chat JSON and reports corrupt files`() {
        Files.writeString(dir.resolve("2026-10-10-1.log"), listOf(line("13:00:00", ticket),line("13:00:01",capture),line("13:01:00","SAFARI REWARD SUMMARY")).joinToString("\n"))
        GZIPOutputStream(Files.newOutputStream(dir.resolve("2026-10-10-2.log.gz"))).bufferedWriter().use { it.write(listOf(line("14:00:00",ticket),line("14:00:01",capture),line("14:01:00","SAFARI REWARD SUMMARY")).joinToString("\n")) }
        Files.writeString(dir.resolve("latest.log"), line("15:00:00", "SPARKLING! Local found a SPARKLING Wumpa!"))
        Files.writeString(dir.resolve("chat.json"), """{"entries":[{"at":${at(16,0)},"player":"Friend","critter":"Gemzie"}]}""")
        Files.writeString(dir.resolve("broken.log.gz"), "not a gzip")
        Files.writeString(dir.resolve("ignore.txt"), "irrelevant")
        val result = LogImporter.read(dir,"Local").join()
        assertEquals(5, result.files); assertEquals(2, result.runs.size); assertEquals(2, result.sparklings.size)
        assertEquals("broken.log.gz", result.failures.single().file)
    }
    @Test fun `ANSI formatting fractional time and curly apostrophes are accepted`() {
        val parser = parse(line("13:00:00.123", "\u001B[32m[NPC] Safari Manager: I already saw your ticket, so you’re free to go.\u001B[0m"),
            line("13:00:02,456", "§dSPARKLING! §a[Friend head]Friend caught a SPARKLING Macaw!"), line("13:01:00", "SAFARI REWARD SUMMARY"))
        assertEquals(1, parser.runs.single().tickets); assertEquals(1, parser.sparklings.single().tickets)
    }
    @Test fun `midnight and different file boundaries retain ticket intervals without merging separate runs`() {
        val parser = LogImporter.Parser("Local")
        parser.read(listOf(line("23:59:00",ticket),line("23:59:01",capture)).joinToString("\n").reader().buffered(), day)
        parser.read(listOf(line("00:00:02","SPARKLING! Local caught a SPARKLING Macaw!"),line("00:01:00","SAFARI REWARD SUMMARY"),
            line("00:02:00", ticket),line("00:03:00","SAFARI REWARD SUMMARY")).joinToString("\n").reader().buffered(),day.plusDays(1))
        assertEquals(2,parser.runs.size); assertEquals(120000L,parser.runs.first().length)
        assertEquals(1,parser.sparklings.single().tickets)
    }
    @Test fun `stash followup does not count the same received feather twice`() {
        val parser = parse(line("13:00:00",ticket), line("13:00:01", "CAPTURE! You caught a SPARKLING Bloodbat and received a Rainbow Feather and 3x Bloodbat Shard!"),
            line("13:00:02","Since you're in the Critter Safari, the Rainbow Feather was sent to your stash!"),line("13:01:00","SAFARI REWARD SUMMARY"))
        assertEquals(1,parser.runs.single().feathers)
        assertEquals(3,parser.runs.single().shards["Bloodbat"])
    }
    @Test fun `new nearby run cannot steal an exact existing run or mutate native fields`() {
        val native = RunRecord(start=at(13,0),caught=9,mode="FULL_CLEAR",coins=123)
        val existing=mutableListOf(native)
        assertEquals(1,HistoryMerge.merge(existing,listOf(RunRecord(start=at(12,59),imported=true),RunRecord(start=at(13,0),imported=true))))
        assertSame(native,existing.first()); assertEquals(9,native.caught); assertEquals(123,native.coins)
        assertEquals("FULL_CLEAR",native.mode)
        assertEquals(0,HistoryMerge.merge(existing,listOf(RunRecord(start=at(12,59),imported=true),RunRecord(start=at(13,0),imported=true))))
    }
    @Test fun `ticket alias matches a late hotspot to its original live run`() {
        val native=RunRecord(start=at(13,0),caught=5)
        val imported=RunRecord(start=at(13,2,40),ticketAt=at(13,0),imported=true)
        val existing=mutableListOf(native)
        assertEquals(0,HistoryMerge.merge(existing,listOf(imported))); assertSame(native,existing.single())
    }
    @Test fun `sparkling merge repairs repeat flags and preserves ticket deltas and legitimate close repeats`() {
        val original=SparklingEntry(at(13,0,5),"Local","Macaw",unique=true,tickets=99)
        val old=SparklingEntry(at(12,0),"Friend","Macaw",tickets=4)
        val entries=mutableListOf(original)
        val found=listOf(old,SparklingEntry(at(13,0,2),"Local","Macaw"),SparklingEntry(at(13,0,5),"Local","Macaw"),
            SparklingEntry(at(13,3),"Friend","Gemzie"))
        assertEquals(3,SparklingHistoryMerge.merge(entries,found,listOf(at(11,0),at(12,30),at(13,1))))
        assertFalse(original.unique); assertEquals(99,original.tickets); assertFalse(original.uniqueEstimated)
        assertEquals(1,entries.first().tickets); assertEquals(1,entries.first { it.at==at(13,0,2) }.tickets)
        assertTrue(old.unique); assertTrue(old.uniqueEstimated)
        assertFalse(entries.first { it.at==at(13,0,2) }.unique)
        assertEquals(0,SparklingHistoryMerge.merge(entries,found,listOf(at(11,0),at(12,30),at(13,1))))
    }
    @Test fun `five minute duplicate window no longer discards separate sparkling catches`() {
        val entries=mutableListOf(SparklingEntry(at(13,0),"Local","Macaw"))
        assertEquals(1,SparklingHistoryMerge.merge(entries,listOf(SparklingEntry(at(13,2),"Local","Macaw")),emptyList()))
        assertEquals(2,entries.size)
    }
    @Test fun `median estimates use recorded runs and imports do not overwrite existing JSON or live counter`() {
        assertEquals(200L,HistoryEstimates.coins(RunRecord(caught=3),listOf(RunRecord(caught=3,coins=100),RunRecord(caught=3,coins=200),RunRecord(caught=3,coins=300))))
        assertEquals(0L,HistoryEstimates.coins(RunRecord(),emptyList()))
        HistoryStore.directory=dir
        try {
            val before=SparklingLog.ticketsSinceSparkling
            HistoryStore.save("runs",RunHistory.Data(runs=mutableListOf(RunRecord(start=1,caught=3)))).getOrThrow()
            val original=Files.readString(dir.resolve("sparkling-mutuals-runs.json"))
            HistoryStore.backup("runs")
            HistoryStore.save("runs",RunHistory.Data()).getOrThrow()
            assertEquals(original,Files.readString(dir.resolve("sparkling-mutuals-runs.json.pre-import.bak")))
            SparklingLog.importEntries(listOf(SparklingEntry(at(13,0),"Local","Macaw")))
            assertEquals(before,SparklingLog.ticketsSinceSparkling)
            assertTrue(Files.exists(dir.resolve("sparkling-mutuals-sparklings.json")))
        } finally { HistoryStore.directory=null }
    }
    @Test fun `History GUI has ordered shared import action and no launch import switch`() {
        ConfigManager.init(dir)
        val buttons=SafariSettings.History::class.java.fields.mapNotNull { it.getAnnotation(ConfigEditorButton::class.java) }
        assertEquals(listOf("History","Import History from Logs"),buttons.take(2).map { it.buttonText })
        assertEquals(listOf(10,11),buttons.take(2).map { it.runnableId })
        assertFalse(SafariSettings.History::class.java.fields.any { it.name=="autoImport" })
    }
    @Test fun `chat JSON handles message fields and external epoch or ISO timestamps for both histories`() {
        val parser = LogImporter.Parser("Local")
        val json = """{"entries":[
            {"timestamp":${at(13,0)/1000},"message":"[System] [CHAT] $ticket"},
            {"at":${at(13,0,1)},"text":"$capture"},
            {"epochMillis":${at(13,0,2)},"content":"SPARKLING! Friend found a SPARKLING Hideyho!"},
            {"time":"${java.time.Instant.ofEpochMilli(at(13,1))}","line":"SAFARI REWARD SUMMARY"}
        ]}"""
        parser.readJson(json.reader().buffered(), day)
        assertEquals(1,parser.runs.single().caught)
        assertEquals(60000L,parser.runs.single().length)
        assertEquals("Hideyho",parser.sparklings.single().critter)
        assertEquals(1,parser.sparklings.single().tickets)
    }
    @Test fun `truncated gzip rolls back that files partial events and valid files still import`() {
        val path=dir.resolve("2026-10-10-1.log.gz")
        GZIPOutputStream(Files.newOutputStream(path)).bufferedWriter().use {
            it.write(listOf(line("13:00:00",ticket),line("13:00:01",capture),line("13:01:00","SAFARI REWARD SUMMARY")).joinToString("\n"))
        }
        val bytes=Files.readAllBytes(path)
        Files.write(path,bytes.copyOf(bytes.size-8))
        Files.writeString(dir.resolve("2026-10-10-2.log"),line("14:00:00","SPARKLING! Friend caught a SPARKLING Macaw!"))
        val result=LogImporter.read(dir,"Local").join()
        assertEquals(1,result.failures.size)
        assertTrue(result.runs.isEmpty())
        assertEquals(1,result.sparklings.size)
    }

}
