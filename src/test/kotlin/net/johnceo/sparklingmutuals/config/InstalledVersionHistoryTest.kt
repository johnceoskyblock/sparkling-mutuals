package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.GZIPOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class InstalledVersionHistoryTest {
    @TempDir lateinit var directory: Path

    @Test fun `multiple upgrades accumulate until the highlights are read`() {
        fun launch(history: InstalledVersionHistory, version: String) = history.launch(version, newConfig = false) { error("already tracked") }
        val fresh = InstalledVersionHistory().launch("4.3.7+mc26.3", newConfig = true) { null }
        assertNull(ReleaseHighlights.pending(fresh))
        val intermediate = launch(fresh, "4.4.0+mc26.3")
        val updated = launch(intermediate, "v4.4.2+mc26.1.2")
        assertEquals("4.4.0", updated.previous)
        assertEquals("4.3.7", updated.highlightsSeenThrough)
        assertEquals(listOf("4.4.2", "4.4.1", "4.4.0", "4.3.9", "4.3.8"),
            ReleaseHighlights.pending(updated)?.releases?.map { it.version })
        assertEquals(updated, launch(updated, "4.4.2+mc26.2"))
        assertEquals(updated, updated.acknowledge("4.4.0"))
        val read = updated.acknowledge("4.4.2")
        assertNull(ReleaseHighlights.pending(read))
        assertNull(ReleaseHighlights.pending(launch(launch(read, "4.4.1"), "4.4.2")))
    }

    @Test fun `established config saves the previous version and acknowledgement across reloads`() {
        val path = directory.resolve("sparkling-mutuals.properties")
        Files.writeString(path, "shinyDetection=false\n")
        ConfigManager.init(directory, "4.4.1") { "4.3.9+mc26.3" }
        val pending = ReleaseHighlights.pending(ConfigManager.installedVersionHistory)
        assertEquals(listOf("4.4.1", "4.4.0"), pending?.releases?.map { it.version })
        ConfigManager.acknowledgeReleaseHighlights("4.4.1")
        ConfigManager.save()
        ConfigManager.init(directory, "4.4.1+mc26.1.2") { error("should not scan again") }
        val props = Properties().apply { Files.newInputStream(path).use(::load) }
        assertEquals("false", props.getProperty("shinyDetection"))
        assertEquals("4.4.1", props.getProperty("updates.installedVersion"))
        assertEquals("4.3.9", props.getProperty("updates.previousVersion"))
        assertEquals("4.4.1", props.getProperty("updates.highlightsSeenThrough"))
        assertNull(ReleaseHighlights.pending(ConfigManager.installedVersionHistory))
    }

    @Test fun `legacy installations without recoverable history only get the current release`() {
        val legacy = InstalledVersionHistory().launch("4.4.1", newConfig = false) { null }
        assertNull(legacy.previous)
        val pending = ReleaseHighlights.pending(legacy)
        assertNull(pending?.through)
        assertEquals(listOf("4.4.1"), pending?.releases?.map { it.version })
        val patch = InstalledVersionHistory().launch("4.3.6", newConfig = false) { null }
        assertNull(ReleaseHighlights.pending(patch))
    }

    @Test fun `log recovery uses newest numeric archive and excludes the active log`() {
        val row = "\t- sparkling-mutuals 4.3.9+mc26.3\n"
        Files.writeString(directory.resolve("latest.log"), "\t- sparkling-mutuals 4.4.2\n")
        Files.writeString(directory.resolve("2026-11-21-9.log"), "\t- sparkling-mutuals 4.3.8\n")
        Files.writeString(directory.resolve("2026-11-21-11.log.gz"), "broken gzip")
        val newest = directory.resolve("2026-11-21-10.log.gz")
        GZIPOutputStream(Files.newOutputStream(newest)).bufferedWriter().use { it.write(row) }
        assertEquals("4.3.9", PreviousModVersion.fromLogs(directory))
        val recovered = InstalledVersionHistory().launch("4.4.2", newConfig = true) { PreviousModVersion.fromLogs(directory) }
        assertEquals("4.3.9", recovered.previous)
        assertTrue(ReleaseHighlights.pending(recovered)?.releases?.none { it.version == "4.3.9" } == true)
    }
}
