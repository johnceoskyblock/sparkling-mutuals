package net.johnceo.sparklingmutuals.skyblock.market

import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarUnlistEvidenceTest {
    private fun row(index: Int=10,owner: String?=null)=MenuSlot(index,"SHARD_BLUEBIRD","§6§lSELL §6Bluebird Shard",
        listOf("Offer amount: 50x","Price per unit: 100 coins")+(owner?.let {listOf("By: $it")} ?: emptyList()))
    @Test fun `slot evidence accepts personal fallback and case insensitive own names but never ambiguous co op ownership`() {
        assertTrue(SellOfferParsing.isOrders("§aYour Bazaar Orders"))
        assertFalse(SellOfferParsing.isOrders("Bazaar"))
        assertEquals(10,SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row()),"Player").single().slot)
        assertEquals(1,SellOfferParsing.parseSlots("Co-op Bazaar Orders",listOf(row(owner="[MVP+] pLaYeR")),"Player").size)
        for(owner in listOf(null,"Other","Player extra",""))assertTrue(
            SellOfferParsing.parseSlots("Co-op Bazaar Orders",listOf(row(owner=owner)),"Player").isEmpty())
        assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row(owner="Other")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row().copy(lore=row().lore+listOf("By: Player","By: Player"))),"Player").isEmpty())
    }
    @Test fun `same product distinct offers retain slots while duplicate slots controls and buy markers are rejected`() {
        assertEquals(listOf(10,11),SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row(),row(11)),"Player").map {it.slot})
        assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row(),row()),"Player").isEmpty())
        assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row().copy(name="§a§lBUY §aBluebird Shard")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",listOf(row().copy(itemId="SHARD_SNOOZLE")),"Player").isEmpty())
        assertTrue(SellOfferParsing.parseSlots("Order options",listOf(row()),"Player").isEmpty())
    }
    @Test fun `component bold SELL styling is preserved across inherited and split text without inventing a marker`() {
        val bold=Component.literal("SELL ").withStyle(ChatFormatting.BOLD).append(Component.literal("Bluebird Shard"))
        val split=Component.empty().withStyle(ChatFormatting.BOLD).append(Component.literal("SE")).append(Component.literal("LL Bluebird Shard"))
        for(name in listOf(bold,split)) {
            val captured=MarketSlotName.capture(name)
            assertEquals("§lSELL Bluebird Shard",captured)
            assertEquals(1,SellOfferParsing.parse("Your Bazaar Orders",listOf(row().copy(name=captured)),"Player").size)
        }
        assertEquals("SELL Bluebird Shard",MarketSlotName.capture(Component.literal("SELL Bluebird Shard")))
        val partly=Component.literal("SEL").withStyle(ChatFormatting.BOLD)
            .append(Component.literal("L Bluebird Shard").withStyle {it.withBold(false)})
        assertFalse(MarketSlotName.capture(partly).startsWith("§l"))
        assertEquals(row().name,MarketSlotName.capture(Component.literal(row().name)))
        assertEquals("BUY Bluebird Shard",MarketSlotName.capture(Component.literal("BUY Bluebird Shard").withStyle(ChatFormatting.BOLD)))
    }
    @Test fun `grouped suggestion names each known product once and refers to last observed offers`() {
        val text=BazaarUnlistPresentation.message(setOf("SHARD_HIDEONWALL","SHARD_BLUEBIRD","NOT_A_SHARD"))
        assertNotNull(text)
        assertTrue(text!!.contains("Bluebird, Hideonwall"))
        assertTrue(text.contains("HOLD"));assertTrue(text.contains("last observed"));assertTrue(text.contains("active"))
        assertNull(BazaarUnlistPresentation.message(emptySet()))
        assertNull(BazaarUnlistPresentation.message(setOf("NOT_A_SHARD")))
    }
    @Test fun `mixed legacy and structured SELL letters require effective bold on every visible letter`() {
        val mixed=Component.literal("SE").withStyle(ChatFormatting.BOLD)
            .append(Component.literal("§lLL Bluebird Shard"))
        val reversed=Component.literal("§lSE").append(Component.literal("LL Bluebird Shard").withStyle(ChatFormatting.BOLD))
        for(name in listOf(mixed,reversed)) {
            assertEquals(1,SellOfferParsing.parseSlots("Your Bazaar Orders",
                listOf(row().copy(name=MarketSlotName.capture(name))),"Player").size)
        }
        val plainStart=Component.literal("SE").append(Component.literal("§lLL Bluebird Shard"))
        val reset=Component.literal("§lSE§rLL Bluebird Shard")
        val colorReset=Component.literal("SE").withStyle(ChatFormatting.BOLD)
            .append(Component.literal("§6LL Bluebird Shard"))
        for(name in listOf(plainStart,reset,colorReset))assertTrue(SellOfferParsing.parseSlots("Your Bazaar Orders",
            listOf(row().copy(name=MarketSlotName.capture(name))),"Player").isEmpty())
    }
}
