package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WholeBoxTotalsTest {
    private val now=MarketFixtures.NOW
    private val rows=listOf(WholeBoxOwned("SHARD_BLUEBIRD",10,now),WholeBoxOwned("SHARD_BAL",3,now))
    private fun quotes(ask: Double=200.0,at: Long=now)=QuoteSnapshot(at,mapOf(
        "SHARD_BLUEBIRD" to MarketQuote("SHARD_BLUEBIRD",100.0,90.0,at),
        "SHARD_BAL" to MarketQuote("SHARD_BAL",ask,80.0,at)),emptyMap())
    @Test fun `cumulative sell offer total uses asks rather than bids or profit ranking`() {
        val value=WholeBoxTotals.calculate(rows,quotes(),now)
        assertEquals(1600.0,value.known);assertEquals(2,value.priced);assertEquals(0,value.missing)
    }
    @Test fun `missing invalid or unsafe prices yield a known subtotal and stale data is unavailable`() {
        for(ask in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.MAX_VALUE,0.0,79.0)) {
            val value=WholeBoxTotals.calculate(rows,quotes(ask),now)
            assertEquals(1000.0,value.known);assertEquals(1,value.missing)
        }
        assertNull(WholeBoxTotals.calculate(rows,null,now).known)
        assertNull(WholeBoxTotals.calculate(rows,quotes(at=now-180001),now).known)
        assertNull(WholeBoxTotals.calculate(rows,quotes(at=now+60001),now).known)
        assertEquals(0.0,WholeBoxTotals.calculate(emptyList(),quotes(),now).known)
    }
}
