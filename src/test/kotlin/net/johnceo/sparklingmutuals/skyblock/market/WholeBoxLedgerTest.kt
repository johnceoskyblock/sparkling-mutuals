package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WholeBoxLedgerTest {
    private val now=MarketFixtures.NOW
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val bird="SHARD_BLUEBIRD"
    private val spider="SHARD_VORACIOUS_SPIDER"
    private fun page(n: Int?,id: String=bird,amount: Long=10,view: String="",filtered: Boolean=false,unsupported: Int=0)=
        WholeBoxPage(listOf(BoxShard(10,id,amount)),n,if(n==null)null else 2,view,filtered,unsupported)
    private fun ledger()=WholeBoxLedger().apply {select(scope,null);beginOpening()}

    @Test fun `pages replace quantities overlaps deduplicate and zero removes without inferring missing counts`() {
        val c=ledger();c.observe(page(1),now);c.observe(page(1,amount=20),now+1);c.observe(page(2,spider,3),now+2)
        assertEquals(mapOf(bird to 20L,spider to 3L),c.rows().associate {it.productId to it.amount})
        assertTrue(c.coverage().complete)
        c.observe(page(2,bird,7),now+3)
        assertEquals(mapOf(bird to 7L,spider to 3L),c.rows().associate {it.productId to it.amount})
        c.observe(page(1,bird,0),now+4);assertEquals(listOf(spider),c.rows().map {it.productId})
        c.beginOpening();c.observe(page(1,bird,2),now+5)
        assertEquals(1,c.coverage().older);assertFalse(c.coverage().complete)
    }
    @Test fun `unknown page coverage active filters changed views and unsupported entries never look complete`() {
        val c=ledger();c.observe(page(null),now)
        assertNull(c.coverage().total);assertFalse(c.coverage().complete)
        c.beginOpening();c.observe(page(1,filtered=true),now);c.observe(page(2,spider,filtered=true),now)
        assertFalse(c.coverage().complete)
        c.beginOpening();c.observe(page(1),now);c.observe(page(2,spider,view="sort:new"),now)
        assertEquals(1,c.coverage().visited);assertFalse(c.coverage().complete)
        c.observe(page(1,view="sort:new",unsupported=1),now)
        assertFalse(c.coverage().complete)
    }
    @Test fun `verified scopes persist but unverified observations cannot migrate across connections or profiles`() {
        val c=ledger();c.observe(page(1),now);val raw=c.export()
        c.select(null,Any());c.observe(page(1,spider),now)
        assertFalse(c.export().contains(spider))
        c.select(null,Any());assertTrue(c.rows().isEmpty())
        c.select(MarketScope(scope.accountId,"3".repeat(32)),null);assertTrue(c.rows().isEmpty())
        val restored=WholeBoxLedger();restored.restore(raw,now);restored.select(scope,null)
        assertEquals(bird,restored.rows().single().productId);assertEquals(1,restored.coverage().older)
    }
    @Test fun `late restore cannot replace newer live edits or resurrect explicit zeros`() {
        val saved=ledger().apply {observe(page(1),now)}.export()
        val c=ledger();c.observe(page(1,amount=0),now+10);c.restore(saved,now+20)
        assertTrue(c.rows().isEmpty())
        c.observe(page(1,amount=5),now+30);c.restore(saved,now+40)
        assertEquals(5,c.rows().single().amount)
        c.observe(page(1,amount=100),now+20);assertEquals(5,c.rows().single().amount)
    }
    @Test fun `product and scope caps remain bounded and truncated collection is partial`() {
        val c=ledger()
        repeat(513) {c.observe(page(1,"SHARD_TEST_$it"),now)}
        assertEquals(512,c.rows().size);assertFalse(c.coverage().complete);assertTrue(c.coverage().limited)
        repeat(5) {c.select(MarketScope(scope.accountId,(it+6).toString(16).repeat(32)),null);c.observe(page(1),now)}
        assertTrue(c.export().toByteArray().size<=WholeBoxLedger.LIMIT)
        c.select(scope,null);assertTrue(c.rows().isEmpty(),"Oldest fifth scope is evicted")
        val clean=c.export()
        for(raw in listOf("x".repeat(WholeBoxLedger.LIMIT+1),clean.replace("\"schema\":1","\"schema\":2"))) {
            c.restore(raw,now);assertEquals(clean,c.export())
        }
    }
}
