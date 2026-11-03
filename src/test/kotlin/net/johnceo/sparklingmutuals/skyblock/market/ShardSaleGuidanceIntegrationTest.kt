package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShardSaleGuidanceIntegrationTest {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val box=listOf(MenuSlot(10,"SHARD_BLUEBIRD","Bluebird Shard",listOf("Owned: 10 Shards")))
    private val order=listOf(MenuSlot(10,"","§6§lSELL §6Bluebird Shard",listOf("Offer amount: 50x","Price per unit: 100 coins")))
    @Test fun `visible demand and revalidated offers combine without render network work`() {
        val calls=mutableListOf<Set<String>>();val messages=mutableListOf<String>()
        val controller=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,_->calls.add(ids)},{_,text->messages.add(text)},profitProducts={setOf("SHARD_BLUEBIRD")})
        val advice=mapOf("SHARD_BLUEBIRD" to Recommendation("SHARD_BLUEBIRD",AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE",100.0,"range"))
        assertEquals(setOf(10),controller.tick(true,true,scope,"Player","Hunting Box",box,advice,now).keys)
        assertEquals(setOf("SHARD_BLUEBIRD"),calls.last())
        controller.tick(true,true,scope,"Player","Your Bazaar Orders",order,advice,now)
        controller.tick(true,true,scope,"Player","",emptyList(),advice,now+60000)
        assertEquals(setOf("SHARD_BLUEBIRD"),calls.last())
        controller.tick(true,true,scope,"Player","",emptyList(),advice.mapValues {it.value.copy(state=AdviceState.HOLD,ask=98.0,quoteAt=now+120000)},now+120000)
        assertEquals(1,messages.size)
        repeat(10) {ShardSalePresentation.tooltip(advice.values.single(),now)}
        assertEquals(4,calls.size,"Presentation does not touch demand or transports")
    }
    @Test fun `disable profile uncertainty disconnect and stale advice clear colors and notifications`() {
        val calls=mutableListOf<Pair<Long,Set<String>>>()
        val messages=mutableListOf<String>()
        val c=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,generation->calls.add(generation to ids)},{_,text->messages.add(text)},profitProducts={setOf("SHARD_BLUEBIRD")})
        val rec=Recommendation("SHARD_BLUEBIRD",AdviceState.SELL,100.0,now,"EXPERIMENTAL_RANGE",100.0,"range")
        c.tick(true,true,scope,"Player","Your Bazaar Orders",order,mapOf(rec.productId to rec),now)
        val first=calls.last().first
        c.tick(true,true,null,"Player","",emptyList(),mapOf(rec.productId to rec.copy(state=AdviceState.HOLD,ask=90.0)),now+60000)
        assertTrue(calls.last().first>first);assertTrue(calls.last().second.isEmpty());assertTrue(messages.isEmpty())
        val stale=c.tick(true,true,scope,"Player","Hunting Box",box,mapOf(rec.productId to rec),now+180001)
        assertEquals(AdviceState.UNKNOWN,stale.getValue(10).state)
        assertTrue(c.tick(false,true,scope,"Player","Hunting Box",box,mapOf(rec.productId to rec),now).isEmpty())
        assertTrue(c.tick(true,false,scope,"Player","Hunting Box",box,mapOf(rec.productId to rec),now).isEmpty())
        assertTrue(calls.last().second.isEmpty());assertTrue(messages.isEmpty())
    }
}
