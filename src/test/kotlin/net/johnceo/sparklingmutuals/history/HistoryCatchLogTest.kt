package net.johnceo.sparklingmutuals.history

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.GZIPOutputStream
import net.johnceo.sparklingmutuals.history.importing.LogImporter
import net.johnceo.sparklingmutuals.history.importing.SparklingHistoryMerge
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HistoryCatchLogTest {
    @TempDir lateinit var dir: Path
    private val day = LocalDate.of(2026,10,5)
    private val time get() = day.atTime(12,1,5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun log(owner: String, host: String = "mc.hypixel.net") = """
        [12:00:00] [Render thread/INFO]: Setting user: $owner
        [12:00:01] [Render thread/INFO]: Connecting to $host, 25565
        [12:01:04] [Render thread/INFO]: [System] [CHAT] Party > Local: SPARKLING! Local caught a SPARKLING Foxtrot!
        [12:01:05] [Render thread/INFO]: [System] [CHAT] SPARKLING! Local caught a SPARKLING Foxtrot!
        [12:01:06] [Render thread/INFO]: [System] [CHAT] Another line
    """.trimIndent()

    @Test fun `legacy entries locate gzip original line ignoring foreign alpha and pasted announcements`() {
        val foreign = dir.resolve("2026-10-05-1.log")
        Files.writeString(foreign,log("Other"))
        Files.writeString(dir.resolve("2026-10-05-2.log"),log("Local","alpha.hypixel.net"))
        val archive = dir.resolve("2026-10-05-3.log.gz")
        GZIPOutputStream(Files.newOutputStream(archive)).bufferedWriter().use { it.write(log("Local")) }
        val entry = SparklingEntry(at=time+750,player="Local",critter="Foxtrot",logFile=foreign.toString(),logLine=4)
        val found = LogImporter.findCatch(entry,"Local",dir).join()!!
        assertEquals(archive,found.path)
        assertEquals(3,found.line)
        assertTrue(found.lines[found.line].endsWith("[CHAT] SPARKLING! Local caught a SPARKLING Foxtrot!"))
        assertEquals(3,Files.list(dir).use { it.count() }) // No extracted copy.
        assertNull(LogImporter.findCatch(SparklingEntry(at=time,player="Local",critter="Macaw"),"Local",dir).join())
    }
    @Test fun `imports retain source and update matching saved entries without adding a duplicate`() {
        val file = dir.resolve("2026-10-05-1.log")
        Files.writeString(file,log("Local"))
        val parser = LogImporter.Parser("Local")
        Files.newBufferedReader(file).use { parser.read(it,day,source=file.toString()) }
        val found = parser.sparklings.single()
        assertEquals(file.toString(),found.logFile); assertEquals(4,found.logLine)
        val saved = SparklingEntry(at=time+500,player="Local",critter="Foxtrot",unique=true)
        val entries = mutableListOf(saved)
        assertEquals(0,SparklingHistoryMerge.merge(entries,listOf(found),emptyList()))
        assertEquals(file.toString(),saved.logFile); assertEquals(4,saved.logLine)
        assertTrue(saved.unique)
        assertEquals(0,SparklingHistoryMerge.merge(entries,listOf(found),emptyList()))
    }
}
