package net.johnceo.sparklingmutuals.skyblock.market

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardHistoryParsingTest {
    private val now=Instant.parse("2026-11-03T12:05:00Z").toEpochMilli()
    private fun point(time: String, price: String="100")="""{"timestamp":"$time","buy":$price,"sell":90}"""
    @Test fun `timezone-less provider UTC and offset timestamps give the same closed bin`() {
        val a=ShardHistoryParsing.parse("[${point("2026-11-03T12:01:00")}]",now)
        val b=ShardHistoryParsing.parse("[${point("2026-11-03T16:01:00+04:00")}]",now)
        assertEquals(a,b)
        assertEquals(now,a.single().at)
        assertEquals(100.0,a.single().ask)
    }
    @Test fun `unfinished bins future records and invalid spreads cannot leak into decisions`() {
        val raw="[${point("2026-11-03T12:06:00")},${point("2026-11-03T12:03:00")},${point("2026-11-03T12:01:00","-1")}]"
        assertTrue(ShardHistoryParsing.parse(raw,now-60_000).isEmpty())
        assertTrue(ShardHistoryParsing.parse("[${point("2026-11-03T12:01:00","80")}]",now).isEmpty())
    }
    @Test fun `newest actual observation wins a bin independently of response order`() {
        val a=point("2026-11-03T12:01:00","100")
        val b=point("2026-11-03T12:03:00","110")
        assertEquals(110.0,ShardHistoryParsing.parse("[$b,$a,$b]",now).single().ask)
        assertEquals(ShardHistoryParsing.parse("[$a,$b]",now),ShardHistoryParsing.parse("[$b,$a]",now))
    }
    @Test fun `conflicting duplicate timestamps and missing intervals stay unknown`() {
        val a=point("2026-11-03T12:01:00","100")
        val b=point("2026-11-03T12:01:00","110")
        assertTrue(ShardHistoryParsing.parse("[$a,$b]",now).isEmpty())
        val sparse=ShardHistoryParsing.parse("[${point("2026-11-03T11:01:00")},$a]",now)
        assertEquals(2,sparse.size,"missing five-minute intervals must not be interpolated")
    }
    @Test fun `coarse weekly samples never become fine replay observations`() {
        val samples=ShardHistoryParsing.parse("[${point("2026-11-03T10:00:00")}]",now,7_200_000)
        val normalized=ShardHistoryParsing.normalize(samples,now)
        assertTrue(normalized.fine.isEmpty())
        assertEquals(1,normalized.week.size)
        assertEquals(Instant.parse("2026-11-03T12:00:00Z").toEpochMilli(),normalized.week.single().at)
    }
}
