package net.johnceo.sparklingmutuals.history.achievements

import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AchievementCalibrationTest {
    @Test fun `workbook exposure covers exactly the canonical roster`() {
        assertEquals(SafariRoster.all.map { it.name }.toSet(), AchievementCalibration.rates.keys)
        assertEquals(37, AchievementCalibration.rates.size)
        assertEquals(144.994135774, AchievementCalibration.rates.values.sum(), 1e-6)
        assertEquals(6.0, AchievementCalibration.rates.getValue("Strongarm"))
        assertEquals(4.5, AchievementCalibration.rates.getValue("Mantis Shrimp"))
    }
    @Test fun `dry tails use exposure rather than run count`() {
        assertEquals(2047.0 / 2048, AchievementCalibration.q)
        assertEquals(0.4925535559, AchievementCalibration.dryProbability(AchievementCalibration.rates.values.sum() * 10), 1e-9)
        assertEquals(0.8850597479, AchievementCalibration.dryProbability(250.0), 1e-9)
        assertEquals(0.0, AchievementCalibration.hazard(0.0))
        assertEquals(1.0, AchievementCalibration.dryProbability(0.0))
        for (invalid in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { AchievementCalibration.hazard(invalid) }
            assertThrows(IllegalArgumentException::class.java) { AchievementCalibration.dryProbability(invalid) }
        }
    }
    @Test fun `inclusive survival sums reproduce workbook moments`() {
        val (mean, sd) = AchievementCalibration.completionMoments(20000)
        assertEquals(4365.831526956, mean, .001)
        assertEquals(2152.067616569, sd, .001)
    }
    @Test fun `completion quantiles are first whole run crossings`() {
        for ((p, run) in listOf(.5 to 3839, .75 to 5304, .9 to 7164, .95 to 8559, .99 to 11798, .999 to 16457)) {
            assertEquals(run, AchievementCalibration.firstCompletionQuantile(p))
            assertTrue(AchievementCalibration.completionProbability(run - 1) < p)
            assertTrue(AchievementCalibration.completionProbability(run) >= p)
        }
        assertEquals(0.0, AchievementCalibration.completionProbability(0))
    }
    @Test fun `expected count crossings differ from full completion quantiles`() {
        for ((count, run) in listOf(20 to 441, 30 to 1056)) {
            assertTrue(AchievementCalibration.expectedDistinct(run - 1) < count)
            assertTrue(AchievementCalibration.expectedDistinct(run) >= count)
        }
        assertEquals(0.0, AchievementCalibration.expectedDistinct(0))
    }
    @Test fun `drought threshold awards only on whole run crossing`() {
        val perRun = AchievementCalibration.hazard(AchievementCalibration.rates.values.sum())
        for ((k, run) in listOf(250 to 72, 500 to 143, 1000 to 286)) {
            val target = AchievementCalibration.droughtThreshold(k)
            assertTrue(perRun * (run - 1) < target)
            assertTrue(perRun * run >= target)
        }
    }
}
