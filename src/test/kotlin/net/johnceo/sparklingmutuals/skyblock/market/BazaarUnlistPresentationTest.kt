package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarUnlistPresentationTest {
    private val now=MarketFixtures.NOW
    private val menu=Any()
    private val item=Any()
    private val rec=Recommendation("SHARD_BLUEBIRD",AdviceState.HOLD,100.0,now,"EXPERIMENTAL_RANGE",98.0,"range")
    private fun snapshot(advice: Recommendation=rec)=OrderMarkerSnapshot(menu,7,mapOf(10 to item),mapOf(10 to advice))
    @Test fun `blue mark requires the same menu revision item reference and a fresh confirmed HOLD`() {
        val s=snapshot()
        assertTrue(s.needsUnlist(menu,7,10,item,now))
        assertFalse(s.needsUnlist(Any(),7,10,item,now))
        assertFalse(s.needsUnlist(menu,8,10,item,now))
        assertFalse(s.needsUnlist(menu,7,10,Any(),now))
        assertFalse(s.needsUnlist(menu,7,11,item,now))
        assertFalse(s.needsUnlist(menu,7,10,item,now+180001))
        assertFalse(s.needsUnlist(menu,7,10,item,now-60001))
    }
    @Test fun `sell unknown and incomplete recommendation fields never mark an offer`() {
        for(advice in listOf(rec.copy(state=AdviceState.SELL),rec.copy(state=AdviceState.UNKNOWN),
            rec.copy(target=null),rec.copy(ask=null),rec.copy(ask=Double.NaN),rec.copy(target=Double.NaN)))
            assertFalse(snapshot(advice).needsUnlist(menu,7,10,item,now))
    }
    @Test fun `unlist border is opaque bright cyan blue distinct from ordinary red hold`() {
        val color=BazaarUnlistPresentation.COLOR
        assertEquals(255,color ushr 24)
        assertTrue((color and 255)>=200);assertTrue((color shr 8 and 255)>=180);assertTrue((color shr 16 and 255)<=30)
        assertNotEquals(ShardSalePresentation.color(AdviceState.HOLD),color)
    }
}
