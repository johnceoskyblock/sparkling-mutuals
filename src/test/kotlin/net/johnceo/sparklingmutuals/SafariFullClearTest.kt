package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.*
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariFullClearTest {
    @TempDir lateinit var dir: Path
    @Test fun `full clear presets persist without erasing the ledger`() {
        ConfigManager.init(dir)
        val run = SafariRun(10)
        run.record(SafariCatch.parse("CAPTURE! You caught a Foxtrot!")!!)
        assertTrue(SafariFullClear.toggle())
        assertTrue(ConfigManager.catchCountPanel); assertTrue(ConfigManager.showMoundStats)
        assertFalse(ConfigManager.countUniqueOnly)
        assertTrue(SafariEspConfig.mobs.values.all { it.enabled })
        assertTrue(SafariEspConfig.rockmiteMoundEnabled)
        assertTrue(SafariEspConfig.groups.getValue("floor").enabled)
        assertTrue(ConfigManager.showBeeNests && ConfigManager.showSnooperWalls && ConfigManager.showMoundCount)
        ConfigManager.init(dir); assertTrue(ConfigManager.fullClearMode)
        assertFalse(SafariFullClear.toggle())
        assertFalse(SafariEspConfig.mobs.getValue("Rockmite").enabled)
        assertTrue(SafariEspConfig.rockmiteMoundEnabled)
        assertTrue(SafariEspConfig.mobs.getValue("Driftling").enabled)
        assertTrue(SafariEspConfig.mobs.getValue("Foxtrot").enabled)
        assertFalse(SafariEspConfig.mobs.getValue("Gemzie").enabled)
        assertFalse(SafariEspConfig.mobs.getValue("Gazer").enabled)
        assertTrue(SafariEspConfig.groups.getValue("floor").enabled)
        assertEquals(1, run.count("Foxtrot"))
        assertFalse(ConfigManager.catchCountPanel); assertFalse(ConfigManager.showMoundStats)
        ConfigManager.init(dir); assertFalse(ConfigManager.fullClearMode)
    }
    @Test fun `full clear numbers turn green at the user minimum and Scrappy remains missing until three`() {
        ConfigManager.init(dir); SafariFullClear.toggle()
        val run = SafariRun(0)
        repeat(2) { run.record(SafariCatch.parse("CAPTURE! You caught a Scrappy!")!!) }
        val missing = SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.first { it.label == "Scrappy" }
        assertEquals("2/3", missing.value)
        var row = SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Scrappy" }
        assertEquals("2", row.value); assertEquals(0xFFFF5555.toInt(), row.valueColor)
        run.record(SafariCatch.parse("CAPTURE! You caught a Scrappy!")!!)
        run.updateCaptureEvidence(SafariBiome.CAVERN, emptySet(), false)
        row = SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Scrappy" }
        assertEquals(0xFF55FF55.toInt(), row.valueColor)
        assertFalse(SafariPanels.missing(run, SafariBiome.CAVERN, false, 0).rows.any { it.label == "Scrappy" })
        assertEquals(3, run.count("Scrappy"))
    }
    @Test fun `personal captures remain separate from the combined GUI counts`() {
        val run = SafariRun(0)
        run.record(SafariCatch.parse("LOOT SHARE! You received a Wumpa Shard from Friend catching a Wumpa!")!!)
        assertEquals(1, run.count("Wumpa")); assertEquals(0, run.personalCount("Wumpa"))
        run.record(SafariCatch.parse("CAPTURE! You caught a Wumpa!")!!)
        assertEquals(2, run.count("Wumpa")); assertEquals(1, run.personalCount("Wumpa"))
        run.record(SafariCatch.parse("CAPTURE! Friend caught a Wumpa!")!!)
        assertEquals(3, run.count("Wumpa")); assertEquals(1, run.personalCount("Wumpa"))
    }
}
