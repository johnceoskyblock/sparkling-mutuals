package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SafariStructuresTest {
    @TempDir lateinit var dir: Path
    @Test fun `mounds require a centered low empty hitbox within 64 blocks`() {
        val mound = MoundShape(-100.5, 40.0, 30.5, .7, .5, 64.0 * 64)
        assertTrue(SafariMounds.detected(mound))
        assertTrue(SafariMounds.detected(mound.copy(width = .35, height = .25, y = 65.0)))
        assertTrue(SafariMounds.detected(mound.copy(width = 1.10, height = .95)))
        assertTrue(SafariMounds.detected(mound.copy(width = .5, height = .6)))
        assertFalse(SafariMounds.detected(mound.copy(width = .5, height = .6001)))
        assertFalse(SafariMounds.detected(mound.copy(width = .35, height = .95)))
        for (other in listOf(mound.copy(distanceSquared = 4096.01), mound.copy(y = 65.01),
            mound.copy(width = .34), mound.copy(width = 1.11), mound.copy(height = .24),
            mound.copy(height = .96), mound.copy(x = -100.2), mound.copy(z = 30.8),
            mound.copy(occupied = true))) assertFalse(SafariMounds.detected(other), other.toString())
    }
    @Test fun `only actual mound outcomes count independently of critter captures`() {
        val run = SafariRun(1000)
        assertTrue(run.recordMound("§7The mound falls apart, but nothing is inside..."))
        assertTrue(run.recordMound("The mound fell apart, revealing a Rockmite hidden inside!"))
        assertFalse(run.recordMound("The Spider Mound falls apart!"))
        assertFalse(run.recordMound("The mound fell apart, revealing a fish!"))
        assertFalse(run.recordMound("Party > Player: The mound fell apart, revealing a Rockmite hidden inside!"))
        assertEquals(2, run.brokenMounds)
        assertEquals(1, run.rockmiteMounds)
        assertEquals(0, run.count("Rockmite"))
        run.record(SafariCatch.parse("CAPTURE! You caught a Rockmite!")!!)
        assertEquals(1, run.count("Rockmite"))
        assertEquals(2, run.brokenMounds)
    }
    @Test fun `mound results continue while hidden survive departure and reset only on next run`() {
        ConfigManager.init(dir)
        val ledger = SafariLedger()
        ledger.arrive(1000)
        assertFalse(ConfigManager.showMoundStats)
        val run = ledger.current!!
        SafariMessages.lines("The mound fell apart, revealing a Rockmite hidden inside!").forEach(run::recordMound)
        assertFalse(SafariPanels.captures(run, SafariBiome.CAVERN).rows.any { it.label == "Rockmite Mounds" })
        ConfigManager.showMoundStats = true
        assertEquals("1", SafariPanels.captures(run, SafariBiome.CAVERN).rows.first { it.label == "Rockmite Mounds" }.value)
        assertFalse(SafariPanels.captures(run, SafariBiome.FOREST).rows.any { it.label == "Rockmite Mounds" })
        ledger.update(SafariLocation.INSIDE, null, 2000)
        ledger.leave(3000)
        assertEquals(1, ledger.displayed!!.brokenMounds)
        assertEquals(1, ledger.displayed!!.rockmiteMounds)
        ledger.arrive(4000)
        assertEquals(0, ledger.current!!.brokenMounds)
        assertEquals(0, ledger.current!!.rockmiteMounds)
    }
}
