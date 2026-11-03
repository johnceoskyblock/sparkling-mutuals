package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HuntingBoxLabelCompatibilityTest {
    private val liveLore=listOf("Shulker Family","Owned: 243 Shards","Attribute Maxed!",
        "Right-click to convert to an item!","RARE FOREST SHARD (ID R79)")
    private fun entry(id: String="",name: String="§9Hideonwall",lore: List<String> = liveLore)=MenuSlot(16,id,name,lore)

    @Test fun `live box short label resolves without a carried shard suffix`() {
        for(id in listOf("","ATTRIBUTE_SHARD")) {
            val shard=HuntingBoxShardParsing.parse("(3/3) Hunting Box",listOf(entry(id))).single()
            assertEquals("SHARD_HIDEONWALL",shard.productId)
            assertEquals(243,shard.amount)
        }
    }
    @Test fun `short menu label never overrides conflicting metadata or weak unrelated lore`() {
        for(id in listOf("STONE","SHARD_UNKNOWN","SHARD_BLUEBIRD"))
            assertTrue(HuntingBoxShardParsing.parse("(3/3) Hunting Box",listOf(entry(id))).isEmpty())
        for(lore in listOf(listOf("Owned: 243 Shards"),liveLore.filterNot {it.startsWith("Owned:")},liveLore+"Owned: 5 Shards",
            liveLore+"RARE FOREST SHARD (ID R79)",liveLore.map {it.replace("(ID R79)","(ID unknown)")}))
            assertTrue(HuntingBoxShardParsing.parse("(3/3) Hunting Box",listOf(entry(lore=lore))).isEmpty())
        assertTrue(HuntingBoxShardParsing.parse("Your Bazaar Orders",listOf(entry())).isEmpty())
        assertTrue(HuntingBoxShardParsing.parse("(3/3) Hunting Box",listOf(entry(name="Fake Hideonwall"))).isEmpty())
    }
    @Test fun `recognized menu slots reach unavailable advice before history is ready`() {
        val session=SaleGuidanceSession(SellOfferWatch(),{_,_,_,_->},{_,_->},profitProducts={setOf("SHARD_HIDEONWALL")})
        val view=session.tick(true,true,null,"Local","(3/3) Hunting Box",listOf(entry()),emptyMap(),1000)
        assertEquals(AdviceState.UNKNOWN,view.getValue(16).state)
        assertEquals("HISTORY_COVERAGE",view.getValue(16).reason)
    }
}
