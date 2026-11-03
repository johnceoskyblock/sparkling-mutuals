package net.johnceo.sparklingmutuals.skyblock.market

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SaleProfileConfirmationTest {
    private val id="11111111-2222-3333-4444-555555555555"
    private val compact=id.replace("-","")
    private fun reply(value: String=id,label: String="DASHES")=Component.literal("CLICK THIS TO SUGGEST IT IN CHAT [$label]")
        .withStyle {it.withClickEvent(ClickEvent.SuggestCommand(value))}
    @Test fun `current clickable profile labels read UUID data through inherited and split styles`() {
        for(message in listOf(reply(),reply(compact,"NO DASHES"),Component.empty().withStyle {
            it.withClickEvent(ClickEvent.SuggestCommand(id))}.append(Component.literal("CLICK THIS TO SUGGEST "))
            .append(Component.literal("IT IN CHAT [DASHES]"))))assertEquals(compact,SaleProfileEvidence.profile(message))
        assertEquals(compact,SaleProfileEvidence.profile(Component.literal("Profile ID: $id")))
    }
    @Test fun `lookalikes wrong actions invalid UUIDs and conflicting click identities are rejected`() {
        val conflict=Component.literal("CLICK THIS TO SUGGEST IT IN CHAT ").withStyle {it.withClickEvent(ClickEvent.SuggestCommand(id))}
            .append(Component.literal("[DASHES]").withStyle {it.withClickEvent(ClickEvent.SuggestCommand("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))})
        for(message in listOf(reply("/profileid"),reply("not-an-id"),reply("$id extra"),reply(id,"UNKNOWN"),
            Component.literal("Party > Player: ").append(reply()),Component.literal("CLICK THIS TO SUGGEST IT IN CHAT [DASHES]"),
            reply().withStyle {it.withClickEvent(ClickEvent.RunCommand(id))},conflict))assertNull(SaleProfileEvidence.profile(message))
    }
    @Test fun `automatic confirmation is delayed bounded and only accepts pending valid replies`() {
        val requests=SaleProfileConfirmation()
        assertFalse(requests.next(true,false,1000));assertFalse(requests.next(true,false,1999))
        assertTrue(requests.next(true,false,2000));assertFalse(requests.next(true,false,2001))
        assertNull(requests.accept(reply("bad"),2002));assertEquals(compact,requests.accept(reply(),2003))
        assertNull(requests.accept(reply(),2004));assertFalse(requests.next(true,true,3000))
    }
    @Test fun `timeouts retry at most three times and reset drains delayed old replies`() {
        val requests=SaleProfileConfirmation()
        requests.next(true,false,1000);assertTrue(requests.next(true,false,2000))
        assertNull(requests.accept(reply(),17001));assertFalse(requests.next(true,false,31999))
        assertTrue(requests.next(true,false,32000));assertTrue(requests.next(true,false,62000))
        assertFalse(requests.next(true,false,92000));assertFalse(requests.next(true,false,1000000))
        val changed=SaleProfileConfirmation();changed.next(true,false,1000);changed.next(true,false,2000)
        changed.reset(3000);assertNull(changed.accept(reply(),3001));assertFalse(changed.next(true,false,16000))
        assertTrue(changed.next(true,false,18000))
    }
    @Test fun `disable unknown context and backward clock cannot accept or spam requests`() {
        val requests=SaleProfileConfirmation()
        assertFalse(requests.next(false,false,1000));assertNull(requests.accept(reply(),1001))
        requests.next(true,false,2000);assertTrue(requests.next(true,false,3000))
        assertFalse(requests.next(false,false,4000));assertNull(requests.accept(reply(),4001))
        assertFalse(requests.next(true,false,4500));assertFalse(requests.next(true,false,2000))
        assertNull(requests.accept(reply(),2001))
    }
    @Test fun `repeated context resets retain the old response drain interval`() {
        val requests=SaleProfileConfirmation();requests.next(true,false,1000);requests.next(true,false,2000)
        requests.reset(3000);requests.reset(4000)
        assertFalse(requests.next(true,false,10000))
        assertFalse(requests.next(true,false,11000))
        assertNull(requests.accept(reply(),11001))
        assertTrue(requests.next(true,false,18000))
    }
}
