package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SavedHuntingBoxLedgerTest {
    private val hide="SHARD_HIDEONWALL"
    private val bird="SHARD_BLUEBIRD"
    private val owner="a".repeat(32)
    private val scope=MarketScope(owner,"b".repeat(32))
    private fun ledger()=SavedHuntingBoxLedger(setOf(hide,bird)).also {it.select(scope,Any());it.beginOpening()}
    private fun shard(id: String=hide,count: Long=243)=BoxShard(10,id,count)

    @Test fun `visited pages combine and revisits replace rather than sum`() {
        val ledger=ledger();ledger.observe(listOf(shard()),1000);ledger.observe(listOf(shard(bird,2)),1100)
        ledger.observe(listOf(shard(count=200)),1200)
        assertEquals(listOf(bird,hide),ledger.rows().map {it.first.productId})
        assertEquals(listOf(2L,200L),ledger.rows().map {it.first.amount})
        assertTrue(ledger.rows().all {it.second})
        ledger.beginOpening();assertTrue(ledger.rows().none {it.second})
        ledger.observe(listOf(shard(bird,3)),1300)
        assertEquals(listOf(true,false),ledger.rows().map {it.second})
    }
    @Test fun `omissions retain entries but explicit zero removes and invalid observations do not erase`() {
        val ledger=ledger();ledger.observe(listOf(shard()),1000);ledger.observe(emptyList(),1100)
        ledger.observe(listOf(shard(count=-1),shard("SHARD_FAKE",10)),1200)
        assertEquals(243,ledger.rows().single().first.amount)
        ledger.observe(listOf(shard(count=4),shard(count=7)),1300)
        assertEquals(243,ledger.rows().single().first.amount)
        ledger.observe(listOf(shard(count=0)),1400);assertTrue(ledger.rows().isEmpty())
    }
    @Test fun `verified scopes survive restarts but unknown scope never inherits their private rows`() {
        val old=ledger();old.observe(listOf(shard()),1000)
        val restarted=SavedHuntingBoxLedger(setOf(hide,bird));val connection=Any()
        restarted.restore(old.export(),2000);restarted.select(null,connection)
        assertTrue(restarted.rows().isEmpty())
        restarted.select(scope,connection);assertEquals(243,restarted.rows().single().first.amount)
        assertFalse(restarted.rows().single().second)
        restarted.select(MarketScope("c".repeat(32),scope.profileId),connection);assertTrue(restarted.rows().isEmpty())
        restarted.select(scope,connection);assertEquals(243,restarted.rows().single().first.amount)
    }
    @Test fun `unknown profile observations are connection local and never persisted or adopted`() {
        val ledger=SavedHuntingBoxLedger(setOf(hide));val first=Any()
        ledger.select(null,first);ledger.beginOpening();ledger.observe(listOf(shard()),1000)
        assertEquals(1,ledger.rows().size)
        val raw=ledger.export();assertFalse(raw.contains(hide))
        ledger.select(null,Any());assertTrue(ledger.rows().isEmpty())
        ledger.observe(listOf(shard()),1100);ledger.select(scope,first);assertTrue(ledger.rows().isEmpty())
    }
    @Test fun `late restore cannot overwrite or resurrect an already touched scope`() {
        val saved=ledger();saved.observe(listOf(shard(),shard(bird,2)),1000)
        val live=ledger();live.observe(listOf(shard(count=200),shard(bird,0)),1500)
        live.restore(saved.export(),2000)
        assertEquals(200,live.rows().single().first.amount)
        val empty=ledger();empty.observe(listOf(shard(count=0)),1500);empty.restore(saved.export(),2000)
        assertTrue(empty.rows().isEmpty())
    }
    @Test fun `only four verified scopes and allowed products survive persistence`() {
        val ledger=SavedHuntingBoxLedger(setOf(hide));val connection=Any()
        for(profile in listOf('b','c','d','e','f')) {
            ledger.select(MarketScope(owner,profile.toString().repeat(32)),connection)
            ledger.observe(listOf(shard(),shard(bird,2)),1000)
        }
        val restarted=SavedHuntingBoxLedger(setOf(hide));restarted.restore(ledger.export(),2000)
        restarted.select(scope,connection);assertTrue(restarted.rows().isEmpty())
        restarted.select(MarketScope(owner,"f".repeat(32)),connection)
        assertEquals(listOf(hide),restarted.rows().map {it.first.productId})
    }
    @Test fun `corrupt oversized foreign and future cache data cannot populate rows`() {
        val saved=ledger();saved.observe(listOf(shard()),3000)
        for(raw in listOf("{", "x".repeat(65537),saved.export(),saved.export().replace(hide,"SHARD_FAKE"),saved.export().replace(owner,"not-a-uuid"))) {
            val restarted=SavedHuntingBoxLedger(setOf(hide));restarted.restore(raw,2000);restarted.select(scope,Any())
            assertTrue(restarted.rows().isEmpty())
        }
    }
    @Test fun `box observations include explicit zero while existing slot advice stays positive only`() {
        val slot=MenuSlot(16,"ATTRIBUTE_SHARD","Hideonwall Shard",listOf("Owned: 0 Shards"))
        assertEquals(0,HuntingBoxShardParsing.observe("(3/3) Hunting Box",listOf(slot)).single().amount)
        assertTrue(HuntingBoxShardParsing.parse("(3/3) Hunting Box",listOf(slot)).isEmpty())
    }
}
