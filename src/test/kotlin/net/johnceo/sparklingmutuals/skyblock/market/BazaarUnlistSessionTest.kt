package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarUnlistSessionTest {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val connection=Any()
    private val bird="SHARD_BLUEBIRD"
    private val hide="SHARD_HIDEONWALL"
    private val snooze="SHARD_SNOOZLE"
    private var ranked=setOf(bird)
    private var owned=setOf(hide)
    private var rankingReads=0
    private val messages=mutableListOf<String>()
    private val demands=mutableListOf<Set<String>>()
    private val c=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,_->demands.add(ids)},{_,text->messages.add(text)},
        profitProducts={rankingReads++;ranked},ownedProducts={owned})
    private fun order(slot: Int,id: String=bird,owner: String?="Player",completed: Boolean=false,buy: Boolean=false)=MenuSlot(slot,id,
        "§6§l${if(buy)"BUY"else "SELL"} §6${SaleProducts.names[id] ?: "Bal"} Shard",
        listOf("Offer amount: 20x","Price per unit: 100 coins")+(owner?.let {listOf("By: $it")} ?: emptyList())+
            if(completed)listOf("Filled: 20/20 100%!")else emptyList())
    private fun rec(id: String,state: AdviceState=AdviceState.HOLD,at: Long=now,key: String="range")=
        Recommendation(id,state,100.0,at,"EXPERIMENTAL_RANGE",if(state==AdviceState.SELL)100.0 else 98.0,key,100.0)
    private fun tick(rows: List<MenuSlot> = listOf(order(10)),advice: Map<String,Recommendation> = mapOf(bird to rec(bird)),
        at: Long=now,title: String="Your Bazaar Orders",selected: MarketScope?=scope,on: Boolean=true)=
        c.tick(on,true,selected,"Player",title,rows,advice,at,connection)

    @Test fun `top ten or owned Safari offers mark every own HOLD slot but not foreign buy filled or ineligible rows`() {
        val rows=listOf(order(10),order(11,hide,owner=null),order(12,snooze),order(13,hide),
            order(14,owner="Other"),order(15,buy=true),order(16,completed=true),order(17,"SHARD_BAL"))
        val advice=listOf(bird,hide,snooze,"SHARD_BAL").associateWith {rec(it)}
        assertTrue(tick(rows,advice).isEmpty(),"Order markers are separate from box advice")
        assertEquals(setOf(10,11,13),c.orderMarkers().keys)
        assertEquals(setOf(bird,hide),demands.last())
        assertTrue(messages.isEmpty(),"Already-HOLD offers only get an outline")
    }
    @Test fun `confirmed transitions group products while fast box HOLD and repeated quotes cannot mark orders early`() {
        val latches=mutableMapOf<String,AdviceLatch>()
        fun confirmed(ask: Double,at: Long)=listOf(bird,hide).associateWith {id->
            val latch=ShardTargetModel.observe(latches[id],MarketQuote(id,ask,90.0,at),TargetResult(100.0,false,null),at,true)
            latches[id]=latch
            rec(id,latch.state,at).copy(ask=ask,declineFromAsk=latch.confirmedSellAsk)
        }
        val rows=listOf(order(10),order(11),order(12,hide))
        tick(rows,confirmed(100.0,now));assertTrue(c.orderMarkers().isEmpty())
        val pending=confirmed(98.0,now+60000)
        val box=tick(listOf(MenuSlot(10,bird,"Bluebird Shard",listOf("Owned: 10 Shards"))),pending,now+60000,"Hunting Box")
        assertEquals(AdviceState.HOLD,box.getValue(10).state)
        repeat(5) {tick(rows,pending,now+60001+it);assertTrue(c.orderMarkers().isEmpty());assertTrue(messages.isEmpty())}
        val held=confirmed(98.0,now+120000)
        tick(rows,held,now+120000)
        assertEquals(setOf(10,11,12),c.orderMarkers().keys)
        assertEquals(1,messages.size);assertTrue(messages.single().contains("Bluebird, Hideonwall"))
        tick(rows,held,now+120001);assertEquals(1,messages.size)
    }
    @Test fun `eligibility loss and return cannot manufacture an alert for already HOLD data`() {
        owned=emptySet()
        tick(advice=mapOf(bird to rec(bird,AdviceState.SELL)))
        ranked=emptySet()
        tick(advice=mapOf(bird to rec(bird,at=now+60000)),at=now+60000)
        assertTrue(c.orderMarkers().isEmpty());assertTrue(demands.last().isEmpty())
        ranked=setOf(bird)
        tick(advice=mapOf(bird to rec(bird,at=now+120000)),at=now+120000)
        assertEquals(setOf(10),c.orderMarkers().keys);assertTrue(messages.isEmpty())
        ranked=emptySet();owned=setOf(bird)
        tick(advice=mapOf(bird to rec(bird,at=now+180000)),at=now+180000)
        assertEquals(setOf(10),c.orderMarkers().keys);assertTrue(messages.isEmpty())
    }
    @Test fun `unknown stale disabled and unverified scope suppress visible markers and reset chat baseline`() {
        tick();assertEquals(setOf(10),c.orderMarkers().keys)
        tick(advice=mapOf(bird to rec(bird,AdviceState.UNKNOWN,now+1)),at=now+1)
        assertTrue(c.orderMarkers().isEmpty())
        tick(advice=mapOf(bird to rec(bird,at=now+2)),at=now+2);assertTrue(messages.isEmpty())
        tick(at=now+180001);assertTrue(c.orderMarkers().isEmpty())
        tick(selected=null);assertTrue(c.orderMarkers().isEmpty());assertTrue(demands.last().isEmpty())
        tick(on=false);assertTrue(c.orderMarkers().isEmpty())
        tick(title="Your Skills");assertTrue(c.orderMarkers().isEmpty())
        c.reset();assertTrue(c.orderMarkers().isEmpty())
    }
    @Test fun `background changes retain last observed wording but empty idle contexts do not bootstrap rankings`() {
        tick(rows=emptyList(),advice=emptyMap(),title="Your Skills")
        assertEquals(0,rankingReads)
        tick(advice=mapOf(bird to rec(bird,AdviceState.SELL)))
        tick(rows=emptyList(),advice=mapOf(bird to rec(bird,at=now+120000)),at=now+120000,title="Your Skills")
        assertEquals(1,messages.size);assertTrue(messages.single().contains("last observed"))
        assertTrue(c.orderMarkers().isEmpty())
    }
}
