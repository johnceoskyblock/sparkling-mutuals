package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.commands.SafariSinceReply
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparklingUniquenessTest {
    @Test fun `live uniqueness follows local discoveries and any earlier participating catcher`() {
        val catch = SparklingEntry(3000, "Friend", "Polaris")
        assertTrue(SparklingUniqueness.isNew(catch, listOf(catch), false))
        assertFalse(SparklingUniqueness.isNew(catch, listOf(catch), true))
        assertFalse(SparklingUniqueness.isNew(catch, listOf(catch), null))
        assertFalse(SparklingUniqueness.isNew(catch, listOf(SparklingEntry(1000, "Another", "Polaris"), catch), false))
    }

    @Test fun `legacy catcher-based repeat cannot supersede Polaris in the unique streak`() {
        val earlier = SparklingEntry(1500, "Me", "Litterbug", unique = false)
        val polaris = SparklingEntry(3500, "Friend", "Polaris", unique = true)
        val repeat = SparklingEntry(5500, "NewPlayer", "Litterbug", unique = true)
        val entries = listOf(repeat, polaris, earlier)
        val runs = listOf(1000L, 3000L, 5000L, 7000L).map { RunRecord(start = it, length = 1000) }
        assertTrue(SparklingUniqueness.reconcile(entries))
        assertFalse(earlier.unique) // A verified pre-owned first saved catch is not promoted.
        assertFalse(repeat.unique)
        assertTrue(polaris.unique)
        assertEquals("2 runs since last unique shiny: Polaris caught by Friend.",
            SafariSinceReply.format("Me", true, runs, entries))
        assertFalse(SparklingUniqueness.reconcile(entries))
    }
}
