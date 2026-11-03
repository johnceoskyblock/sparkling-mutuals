package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WholeBoxParsingTest {
    private val id="SHARD_VORACIOUS_SPIDER"
    private val catalog=setOf(id,"SHARD_BLUEBIRD","SHARD_BAL")
    private fun slot(index: Int=10,item: String=id,name: String="Voracious Spider",amount: String="12")=
        MenuSlot(index,item,name,listOf("Owned: $amount Shards","RARE COMBAT SHARD (ID R20)"))

    @Test fun `native and generic non Safari shards resolve only in owned inner box slots`() {
        for(item in listOf(id,"ATTRIBUTE_SHARD",""))for(name in listOf("Voracious Spider","Voracious Spider Shard")) {
            val page=WholeBoxParsing.observe("(2/3) Hunting Box",listOf(slot(item=item,name=name)),catalog)!!
            assertEquals(listOf(BoxShard(10,id,12)),page.shards)
            assertEquals(2,page.page);assertEquals(3,page.totalPages)
        }
        assertNull(WholeBoxParsing.observe("Your Bazaar Orders",listOf(slot()),catalog))
        assertTrue(WholeBoxParsing.observe("Hunting Box",listOf(slot(9),slot(44),slot(54)),catalog)!!.shards.isEmpty())
    }
    @Test fun `unpriced native identity is retained and unresolved generic identity is flagged`() {
        assertEquals(id,WholeBoxParsing.observe("Hunting Box",listOf(slot()),emptySet())!!.shards.single().productId)
        val pending=WholeBoxParsing.observe("Hunting Box",listOf(slot(item="ATTRIBUTE_SHARD")),emptySet())!!
        assertTrue(pending.shards.isEmpty());assertEquals(1,pending.unsupported)
    }
    @Test fun `controls conflicting metadata duplicate slots and malformed owned lore cannot count`() {
        val inputs=listOf(slot(item="PAPER"),slot(item="SHARD_BLUEBIRD"),slot(amount="1.5"),
            slot().copy(lore=listOf("Owned: 1 Shard","Owned: 2 Shards")),slot(item="ATTRIBUTE_SHARD").copy(lore=listOf("Owned: 12 Shards")))
        for(input in inputs)assertTrue(WholeBoxParsing.observe("Hunting Box",listOf(input),catalog)!!.shards.isEmpty())
        val duplicate=WholeBoxParsing.observe("Hunting Box",listOf(slot(),slot(amount="1")),catalog)!!
        assertTrue(duplicate.shards.isEmpty());assertTrue(duplicate.unsupported>0)
        assertEquals(0L,WholeBoxParsing.observe("Hunting Box",listOf(slot(amount="0")),catalog)!!.shards.single().amount)
    }
    @Test fun `an invalid second owned line cannot be hidden by a valid first line`() {
        val malformed=slot().copy(lore=listOf("Owned: 12 Shards","Owned: 999999999999999999999999999 Shards"))
        val page=WholeBoxParsing.observe("Hunting Box",listOf(malformed),catalog)!!
        assertTrue(page.shards.isEmpty());assertEquals(1,page.unsupported)
    }
    @Test fun `view fingerprints reflect filters and searches without including page arrows`() {
        val page=WholeBoxParsing.observe("(1/2) Hunting Box",listOf(slot(),MenuSlot(50,"","Next Page",listOf("Click to view page 2"))),catalog)!!
        val other=WholeBoxParsing.observe("(2/2) Hunting Box",listOf(slot(),MenuSlot(50,"","Previous Page",emptyList())),catalog)!!
        assertEquals(page.view,other.view)
        val filtered=WholeBoxParsing.observe("(1/2) Hunting Box",listOf(slot(),MenuSlot(48,"","Search",listOf("Query: Spider"))),catalog)!!
        assertTrue(filtered.filtered);assertNotEquals(page.view,filtered.view)
        val conflict=WholeBoxParsing.observe("Hunting Box",listOf(slot(),slot(11,amount="13")),catalog)!!
        assertTrue(conflict.shards.isEmpty());assertTrue(conflict.unsupported>0)
    }
}
