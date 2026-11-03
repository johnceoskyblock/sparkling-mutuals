package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardFastDisplayTest {
    private val id="SHARD_BLUEBIRD"
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val box=listOf(MenuSlot(10,id,"Bluebird Shard",listOf("Owned: 10 Shards")))
    private val order=listOf(MenuSlot(10,"","§6§lSELL §6Bluebird Shard",listOf("Offer amount: 50x","Price per unit: 100 coins")))
    private val alerts=mutableListOf<String>()
    private val session=SaleGuidanceSession(SellOfferWatch(),{_,_,_,_->},{_,text->alerts.add(text)},profitProducts={setOf(id)})
    private fun rec(ask: Double,at: Long=now,state: AdviceState=AdviceState.SELL,key: String="range")=
        Recommendation(id,state,100.0,at,"EXPERIMENTAL_RANGE",ask,key,100.0)
    private fun show(rec: Recommendation,at: Long=rec.quoteAt,selected: MarketScope?=scope,connection: Any?=null)=
        session.tick(true,true,selected,"Player","Hunting Box",box,mapOf(id to rec),at,connection).getValue(10)

    @Test fun `first below-range quote changes box and sidebar while actual model warning waits for confirmation`() {
        var latch: AdviceLatch?=null
        fun confirmed(ask: Double,at: Long): Recommendation {
            val next=ShardTargetModel.observe(latch,MarketQuote(id,ask,90.0,at),TargetResult(100.0,false,null),at,true)
            latch=next
            return rec(ask,at,next.state).copy(declineFromAsk=next.confirmedSellAsk)
        }
        val sell=confirmed(100.0,now)
        session.tick(true,true,scope,"Player","Your Bazaar Orders",order,mapOf(id to sell),now)
        assertEquals(AdviceState.SELL,show(sell).state)
        val first=confirmed(98.0,now+60000)
        assertEquals(AdviceState.SELL,first.state,"Alert model retains its confirmation minute")
        assertEquals(AdviceState.HOLD,show(first).state,"Box does not wait the extra alert minute")
        assertEquals(AdviceState.HOLD,session.sidebar(now+60000).single().advice.state)
        repeat(5) {show(first,now+60000+it);assertTrue(alerts.isEmpty(),"Repeated reads cannot confirm alerts")}
        val held=confirmed(98.0,now+120000)
        assertEquals(AdviceState.HOLD,held.state)
        show(held)
        assertEquals(1,alerts.size)
        assertTrue(alerts.single().contains("after a price decline"))
        show(held);assertEquals(1,alerts.size)
    }

    @Test fun `immediate display HOLD persists through hysteresis band and recovers only at sell threshold`() {
        show(rec(100.0))
        assertEquals(AdviceState.HOLD,show(rec(98.0,now+60000)).state)
        assertEquals(AdviceState.HOLD,show(rec(99.0,now+61000)).state)
        assertEquals(AdviceState.HOLD,show(rec(99.49,now+62000)).state)
        assertEquals(AdviceState.SELL,show(rec(99.5,now+63000)).state)
        assertEquals(AdviceState.SELL,show(rec(98.5,now+64000)).state)
        assertEquals(AdviceState.HOLD,show(rec(98.49,now+65000)).state)
    }

    @Test fun `old snapshot cannot roll displayed HOLD back to SELL`() {
        val old=rec(100.0)
        show(old);show(rec(98.0,now+60000))
        assertEquals(AdviceState.HOLD,show(old,now+60001).state)
    }

    @Test fun `unknown and changed model reset display hysteresis without restoring stale colors`() {
        show(rec(100.0));show(rec(98.0,now+60000))
        assertEquals(AdviceState.UNKNOWN,show(rec(98.0,now+61000,AdviceState.UNKNOWN).copy(target=null)).state)
        assertEquals(AdviceState.SELL,show(rec(99.0,now+62000)).state)
        show(rec(98.0,now+63000))
        assertEquals(AdviceState.SELL,show(rec(99.0,now+64000,key="new-rule")).state)
        assertEquals(AdviceState.UNKNOWN,show(rec(98.0),now+180001).state)
        assertEquals(AdviceState.SELL,show(rec(99.0,now+181000,key="new-rule")).state)
    }

    @Test fun `disable profile connection and explicit reset clear display baselines`() {
        var at=now
        fun hold() {at+=10000;show(rec(100.0,at));assertEquals(AdviceState.HOLD,show(rec(98.0,at+1)).state)}
        hold()
        session.tick(false,true,scope,"Player","",emptyList(),emptyMap(),at+2)
        assertEquals(AdviceState.SELL,show(rec(99.0,at+3)).state)
        hold()
        assertEquals(AdviceState.SELL,show(rec(99.0,at+3),selected=MarketScope(scope.accountId,"3".repeat(32))).state)
        hold()
        assertEquals(AdviceState.SELL,show(rec(99.0,at+3),connection=Any()).state)
        hold();session.reset()
        assertEquals(AdviceState.SELL,show(rec(99.0,at+3)).state)
    }

    @Test fun `first observed pending sell uses current below-range quote without a saved display baseline`() {
        assertEquals(AdviceState.HOLD,show(rec(98.0)).state)
        assertEquals(AdviceState.HOLD,show(rec(99.0,now+1000)).state)
    }
}
