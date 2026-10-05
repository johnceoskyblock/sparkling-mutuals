package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.contest.MiriaContestFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MiriaContestFilterTest {
    @Test fun `Miria rows exclude Agatha when both are visible`() {
        assertEquals(listOf("UNCOMMON: 400", "Time Left: 4:00"), MiriaContestFilter.miriaLines(listOf(
            "Agatha's Contest", "RARE: 1000", "Time Left: 8:00", "§aMiria’s Contest:", "§bUNCOMMON: 400", "Time Left: 4:00")))
        assertEquals(emptyList<String>(), MiriaContestFilter.miriaLines(listOf("Agatha's Contest", "UNCOMMON: 400")))
    }
    @Test fun `truncated Miria section stops at another contest header`() {
        assertEquals(listOf("Time Left: 4:00"), MiriaContestFilter.miriaLines(listOf("Miria's Contest", "Time Left: 4:00", "Agatha's Contest", "RARE: 1000")))
    }
}
