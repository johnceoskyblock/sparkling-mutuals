package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SafariSplitsTest {
    @Test fun `split precision changes only the displayed fraction`() {
        assertEquals("1:48.159", SplitTimeFormat.MILLISECONDS.format(108_159L))
        assertEquals("1:48.15", SplitTimeFormat.HUNDREDTHS.format(108_159L))
        assertEquals("1:48.1", SplitTimeFormat.TENTHS.format(108_159L))
        assertEquals("1:48", SplitTimeFormat.SECONDS.format(108_159L))
        assertEquals("0:03.007", SplitTimeFormat.MILLISECONDS.format(3_007L))
        assertEquals("0:59.9", SplitTimeFormat.TENTHS.format(59_999L))
        assertEquals("1:00", SplitTimeFormat.SECONDS.format(60_000L))
    }

    private fun catch(run: SafariRun, critter: SafariCritter, times: Int = 1) {
        repeat(times) { run.record(SafariCatch(critter), now = 50_000) }
    }

    @Test fun `unique reports excuse only Macaw and the two cavern species`() {
        val run = SafariRun(1_000)
        SafariBiome.entries.forEach { biome ->
            val missing = when (biome) {
                SafariBiome.FOREST -> setOf("Macaw", "Bluebird")
                SafariBiome.CAVERN -> setOf("Snoozle", "Rockmite")
                SafariBiome.ICY -> setOf("Wumpa")
                SafariBiome.HAUNTED -> setOf("Doomspiral")
            }
            biome.critters.filterNot { it.name in missing }.forEach { catch(run, it) }
        }
        run.splits.observe(run, 54_000)
        assertNull(run.splits.time(SafariMode.UNIQUE, SafariBiome.CAVERN))
        run.manualBiomeReport("Party > Runner: fd")
        SafariBiome.entries.filterNot { it == SafariBiome.FOREST }.forEach(run::sharedBiomeReport)
        run.splits.observe(run, 55_000)
        assertNull(run.splits.time(SafariMode.UNIQUE, SafariBiome.FOREST))
        assertNull(run.splits.time(SafariMode.UNIQUE, SafariBiome.ICY))
        assertNull(run.splits.time(SafariMode.UNIQUE, SafariBiome.HAUNTED))
        assertEquals(54_000L, run.splits.time(SafariMode.UNIQUE, SafariBiome.CAVERN))
        listOf("Bluebird", "Wumpa", "Doomspiral").forEach { catch(run, SafariRoster.named(it)!!) }
        run.splits.observe(run, 56_000)
        assertEquals(55_000L, run.splits.time(SafariMode.UNIQUE, SafariBiome.FOREST))
        assertEquals(55_000L, run.splits.time(SafariMode.UNIQUE, SafariBiome.ICY))
        assertEquals(55_000L, run.splits.time(SafariMode.UNIQUE, SafariBiome.HAUNTED))
        assertEquals(54_000L, run.splits.time(SafariMode.UNIQUE, SafariBiome.CAVERN))
        assertNull(run.splits.time(SafariMode.SPARKLING, SafariBiome.CAVERN))
    }

    @Test fun `full clear waits for both a report and capture minima`() {
        val run = SafariRun(1_000)
        val biome = SafariBiome.ICY
        biome.critters.forEach { catch(run, it, SafariFullClear.minimum(it.name) - if (it.name == "Troodon") 1 else 0) }
        run.splits.observe(run, 56_000)
        assertEquals(55_000L, run.splits.time(SafariMode.UNIQUE, biome))
        assertNull(run.splits.time(SafariMode.FULL_CLEAR, biome))
        run.backendBiomeReportSent(biome)
        run.splits.observe(run, 57_000)
        assertNull(run.splits.time(SafariMode.FULL_CLEAR, biome))
        catch(run, SafariRoster.named("Troodon")!!)
        run.splits.observe(run, 58_000)
        assertEquals(57_000L, run.splits.time(SafariMode.FULL_CLEAR, biome))
    }
}
