package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SavedBoxGuidanceSessionTest {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("a".repeat(32),"b".repeat(32))
    private val bird="SHARD_BLUEBIRD"
    private val hide="SHARD_HIDEONWALL"
    private val demands=mutableListOf<Set<String>>()
    private val c=SaleGuidanceSession(SellOfferWatch(),{_,_,ids,_->demands.add(ids)},{_,_->},profitProducts={setOf(bird,hide)})
    private fun slots(id: String=bird,count: Long=10)=listOf(MenuSlot(10,id,"${SaleProducts.names.getValue(id)} Shard",listOf("Owned: $count Shards")))
    private fun advice(at: Long=now)=mapOf(bird to Recommendation(bird,AdviceState.SELL,100.0,at,"EXPERIMENTAL_RANGE"),
        hide to Recommendation(hide,AdviceState.HOLD,100.0,at,"EXPERIMENTAL_RANGE"))
    private fun tick(title: String="Hunting Box",items: List<MenuSlot> = slots(),at: Long=now,enabled: Boolean=true,
        selected: MarketScope?=scope,connection: Any?=null)=c.tick(enabled,true,selected,"Local",title,items,advice(at),at,connection)

    @Test fun `all remembered pages participate in advice and box demand but closing keeps only watches`() {
        tick();tick("(2/3) Hunting Box",slots(hide,20),now+50)
        assertEquals(setOf(bird,hide,SaleMarketProducts.ESSENCE),demands.last())
        assertEquals(listOf(AdviceState.SELL,AdviceState.HOLD),c.sidebar(now+50).map {it.advice.state})
        tick("Your Skills",emptyList(),now+100);assertTrue(c.sidebar(now+100).isEmpty());assertTrue(demands.last().isEmpty())
        tick(at=now+3000)
        assertEquals(listOf(true,false),c.sidebar(now+3000).map {it.refreshedThisOpening})
        assertEquals(setOf(bird,hide,SaleMarketProducts.ESSENCE),demands.last())
    }
    @Test fun `cached ownership does not keep stale colors and explicit zero removes a row`() {
        tick();assertEquals(AdviceState.UNKNOWN,c.sidebar(now+180001).single().advice.state)
        assertNull(c.sidebar(now+180001).single().advice.target)
        tick(items=slots(count=0),at=now+100);assertTrue(c.sidebar(now+100).isEmpty())
    }
    @Test fun `disable hides immediately but verified ownership returns on reenable`() {
        tick();tick(enabled=false,at=now+10)
        assertTrue(c.sidebar(now+10).isEmpty());assertTrue(demands.last().isEmpty())
        tick("(2/3) Hunting Box",slots(hide),now+100)
        assertEquals(setOf(bird,hide),c.sidebar(now+100).map {it.shard.productId}.toSet())
    }
    @Test fun `reset and restore preserve only verified profile rows without resurrecting live deletions`() {
        tick();val raw=c.exportBoxes();c.reset()
        tick(items=emptyList(),selected=null);assertTrue(c.sidebar(now).isEmpty())
        c.restoreBoxes(raw,now+10)
        tick(items=emptyList(),at=now+20);assertEquals(bird,c.sidebar(now+20).single().shard.productId)
        tick(items=slots(count=0),at=now+30);c.restoreBoxes(raw,now+40)
        assertTrue(c.sidebar(now+40).isEmpty())
        tick(items=emptyList(),selected=MarketScope(scope.accountId,"c".repeat(32)),at=now+50)
        assertTrue(c.sidebar(now+50).isEmpty())
    }
    @Test fun `unverified profile and connection transitions cannot leak temporary or saved rows`() {
        val first=Any();tick(selected=null,connection=first)
        assertEquals(1,c.sidebar(now).size)
        tick(items=emptyList(),selected=null,connection=Any(),at=now+10)
        assertTrue(c.sidebar(now+10).isEmpty())
        tick(selected=null,connection=first,at=now+20)
        tick(items=emptyList(),selected=scope,connection=first,at=now+30)
        assertTrue(c.sidebar(now+30).isEmpty())
    }
    @Test fun `short page transition retains current opening freshness`() {
        tick();tick("",emptyList(),now+20);tick("(2/3) Hunting Box",slots(hide),now+100)
        assertEquals(2,c.sidebar(now+100).size)
        assertTrue(c.sidebar(now+100).all {it.refreshedThisOpening})
    }
}
