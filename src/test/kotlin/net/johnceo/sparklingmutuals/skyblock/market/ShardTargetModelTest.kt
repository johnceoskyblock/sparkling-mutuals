package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal object MarketFixtures {
    const val STEP=300_000L
    const val HOUR=3_600_000L
    const val DAY=24*HOUR
    const val NOW=1_800_057_600_000L
    fun samples(days: Int=8, until: Long=NOW, price: (Long)->Double={100.0}): List<PriceSample> =
        (0 until days*288).map { offset -> val at=until-offset*STEP; PriceSample(at,price(at),STEP,
            basis=PriceBasis.VERIFIED_BEST_ASK,availableAt=at) }.reversed()
    fun target(samples: List<PriceSample>,now: Long=NOW)=ShardTargetModel.target(ShardHistoryParsing.normalize(samples,now),now)
}

class ShardTargetModelTest {
    private val now=MarketFixtures.NOW
    private fun quote(price: Double,at: Long=now)=MarketQuote("SHARD_BLUEBIRD",price,price*.9,at)
    private val ready=TargetResult(100.0,false,null)

    @Test fun `recent hours dominate with a bounded weekly contribution`() {
        val samples=MarketFixtures.samples(price={at -> when {at>now-3*MarketFixtures.HOUR ->300.0;at>now-MarketFixtures.DAY ->200.0;else->100.0}})
        assertEquals(250.0,MarketFixtures.target(samples).price!!,.001)
    }
    @Test fun `an isolated wick cannot create a target and sustained lower prices are retained`() {
        val samples=MarketFixtures.samples(price={if(it==now)100_000.0 else 100.0})
        assertEquals(100.0,MarketFixtures.target(samples).price)
        val lowered=MarketFixtures.samples(price={if(it>now-6*MarketFixtures.HOUR)50.0 else 100.0})
        assertEquals(50.0,MarketFixtures.target(lowered).price)
    }
    @Test fun `old incomplete or future-only history cannot qualify a price target`() {
        assertNull(MarketFixtures.target(MarketFixtures.samples(days=1)).price)
        assertNull(MarketFixtures.target(MarketFixtures.samples().dropLast(4)).price)
        assertNull(MarketFixtures.target(MarketFixtures.samples().map { it.copy(at=it.at+20*MarketFixtures.DAY) }).price)
    }
    @Test fun `sustained falling medians allow an exit at the current competitive ask`() {
        val points=MarketFixtures.samples(price={at -> when {at>now-900_000 ->106.0;at>now-1_800_000 ->108.0;else->110.0}})
        val target=MarketFixtures.target(points)
        assertTrue(target.falling)
        val advice=ShardTargetModel.observe(null,quote(106.0),target,now,true)
        assertEquals(AdviceState.SELL,advice.state)
        assertEquals(106.0,advice.target)
    }
    @Test fun `unvalidated model and stale quote suppress otherwise green advice`() {
        assertEquals(AdviceState.UNKNOWN,ShardTargetModel.observe(null,quote(110.0),ready,now,false).state)
        assertEquals(AdviceState.UNKNOWN,ShardTargetModel.observe(null,quote(110.0,now-180_001),ready,now,true).state)
    }
    @Test fun `sell to hold needs two distinct low observations at least a minute apart`() {
        val initial=ShardTargetModel.observe(null,quote(100.0),ready,now,true)
        val low=ShardTargetModel.observe(initial,quote(98.0,now+60_000),ready,now+60_000,true)
        assertEquals(AdviceState.SELL,low.state)
        assertEquals(AdviceState.SELL,ShardTargetModel.observe(low,quote(98.0,now+60_000),ready,now+120_000,true).state)
        assertEquals(AdviceState.HOLD,ShardTargetModel.observe(low,quote(98.0,now+120_000),ready,now+120_000,true).state)
    }
    @Test fun `band recovery resets a pending decline and data recovery establishes a new baseline`() {
        val initial=ShardTargetModel.observe(null,quote(99.5),ready,now,true)
        assertEquals(AdviceState.SELL,initial.state)
        val low=ShardTargetModel.observe(initial,quote(98.0,now+60_000),ready,now+60_000,true)
        val recovered=ShardTargetModel.observe(low,quote(99.0,now+120_000),ready,now+120_000,true)
        val again=ShardTargetModel.observe(recovered,quote(98.0,now+180_000),ready,now+180_000,true)
        assertEquals(AdviceState.SELL,again.state)
        val missing=ShardTargetModel.observe(again,null,ready,now+180_000,true)
        assertEquals(AdviceState.UNKNOWN,missing.state)
        assertNull(missing.belowSince)
        assertEquals(AdviceState.HOLD,ShardTargetModel.observe(missing,quote(98.0,now+240_000),ready,now+240_000,true).state)
    }
}
