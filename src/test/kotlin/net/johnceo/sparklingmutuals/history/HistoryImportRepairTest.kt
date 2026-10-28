package net.johnceo.sparklingmutuals.history

import java.time.LocalDate
import net.johnceo.sparklingmutuals.history.importing.HistoryImportRepair
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HistoryImportRepairTest {
    @Test fun `mixed main and alpha session excludes only alpha catches and runs`() {
        val log = """
            [12:00:00] [Render thread/INFO]: Setting user: Local
            [12:00:01] [Render thread/INFO]: Connecting to mc.hypixel.net, 25565
            [12:01:00] [Render thread/INFO]: [System] [CHAT] HOTSPOT! Your Hunting Hotspot is the Forest Biome!
            [12:01:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Local caught a SPARKLING Foxtrot!
            [12:02:00] [Render thread/INFO]: Connecting to alpha.hypixel.net, 25565
            [12:03:00] [Render thread/INFO]: [System] [CHAT] HOTSPOT! Your Hunting Hotspot is the Icy Biome!
            [12:03:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Friend caught a SPARKLING Tepid!
            [12:04:00] [Render thread/INFO]: Connecting to mc.hypixel.net, 25565
            [12:05:00] [Render thread/INFO]: [System] [CHAT] HOTSPOT! Your Hunting Hotspot is the Haunted Biome!
            [12:05:01] [Render thread/INFO]: [System] [CHAT] Party > Friend: Connecting to alpha.hypixel.net, 25565
            [12:05:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Local caught a SPARKLING Gazer!
            [12:06:00] [Render thread/INFO]: [System] [CHAT] SAFARI REWARD SUMMARY
        """.trimIndent()
        val parser = LogImporter.Parser("Local").apply { read(log.reader().buffered(), LocalDate.of(2026,10,5)) }
        assertEquals(listOf("Foxtrot", "Gazer"), parser.sparklings.map { it.critter })
        assertEquals("Tepid", parser.alphaSparklings.single().critter)
        assertEquals(2, parser.runs.size); assertEquals(1, parser.alphaRuns.size)
        assertEquals(60000L, parser.runs.first().length)
        assertEquals(60000L, parser.alphaRuns.single().length)
    }
    @Test fun `alpha footer survives empty messages and unlabelled JSON cannot reintroduce alpha catch`() {
        val day = LocalDate.of(2026,10,5)
        val parser = LogImporter.Parser("Local")
        parser.read("""
            [12:00:00] [Render thread/INFO]: Setting user: Local
            [12:00:01] [Render thread/INFO]: [System] [CHAT] alpha.hypixel.net
            [12:00:02] [Render thread/INFO]: [System] [CHAT] Unrelated message
            [12:00:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Local caught a SPARKLING Tepid!
            [12:01:00] [Render thread/INFO]: [System] [CHAT] www.hypixel.net
            [12:01:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Local caught a SPARKLING Gazer!
        """.trimIndent().reader().buffered(),day)
        val at = day.atTime(12,0,5).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        parser.readJson("""{"owner":"Local","entries":[{"at":$at,"player":"Local","critter":"Tepid"}]}""".reader().buffered(),day)
        assertEquals(listOf("Gazer"),parser.sparklings.map { it.critter })
        assertEquals(listOf("Tepid"),parser.alphaSparklings.map { it.critter })
        val exported = LogImporter.Parser("Local")
        exported.readJson("""{"owner":"Local","server":"alpha.hypixel.net","entries":[{"at":$at,"player":"Local","critter":"Tepid"}]}""".reader().buffered(),day)
        assertTrue(exported.sparklings.isEmpty()); assertEquals(1,exported.alphaSparklings.size)
    }
    @Test fun `repairs alpha and duplicate catches but preserves records without logs and distinct same species catches`() {
        fun spark(at: Long, species: String = "Foxtrot") = SparklingEntry(at, "Local", species)
        val missingLog = spark(1000, "Macaw")
        val first = spark(100000); val duplicate = spark(100300); val second = spark(108000)
        val alpha = spark(200000, "Tepid")
        val saved = mutableListOf(missingLog, first, duplicate, second, alpha)
        val found = listOf(spark(100000), spark(108000))
        assertEquals(2, HistoryImportRepair.sparklings(saved, found, listOf(spark(200000,"Tepid"))))
        assertEquals(listOf(missingLog,first,second), saved)
        assertEquals(0, HistoryImportRepair.sparklings(saved, found, emptyList()))
        assertEquals(0, HistoryImportRepair.sparklings(saved, emptyList(), emptyList()))
    }
    @Test fun `run repair keeps native detail missing logs and nearby separate runs`() {
        val native = RunRecord(start=100000, ticketAt=99000, caught=25)
        val duplicate = RunRecord(start=100000, imported=true)
        val next = RunRecord(start=160000, imported=true)
        val missing = RunRecord(start=1000)
        val alpha = RunRecord(start=300000)
        val saved = mutableListOf(duplicate,native,next,missing,alpha)
        val found = listOf(RunRecord(start=100000),RunRecord(start=160000))
        assertEquals(2, HistoryImportRepair.runs(saved,found,listOf(RunRecord(start=300000))))
        assertEquals(listOf(native,next,missing),saved)
        assertEquals(25,native.caught)
        assertEquals(0,HistoryImportRepair.runs(saved,found,emptyList()))
    }
}
