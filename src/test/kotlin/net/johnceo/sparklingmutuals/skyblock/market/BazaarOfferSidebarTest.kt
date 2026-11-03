package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarOfferSidebarTest {
    private val now=MarketFixtures.NOW
    private val menu=Any()
    private val items=mapOf(10 to Any(),11 to Any(),12 to Any(),13 to Any())
    private fun rec(id: String,state: AdviceState)=Recommendation(id,state,100.0,now,"EXPERIMENTAL_RANGE",98.0,"range")
    @Test fun `current snapshot lists unique Safari offers with explicit actions and retains blue HOLD only`() {
        val snapshot=OrderMarkerSnapshot(menu,1,items,mapOf(10 to rec("SHARD_BLUEBIRD",AdviceState.HOLD),
            11 to rec("SHARD_SNOOZLE",AdviceState.SELL),12 to rec("SHARD_BLUEBIRD",AdviceState.HOLD),
            13 to rec("SHARD_FOXTROT",AdviceState.UNKNOWN)))
        val rows=snapshot.rows(menu,1,items::get,now)
        assertEquals(listOf("SHARD_BLUEBIRD","SHARD_FOXTROT","SHARD_SNOOZLE"),rows.map {it.productId})
        assertEquals(listOf("TAKE DOWN","UNAVAILABLE","KEEP LISTED"),rows.map {BazaarUnlistPresentation.action(it.state)})
        assertTrue(snapshot.needsUnlist(menu,1,10,items.getValue(10),now))
        assertFalse(snapshot.needsUnlist(menu,1,11,items.getValue(11),now))
    }
    @Test fun `stale or invalid decisions are unavailable while replaced menus revisions and items disappear`() {
        for(advice in listOf(rec("SHARD_BLUEBIRD",AdviceState.SELL).copy(quoteAt=now-180001),
            rec("SHARD_BLUEBIRD",AdviceState.SELL).copy(target=null),rec("SHARD_BLUEBIRD",AdviceState.SELL).copy(ask=Double.NaN))) {
            val snapshot=OrderMarkerSnapshot(menu,1,items,mapOf(10 to advice))
            assertEquals(AdviceState.UNKNOWN,snapshot.rows(menu,1,items::get,now).single().state)
            assertTrue(snapshot.rows(Any(),1,items::get,now).isEmpty())
            assertTrue(snapshot.rows(menu,2,items::get,now).isEmpty())
            assertTrue(snapshot.rows(menu,1,{Any()},now).isEmpty())
        }
        val unsupported=OrderMarkerSnapshot(menu,1,items,mapOf(10 to rec("SHARD_BAL",AdviceState.HOLD)))
        assertTrue(unsupported.rows(menu,1,items::get,now).isEmpty())
    }
    @Test fun `left panel reserves Bazaar controls fits screen and refuses unreadably narrow margins`() {
        for(left in listOf(232,300,500)) {
            val bounds=ShardSaleSidebarLayout.leftBounds(960,540,left,37)!!
            assertTrue(bounds.x>=4);assertTrue(bounds.x+bounds.width<=left-30)
            assertTrue(bounds.y>=0 && bounds.y+bounds.height<=540)
            assertTrue(bounds.scale>=.75f);assertTrue(bounds.rows in 1..12)
            assertEquals(37-bounds.rows,bounds.clampScroll(1000,37))
        }
        assertNull(ShardSaleSidebarLayout.leftBounds(320,240,70,37))
        assertNull(ShardSaleSidebarLayout.leftBounds(320,240,-1,1))
    }
}
