package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExperimentalRangeTest {
    private val now=MarketFixtures.NOW
    private val history=ShardHistoryParsing.normalize(MarketFixtures.samples().map {it.copy(basis=PriceBasis.UNVERIFIED_REFERENCE,availableAt=null)},now)
    @Test fun `approved descriptive mode accepts references without forging forecast certification`() {
        val target=ExperimentalRange.target(history,MarketQuote("SHARD_BLUEBIRD",100.0,90.0,now),now)
        assertEquals(100.0,target.price)
        assertTrue(history.fine.all {it.availableAt==null && it.basis==PriceBasis.UNVERIFIED_REFERENCE})
    }
    @Test fun `missing freshness coverage or reference mismatch suppresses range colors`() {
        assertNull(ExperimentalRange.target(history,MarketQuote("SHARD_BLUEBIRD",120.0,90.0,now),now).price)
        assertNull(ExperimentalRange.target(history,null,now).price)
        assertNull(ExperimentalRange.target(history,MarketQuote("SHARD_BLUEBIRD",100.0,90.0,now-180001),now).price)
        assertNull(ExperimentalRange.target(history.copy(fine=history.fine.take(10)),MarketQuote("SHARD_BLUEBIRD",100.0,90.0,now),now).price)
    }
    @Test fun `range still rejects isolated spikes but permits sustained levels`() {
        fun target(points: List<PriceSample>,ask: Double)=ExperimentalRange.target(ShardHistoryParsing.normalize(points,now),MarketQuote("SHARD_BLUEBIRD",ask,ask*.9,now),now)
        assertEquals(100.0,target(MarketFixtures.samples(price={if(it==now-300000)10000.0 else 100.0}),100.0).price)
        assertEquals(50.0,target(MarketFixtures.samples(price={if(it>now-6*3600000)50.0 else 100.0}),50.0).price)
    }
}
