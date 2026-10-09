package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Properties

class CavernStatusTest {
    @TempDir lateinit var dir: Path
    private val broken = WallSummary(List(5) { WallState.BROKEN })
    @Test fun `cleared walls show a gray absence message until a Snoozle is caught`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        val rows = SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, walls = broken).rows
        assertEquals(HudRow.GRAY, rows.first { it.label == "No snoozles this run" }.color)
        assertFalse(rows.any { it.label.startsWith("Snooper walls") })
        run.record(SafariCatch(SafariRoster.named("Snoozle")!!))
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, walls = broken).rows.any {
            it.label.contains("snoozles", true) || it.label.startsWith("Snooper walls")
        })
    }
    @Test fun `surveyed empty mounds show absence but a revealed Rockmite suppresses it`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        repeat(9) { run.recordMound("The mound falls apart, but nothing is inside...") }
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "No rockmites this run" })
        run.recordMound("The mound falls apart, but nothing is inside...")
        run.moundSurvey.scan(emptySet()) { true }
        assertEquals(HudRow.GRAY, SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.first {
            it.label == "No rockmites this run"
        }.color)
        run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!")
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "No rockmites this run" })
    }
    @Test fun `Rockmite form colors save separately`() {
        val input = Properties().apply {
            setProperty("esp.rockmite.color", "0:255:255:0:0")
            setProperty("esp.rockmite.moundColor", "0:255:0:0:255")
            setProperty("esp.rockmite.enabled", "false")
        }
        SafariEspConfig.load(input)
        val saved = Properties().also(SafariEspConfig::save)
        assertEquals("0:255:255:0:0", saved.getProperty("esp.rockmite.color"))
        assertEquals("0:255:0:0:255", saved.getProperty("esp.rockmite.moundColor"))
        assertEquals("false", saved.getProperty("esp.rockmite.enabled"))
    }
    @Test fun `sightings suppress absence without adding captures and reset on next run`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        repeat(20) { run.recordMound("The mound falls apart, but nothing is inside...") }
        run.observe(EspEntity("sniffer")); run.observe(EspEntity("silverfish"))
        assertEquals(0, run.count("Snoozle")); assertEquals(0, run.count("Rockmite"))
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, walls = broken).rows.any {
            it.label.startsWith("No ") || it.label.startsWith("Snooper walls")
        })
        val next = SafariRun(1000)
        next.observe(EspEntity("silverfish", invisible = true))
        next.observe(EspEntity("silverfish", passengers = true))
        assertFalse(next.encountered("Rockmite")); assertFalse(next.encountered("Snoozle"))
        assertTrue(SafariPanels.missing(next, SafariBiome.CAVERN, false, 0, walls = broken).rows.any { it.label == "No snoozles this run" })
    }
    @Test fun `empty completed scan permits mid-run arrival but unloaded known sites block completion`() {
        val survey = MoundSurvey()
        assertFalse(survey.allBroken)
        survey.scan(emptySet()) { true }; assertTrue(survey.allBroken)
        val sites = (0 until 20).map { Triple(it, 40, 30) }.toSet()
        survey.scan(sites) { true }
        survey.scan(emptySet()) { false }; assertFalse(survey.allBroken)
        survey.scan(emptySet()) { it.first < 19 }; assertFalse(survey.allBroken)
        survey.scan(emptySet()) { true }; assertTrue(survey.allBroken)
        survey.scan(setOf(Triple(19, 40, 30))) { true }; assertFalse(survey.allBroken)
    }
    @Test fun `form colors inherit old settings and customize independently without enabling ESP`() {
        ConfigManager.init(dir)
        SafariEspConfig.load(Properties().apply { setProperty("esp.rockmite.color", "0:255:18:52:86") })
        assertEquals("0:255:18:52:86", SafariEspConfig.entityColor("Rockmite", true))
        SafariEspConfig.mobs.getValue("Rockmite").enabled = false
        val settings = CustomizationSettings()
        settings.selectedHex = "#112233"; assertTrue(settings.rockmiteMound.setSelected())
        settings.selectedHex = "#AABBCC"; assertTrue(settings.rockmite.setSelected())
        settings.apply(); assertFalse(SafariEspConfig.mobs.getValue("Rockmite").enabled)
        ConfigManager.save(); ConfigManager.init(dir)
        assertEquals(0xFF112233.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Rockmite", true)))
        assertEquals(0xFFAABBCC.toInt(), SafariEspConfig.rgb(SafariEspConfig.entityColor("Rockmite")))
        assertTrue(SafariEspConfig.mobs.getValue("Rockmite").enabled) // Modes own visibility after reload.
    }
    @Test fun `unknown walls stay unverified while modes own absence messages`() {
        ConfigManager.init(dir)
        val run = SafariRun(0)
        val unknown = WallSummary(List(4) { WallState.BROKEN } + WallState.UNKNOWN)
        assertEquals("0 (+1?)", SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, walls = unknown).rows.first {
            it.label == "Snooper walls to break"
        }.value)
        repeat(20) { run.recordMound("The mound falls apart, but nothing is inside...") }
        ConfigManager.showMoundCount = false; ConfigManager.showSnooperWalls = false
        assertTrue(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0, walls = broken).rows.any { it.label == "No snoozles this run" })
        assertFalse(SafariPanels.missing(run, SafariBiome.ICY, false, 0, walls = broken).rows.any { it.label.startsWith("No ") })
    }
}
