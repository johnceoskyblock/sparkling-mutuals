package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SellOfferWatchTest {
    private val scope=MarketScope("1".repeat(32),"2".repeat(32))
    private val id="SHARD_BLUEBIRD"
    private val now=MarketFixtures.NOW
    private fun advice(state: AdviceState,ask: Double=100.0,key: String="range")=Recommendation(id,state,100.0,now,"EXPERIMENTAL_RANGE",ask,key)
    private fun watch()=SellOfferWatch().apply {selectScope(scope);observe(listOf(OfferEvidence(id,120.0,100,false)),now)}
    @Test fun `fresh observed offers produce one grouped confirmed decline unlist suggestion`() {
        val w=watch()
        assertTrue(w.update(mapOf(id to advice(AdviceState.SELL)),now).isEmpty())
        assertEquals(1,w.update(mapOf(id to advice(AdviceState.HOLD,98.0)),now+120000).size)
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,97.0)),now+180000).isEmpty())
        val text=watch().apply {update(mapOf(id to advice(AdviceState.SELL)),now)}
            .update(mapOf(id to advice(AdviceState.HOLD,98.0)),now+120000).single().message
        assertTrue(text.contains("Consider unlisting"));assertTrue(text.contains("last observed"));assertTrue(text.contains("HOLD"))
    }
    @Test fun `unknown recovery target changes and rising ask never fabricate price drops`() {
        val w=watch();w.update(mapOf(id to advice(AdviceState.SELL)),now)
        assertTrue(w.update(mapOf(id to advice(AdviceState.UNKNOWN)),now+60000).isEmpty())
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,98.0)),now+120000).isEmpty())
        w.update(mapOf(id to advice(AdviceState.SELL)),now+180000)
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,101.0)),now+240000).isEmpty())
        w.update(mapOf(id to advice(AdviceState.SELL)),now+300000)
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,98.0,"new-model")),now+360000).isEmpty())
    }
    @Test fun `initial hold is silent and cooldown requires a return to sell`() {
        val w=watch()
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,98.0)),now).isEmpty())
        w.update(mapOf(id to advice(AdviceState.SELL)),now+60000)
        assertEquals(1,w.update(mapOf(id to advice(AdviceState.HOLD,97.0)),now+120000).size)
        assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,96.0)),now+180000).isEmpty())
        w.update(mapOf(id to advice(AdviceState.SELL)),now+660000)
        assertEquals(1,w.update(mapOf(id to advice(AdviceState.HOLD,96.0)),now+720000).size)
    }
    @Test fun `legacy initial notice flags never revive initial hold alerts or establish a transition`() {
        for(flag in listOf(false,true)) {
            val raw="""{"schema":1,"scopes":[{"account":"${scope.accountId}","profile":"${scope.profileId}","watches":[["$id",$now,$flag,0]]}]}"""
            val w=SellOfferWatch().apply {restore(raw,now);selectScope(scope);observe(listOf(OfferEvidence(id,100.0,2,false)),now)}
            assertTrue(w.update(mapOf(id to advice(AdviceState.HOLD,98.0)),now).isEmpty())
            w.update(mapOf(id to advice(AdviceState.SELL)),now+60000)
            assertEquals(1,w.update(mapOf(id to advice(AdviceState.HOLD,97.0)),now+120000).size)
        }
    }
    @Test fun `pagination and ambiguous completion never cancel another offer and profiles never mix`() {
        val w=watch();w.observe(emptyList(),now+1000)
        w.observe(listOf(OfferEvidence(id,120.0,100,true)),now+2000)
        assertEquals(setOf(id),w.products(now+2000))
        w.selectScope(null);assertTrue(w.products(now+3000).isEmpty())
        w.selectScope(scope.copy(profileId="3".repeat(32)));assertTrue(w.products(now+4000).isEmpty())
        w.selectScope(scope);assertTrue(w.products(now+5000).isEmpty(),"Returning requires live revalidation")
    }
    @Test fun `persisted state is bounded validated and always needs new live order evidence`() {
        val w=watch();w.update(mapOf(id to advice(AdviceState.HOLD)),now)
        val raw=w.export(now)
        val restored=SellOfferWatch().apply {restore(raw,now);selectScope(scope)}
        assertTrue(restored.products(now).isEmpty())
        restored.observe(listOf(OfferEvidence(id,100.0,20,false)),now)
        assertTrue(restored.update(mapOf(id to advice(AdviceState.HOLD)),now+1000).isEmpty(),"Initial notice persisted")
        for(bad in listOf("{}","broken",raw.replace(id,"SHARD_FAKE"),"x".repeat(1024*1024+1))) {
            val copy=SellOfferWatch();copy.restore(bad,now);copy.selectScope(scope)
            assertTrue(copy.products(now).isEmpty())
        }
        repeat(6) {n -> w.selectScope(scope.copy(profileId=n.toString().repeat(32)));w.observe(listOf(OfferEvidence(id,100.0,1,false)),now+n)}
        assertTrue(com.google.gson.JsonParser.parseString(w.export(now+100)).asJsonObject.getAsJsonArray("scopes").size()<=4)
    }
}
