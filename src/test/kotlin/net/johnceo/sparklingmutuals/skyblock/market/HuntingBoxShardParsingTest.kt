package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HuntingBoxShardParsingTest {
    private fun slot(index: Int=10,id: String="SHARD_HIDEONWALL",name: String="Hideonwall Shard",lore: List<String> = listOf("Owned: 1,234 Shards"))=MenuSlot(index,id,name,lore)
    @Test fun `only exact box titles and owned server identities produce slots`() {
        for(title in listOf("Hunting Box","(2/4) Hunting Box","Hunting Box (Page 2/4)","Hunting Box (Page 2 of 4)")) {
            assertEquals(1234,HuntingBoxShardParsing.parse(title,listOf(slot())).single().amount)
        }
        for(title in listOf("Someone's Hunting Box","Hunting Box loading","Your Skills"))assertTrue(HuntingBoxShardParsing.parse(title,listOf(slot())).isEmpty())
        for(index in listOf(0,9,17,45,54,80))assertTrue(HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(index))).isEmpty())
        for(id in listOf("","STONE","SHARD_UNKNOWN","ESSENCE_SAFARI"))assertTrue(HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(id=id))).isEmpty())
        assertTrue(HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(lore=listOf("Owned: many Shards")))).isEmpty())
    }
    @Test fun `legacy attribute shards require exact roster names and duplicate slot ambiguity is rejected`() {
        assertEquals("SHARD_MANTIS_SHRIMP",HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(id="ATTRIBUTE_SHARD",name="§9Mantis Shrimp Shard"))).single().productId)
        assertTrue(HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(id="ATTRIBUTE_SHARD",name="Fake Shard"))).isEmpty())
        assertTrue(HuntingBoxShardParsing.parse("Hunting Box",listOf(slot(),slot(id="SHARD_BLUEBIRD"))).isEmpty())
        assertTrue(HuntingBoxShardParsing.parse("Hunting Box",emptyList()).isEmpty())
    }
    @Test fun `profile identity needs full exact server line and local account evidence`() {
        val id="11111111-2222-3333-4444-555555555555"
        assertEquals("11111111222233334444555555555555",SaleProfileEvidence.profile("Profile ID: $id"))
        for(line in listOf("[MVP+] Player: Profile ID: $id","Party > Profile ID: $id","Profile ID: not-an-id","Profile ID: $id extra"))assertNull(SaleProfileEvidence.profile(line))
        assertNull(SaleProfileEvidence.scope(null,id))
        assertNull(SaleProfileEvidence.scope(id,null))
        assertNotNull(SaleProfileEvidence.scope(id,id))
        assertTrue(SaleProfileEvidence.changing("You are playing on profile: Apple"))
        assertTrue(SaleProfileEvidence.changing("Your profile was changed to: Banana"))
        assertTrue(SaleProfileEvidence.changing("You are playing on profile: Apple (Co-op) extra suffix"),"Even an unknown login suffix must invalidate old scope")
        assertFalse(SaleProfileEvidence.changing("Player: You are playing on profile: Apple"))
    }
}
