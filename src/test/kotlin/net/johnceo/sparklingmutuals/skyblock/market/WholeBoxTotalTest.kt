package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WholeBoxTotalTest {
    private val now=MarketFixtures.NOW
    private val id="SHARD_VORACIOUS_SPIDER"
    private val bird="SHARD_BLUEBIRD"
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val connection=Any()
    private var refreshes=0
    private val c=WholeBoxTotal {refreshes++}
    private fun items(id: String=this.id,n: Long=3)=listOf(MenuSlot(10,id,id.removePrefix("SHARD_").replace('_',' '),listOf("Owned: $n Shards")))
    private fun prices(at: Long=now)=QuoteSnapshot(at,mapOf(id to MarketQuote(id,100.0,90.0,at),bird to MarketQuote(bird,200.0,180.0,at)),emptyMap())
    private fun tick(title: String="(1/2) Hunting Box",id: String=this.id,at: Long=now,on: Boolean=true,selected: MarketScope?=scope,conn: Any?=connection)=
        c.tick(on,true,selected,conn,title,items(id),prices(at),at)

    @Test fun `opening and page visits refresh saved counts while repeat visits never add quantities`() {
        tick();tick("(2/2) Hunting Box",bird,now+1)
        assertEquals(900.0,c.snapshot(now+1)!!.value.known);assertTrue(c.snapshot(now+1)!!.coverage.complete)
        tick("(2/2) Hunting Box",bird,now+2);assertEquals(900.0,c.snapshot(now+2)!!.value.known)
        tick("Your Skills",at=now+3);assertNull(c.snapshot(now+3))
        tick(at=now+3000)
        assertEquals(900.0,c.snapshot(now+3000)!!.value.known)
        assertEquals(1,c.snapshot(now+3000)!!.coverage.older)
    }
    @Test fun `disable or non Skyblock stops valuation demand and stale read never requests work`() {
        tick();assertEquals(1,refreshes)
        val stale=c.snapshot(now+180001)!!
        assertNull(stale.value.known);repeat(10){c.snapshot(now+180001)};assertEquals(1,refreshes)
        tick(on=false);assertNull(c.snapshot(now));assertEquals(1,refreshes)
        c.tick(true,false,scope,connection,"Hunting Box",items(),prices(),now)
        assertNull(c.snapshot(now));assertEquals(1,refreshes)
    }
    @Test fun `unknown profiles and new connections cannot adopt persisted or anonymous counts`() {
        tick(selected=null);assertFalse(c.export().contains(id))
        tick(id=bird,at=now+1,selected=null,conn=Any())
        assertEquals(600.0,c.snapshot(now+1)!!.value.known)
        tick(at=now+2);assertEquals(300.0,c.snapshot(now+2)!!.value.known)
        val raw=c.export();c.reset();assertNull(c.snapshot(now+2))
        c.restore(raw,now+3)
        tick(id=bird,at=now+4)
        assertEquals(900.0,c.snapshot(now+4)!!.value.known);assertEquals(1,c.snapshot(now+4)!!.coverage.older)
    }
    @Test fun `empty loading packets cannot claim a zero or complete total`() {
        c.tick(true,true,scope,connection,"(1/1) Hunting Box",emptyList(),prices(),now)
        assertNotNull(c.snapshot(now));assertNull(c.snapshot(now)!!.value.known)
        assertFalse(c.snapshot(now)!!.coverage.complete)
        c.tick(true,true,scope,connection,"(1/1) Hunting Box",items(n=0),prices(),now+1)
        assertEquals(0.0,c.snapshot(now+1)!!.value.known)
    }
}
