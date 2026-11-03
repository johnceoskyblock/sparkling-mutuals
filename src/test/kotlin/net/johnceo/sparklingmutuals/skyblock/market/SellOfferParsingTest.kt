package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Constructed combinations of pinned source-backed fields, not a captured live server tooltip. */
class SellOfferParsingTest {
    private fun row(name: String="§6§lSELL §6Bluebird Shard",id: String="",lore: List<String> = listOf("§7Offer amount: §a2,000x","§7Price per unit: §6521.7 coins"))=MenuSlot(10,id,name,lore)
    @Test fun `explicit unit price sell marker and quantity are separate from totals and buy orders`() {
        val parsed=SellOfferParsing.parse("Your Bazaar Orders",listOf(row(lore=listOf("Offer amount: 2,000x","Price per unit: 521.7 coins","Total: 1,043,400 coins","Filled: 26/50 (52%)"))),"Player")
        assertEquals(521.7,parsed.single().unitPrice);assertEquals(2000,parsed.single().quantity);assertFalse(parsed.single().completed)
        assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(row(name="§a§lBUY §aBluebird Shard")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(row(name="SELL Bluebird Shard")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parse("Order options",listOf(row()),"Player").isEmpty())
    }
    @Test fun `co op rows require exact local owner and unknown or conflicting identities are ignored`() {
        assertTrue(SellOfferParsing.parse("Co-op Bazaar Orders",listOf(row()),"Player").isEmpty())
        val owned=row(lore=listOf("Offer amount: 50x","Price per unit: 100 coins","By: [MVP+] Player"))
        assertEquals(1,SellOfferParsing.parse("Co-op Bazaar Orders",listOf(owned),"Player").size)
        assertTrue(SellOfferParsing.parse("Co-op Bazaar Orders",listOf(owned),"Other").isEmpty())
        assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(row(id="SHARD_HIDEONWALL")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(row(name="§6§lSELL §6Fake Shard")),"Player").isEmpty())
    }
    @Test fun `only explicit completion counts and duplicate same product offers remain separate observations`() {
        val complete=row(lore=listOf("Offer amount: 59x","Price per unit: 100 coins","Filled: 59/59 100%!"))
        assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(complete),"Player").single().completed)
        val rounded=row(lore=listOf("Offer amount: 50x","Price per unit: 100 coins","Filled: 50/50 (99.9%)"))
        assertFalse(SellOfferParsing.parse("Your Bazaar Orders",listOf(rounded),"Player").single().completed)
        assertEquals(2,SellOfferParsing.parse("Your Bazaar Orders",listOf(row(),row().copy(index=11)),"Player").size)
        for(lore in listOf(listOf("Offer amount: 50x","Price: 100 coins"),listOf("Offer amount: 50x","Price per unit: NaN coins"),listOf("Offer amount: 50x","Price per unit: 100 coins","Price per unit: 200 coins")))
            assertTrue(SellOfferParsing.parse("Your Bazaar Orders",listOf(row(lore=lore)),"Player").isEmpty())
    }
}
