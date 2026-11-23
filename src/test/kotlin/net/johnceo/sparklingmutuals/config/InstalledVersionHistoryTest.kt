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

    @Test fun `only releases since the previous install appear even when earlier updates are unread`() {
        fun launch(history: InstalledVersionHistory, version: String) = history.launch(version, newConfig = false) { error("already tracked") }
        val fresh = InstalledVersionHistory().launch("4.3.7+mc26.3", newConfig = true) { null }
        assertNull(ReleaseHighlights.pending(fresh))
        val intermediate = launch(fresh, "4.4.0+mc26.3")
        val updated = launch(intermediate, "v4.4.2+mc26.1.2")
        assertEquals("4.4.0", updated.previous)
        assertEquals("4.3.7", updated.highlightsSeenThrough)
        assertEquals(listOf("4.4.2", "4.4.1"), ReleaseHighlights.pending(updated)?.releases?.map { it.version })
        assertEquals("4.4.0", ReleaseHighlights.pending(updated)?.through)
        assertEquals(updated, launch(updated, "4.4.2+mc26.2"))
        assertEquals(updated, updated.acknowledge("4.4.0"))
        val read = updated.acknowledge("4.4.2")
        assertNull(ReleaseHighlights.pending(read))
        assertEquals(ReleaseHighlights.pending(updated), ReleaseHighlights.forCurrentInstall(read))
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
        val prefix = "updates.${ReleaseTrack.current.id}."
        assertEquals("false", props.getProperty("shinyDetection"))
        assertEquals("4.4.1", props.getProperty(prefix + "installedVersion"))
        assertEquals("4.3.9", props.getProperty(prefix + "previousVersion"))
        assertEquals("4.4.1", props.getProperty(prefix + "highlightsSeenThrough"))
        assertNull(ReleaseHighlights.pending(ConfigManager.installedVersionHistory))
        assertEquals(pending, ReleaseHighlights.forCurrentInstall(ConfigManager.installedVersionHistory))

        ConfigManager.init(directory, "4.4.2+mc26.1.2") { error("should not scan again") }
        val next = ReleaseHighlights.forCurrentInstall(ConfigManager.installedVersionHistory)
        assertEquals("4.4.1", next.through)
        assertEquals("4.4.2", next.installed)
        assertEquals(listOf("4.4.2"), next.releases.map { it.version })
        assertEquals(next, ReleaseHighlights.pending(ConfigManager.installedVersionHistory))

        ConfigManager.acknowledgeReleaseHighlights("4.4.2")
        ConfigManager.init(directory, "4.4.2+mc26.3") { error("should not scan again") }
        assertEquals(next, ReleaseHighlights.forCurrentInstall(ConfigManager.installedVersionHistory))
        assertNull(ReleaseHighlights.pending(ConfigManager.installedVersionHistory))
    }

    @Test fun `each edition shows only its own highlights and shares the version range rules`() {
        val history = InstalledVersionHistory(installed = "4.4.3", previous = "4.1.1")
        fun titles(track: ReleaseTrack) = ReleaseHighlights.forCurrentInstall(history, track = track)
            .releases.flatMap { it.features }.map { it.title }
        val standard = titles(ReleaseTrack.STANDARD)
        val full = titles(ReleaseTrack.FULL)
        val noUpdater = titles(ReleaseTrack.FULL_NO_AUTO_UPDATE)
        assertTrue("Safari Splits HUD" in standard && "Safari Splits HUD" in full)
        assertTrue("Standard Safari visuals" in standard && "Standard Safari visuals" !in full)
        assertTrue("Critter nameplates" !in standard && "Critter nameplates" in full)
        assertTrue("Floor-drop visibility" !in standard && "Safari chat filters" in standard)
        assertEquals(full.filterNot { it == "In-game update downloads" }, noUpdater)
        assertTrue("In-game update downloads" in full)

        val fullOnly = InstalledVersionHistory(installed = "3.4.0")
        assertNull(ReleaseHighlights.pending(fullOnly, track = ReleaseTrack.STANDARD))
        assertTrue(ReleaseHighlights.pending(fullOnly, track = ReleaseTrack.FULL) != null)
    }

    @Test fun `saving and resetting keep another edition's settings and version history`() {
        val otherTrack = if (ReleaseTrack.current == ReleaseTrack.STANDARD) ReleaseTrack.FULL else ReleaseTrack.STANDARD
        val path = directory.resolve("sparkling-mutuals.properties")
        val otherHistory = InstalledVersionHistory("4.4.3", "4.4.0", "4.4.3")
        val otherSetting = if (otherTrack == ReleaseTrack.STANDARD) SafariEspConfig.STANDARD_SCRAPPY_COLOR else "esp.scrappy.fish.color"
        val custom = "0:255:1:23:45"
        val initial = Properties().apply {
            otherHistory.save(this, otherTrack)
            setProperty(otherSetting, custom)
        }
        Files.newOutputStream(path).use { initial.store(it, null) }
        ConfigManager.init(directory, "4.4.3")
        ContestConfig.init(directory)
        assertNull(ConfigManager.installedVersionHistory.previous)
        assertTrue(ReleaseHighlights.pending(ConfigManager.installedVersionHistory) != null)
        ConfigManager.acknowledgeReleaseHighlights("4.4.3")

        fun assertPreserved() {
            val saved = Properties().apply { Files.newInputStream(path).use(::load) }
            assertEquals(otherHistory, InstalledVersionHistory.load(saved, otherTrack))
            assertEquals(custom, saved.getProperty(otherSetting))
            assertNull(ReleaseHighlights.pending(InstalledVersionHistory.load(saved)))
        }
        ConfigManager.saveSettings()
        assertPreserved()
        ConfigManager.resetSettings()
        assertPreserved()
    }

    @Test fun `legacy installations without recoverable history only get the current release`() {
        val legacy = InstalledVersionHistory().launch("4.4.1", newConfig = false) { null }
        assertNull(legacy.previous)
        val pending = ReleaseHighlights.pending(legacy)
        assertNull(pending?.through)
        assertEquals(listOf("4.4.1"), pending?.releases?.map { it.version })
        val patch = InstalledVersionHistory().launch("4.3.6", newConfig = false) { null }
        assertNull(ReleaseHighlights.pending(patch))
        assertTrue(ReleaseHighlights.forCurrentInstall(patch).releases.isEmpty())
    }

    @Test fun `manual highlights for a fresh install show the installed release`() {
        val fresh = InstalledVersionHistory().launch("v4.4.1+mc26.1.2", newConfig = true) { null }
        val highlights = ReleaseHighlights.forCurrentInstall(fresh)
        assertEquals("4.4.1", highlights.installed)
        assertEquals(listOf("4.4.1"), highlights.releases.map { it.version })
        assertNull(highlights.through)
        assertNull(ReleaseHighlights.pending(fresh))
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
