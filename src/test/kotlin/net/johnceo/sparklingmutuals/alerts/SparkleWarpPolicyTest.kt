package net.johnceo.sparklingmutuals.alerts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparkleWarpPolicyTest {
    @Test fun `solo disbands and remembers others while duo keeps its selected runner`() {
        val members=listOf("Leader","Runner","LeechA","LeechB")
        val solo=SparkleWarpPolicy.cleanup(SparkleWarpMode.SOLO,"leader","",members)!!
        assertEquals(listOf("p disband"),solo.commands)
        assertEquals(listOf("Runner","LeechA","LeechB"),solo.leechers)
        val duo=SparkleWarpPolicy.cleanup(SparkleWarpMode.DUO,"Leader","runner",members)!!
        assertEquals(listOf("p kick LeechA","p kick LeechB"),duo.commands)
        assertEquals(listOf("LeechA","LeechB"),duo.leechers)
        assertNull(SparkleWarpPolicy.cleanup(SparkleWarpMode.DUO,"Leader","Missing",members))
        assertNull(SparkleWarpPolicy.cleanup(SparkleWarpMode.DUO,"Leader","Leader",members))
    }
    @Test fun `only solo or leaders with room start a window and fallback begins exactly at nineteen seconds`() {
        for (n in 0..3) assertTrue(SparkleWarpPolicy.canStart(true,n))
        assertFalse(SparkleWarpPolicy.canStart(true,4))
        assertFalse(SparkleWarpPolicy.canStart(false,2))
        assertFalse(SparkleWarpPolicy.returningPhase(1000,19999))
        assertTrue(SparkleWarpPolicy.returningPhase(1000,20000))
        val invites=WarpInvitations()
        invites.start(25000)
        invites.add("Flitter",listOf("Priority","Other"))
        assertEquals("Priority",invites.next(1000,emptySet()))
        invites.discardPending()
        invites.add("returning leechers",listOf("OldA","OldB","OldC"))
        assertEquals("OldA",invites.next(19000,setOf("Priority")))
        assertNull(invites.next(20999,setOf("Priority","OldA")))
        assertEquals("OldB",invites.next(21000,setOf("Priority","OldA")))
        assertEquals("OldC",invites.next(23000,setOf("Priority","OldA","OldB")))
        assertNull(invites.next(25000,emptySet()))
    }
    @Test fun `leech accepts actual multiline server invitations but never player quotes`() {
        val invite="-----------------------------------------------------\\n[MVP++] _cj has invited you to join their party!\\nYou have 60 seconds to accept. Click here to join!\\n-----------------------------------------------------"
        assertEquals("_cj",SparkleWarpPolicy.inviter(invite))
        assertNull(SparkleWarpPolicy.inviter("Party > Friend: [MVP++] _cj has invited you to join their party!"))
        assertNull(SparkleWarpPolicy.inviter("[MVP+] _cj: Other has invited you to join their party!"))
        val pending=LeechInvitation()
        pending.offer("_cj",0)
        assertNull(pending.next(0,null))
        assertEquals("p leave",pending.next(1,true))
        assertNull(pending.next(2,true))
        pending.leftParty()
        assertEquals("p accept _cj",pending.next(3,null))
        assertNull(pending.next(4,false))
        pending.offer("Other",5)
        assertEquals("p accept Other",pending.next(5,false))
        pending.offer("Expired",0)
        assertNull(pending.next(15000,false))
        pending.offer("Cancelled",0);pending.clear()
        assertNull(pending.next(1,false))
    }
}
