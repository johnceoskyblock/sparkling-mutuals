package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarOwnedEligibilityTest {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val conn=Any()
    private val bird="SHARD_BLUEBIRD"
    private fun row(count: Long=10)=MenuSlot(10,bird,"Bluebird",listOf("Owned: $count Shards"))
    private fun select(c: WholeBoxTotal,at: Long=now)=c.tick(true,true,scope,conn,"Hunting Box",emptyList(),null,at)
    @Test fun `shared tracker exposes positive Safari ownership only for its current verified scope and connection`() {
        val c=WholeBoxTotal()
        c.tick(true,true,scope,conn,"Hunting Box",listOf(row(),MenuSlot(11,"SHARD_BAL","Bal",listOf("Owned: 3 Shards"))),null,now)
        assertEquals(setOf(bird),c.ownedSafariProducts(scope,conn))
        assertTrue(c.ownedSafariProducts(null,conn).isEmpty());assertTrue(c.ownedSafariProducts(scope,Any()).isEmpty())
        assertTrue(c.ownedSafariProducts(scope.copy(profileId="3".repeat(32)),conn).isEmpty())
        c.tick(true,true,scope,conn,"Hunting Box",listOf(row(0)),null,now+1)
        assertTrue(c.ownedSafariProducts(scope,conn).isEmpty())
    }
    @Test fun `legacy narrow ownership seeds absent scopes but authoritative empty total scope wins`() {
        val legacy=SavedHuntingBoxLedger(SaleProducts.names.keys).apply {select(scope,conn);observe(listOf(BoxShard(10,bird,10)),now)}.export()
        val first=WholeBoxTotal().apply {restore(legacy,now);select(this)}
        assertEquals(setOf(bird),first.ownedSafariProducts(scope,conn))
        first.tick(true,true,scope,conn,"Hunting Box",listOf(row(0)),null,now+1)
        val empty=first.export()
        val next=WholeBoxTotal().apply {restore(legacy,now+2);restore(empty,now+2);select(this,now+2)}
        assertTrue(next.ownedSafariProducts(scope,conn).isEmpty())
        next.tick(true,true,scope,conn,"Hunting Box",listOf(row(5)),null,now+3)
        next.restore(legacy,now+4);assertEquals(setOf(bird),next.ownedSafariProducts(scope,conn))
    }
}
