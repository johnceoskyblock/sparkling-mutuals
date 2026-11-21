package net.johnceo.sparklingmutuals.commands

import java.time.Instant
import java.time.ZoneId
import net.johnceo.sparklingmutuals.history.ProfitPeriods
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.history.SafariProfitPeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SafariProfitReplyTest {
    @Test fun `profit commands use each responding player's own history`() {
        for (period in SafariProfitPeriod.entries) {
            val command = PartyCommand.fromChat("Party > Runner: !PROFIT ${period.name}")!!.command
            assertEquals(period, command.profitPeriod)
            assertTrue(command.personalResult)
            assertFalse(command.requiresApiKey)
            assertEquals("Teammate", command.forResponder("Teammate").ign)
        }
        assertEquals(SafariProfitPeriod.SESSION, PartyCommand.fromChat("Party > Runner: !profit")?.command?.profitPeriod)
        assertNull(PartyCommand.fromChat("Party > Runner: !profit month"))
    }

    @Test fun `history periods align with the local calendar and current session`() {
        fun stamp(value: String) = Instant.parse(value).toEpochMilli()
        val now = stamp("2026-11-18T05:30:00Z")
        val zone = ZoneId.of("America/New_York")
        val runs = listOf("2026-11-16T04:59:59Z", "2026-11-16T05:00:00Z", "2026-11-18T04:59:59Z", "2026-11-18T05:00:00Z")
            .map { RunRecord(start = stamp(it)) }
        fun select(period: SafariProfitPeriod, sessionStart: Long? = runs[2].start) =
            ProfitPeriods.currentRuns(runs, period, sessionStart, now, zone)
        assertEquals(runs, select(SafariProfitPeriod.LIFETIME))
        assertEquals(runs, select(SafariProfitPeriod.SESSION, null))
        assertEquals(runs.drop(2), select(SafariProfitPeriod.SESSION))
        assertEquals(runs.drop(1), select(SafariProfitPeriod.WEEK))
        assertEquals(runs.takeLast(1), select(SafariProfitPeriod.DAY))
    }

    @Test fun `reply labels its pricing basis and handles pending or absent estimates`() {
        val day = SafariProfitPeriod.DAY
        val runs = listOf(RunRecord(length = 1_800_000, coins = 2_000_000), RunRecord())
        assertEquals("[SM] Safari profit (Day, Insta-sell): 2.00M coins | 4.00M coins/h | 1/2 runs",
            SafariProfitReply.format(day, runs, instaSell = true))
        assertEquals("[SM] Safari profit (Day, Sell offer): Bazaar prices unavailable.",
            SafariProfitReply.format(day, runs, instaSell = false, priceChangePending = true))
        assertEquals("[SM] Safari profit (Day, Sell offer): No recorded runs.",
            SafariProfitReply.format(day, emptyList(), instaSell = false))
    }
}
