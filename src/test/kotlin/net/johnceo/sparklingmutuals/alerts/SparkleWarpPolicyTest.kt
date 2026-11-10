package net.johnceo.sparklingmutuals.alerts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SparkleWarpPolicyTest {
    @Test fun `start can use a complete server party list before names resolve from UUIDs`() {
        val roster = StartPartyRoster()
        roster.observe("Party Members (4)")
        roster.observe("Party Leader: [VIP+] Leader ●")
        roster.observe("Party Moderators: [MVP++] Runner ●")
        assertNull(roster.members("Leader", 4))
        roster.observe("Party Members: [MVP++] OldA ● [MVP+] OldB ●")
        assertEquals(listOf("Leader", "Runner", "OldA", "OldB"), roster.members("Leader", 4))
        assertNull(roster.members("Runner", 4))
        assertNull(roster.members("Leader", 3))
    }
    @Test fun `planned leechers fill open requirement slots and contest pause belongs only to the warping leader`() {
        val saved=linkedSetOf("Leader","Runner","LeechA","LeechB")
        assertEquals(saved,SparkleWarpPolicy.requirementRoster(linkedSetOf("Leader","Runner"),saved))
        assertEquals(setOf("Leader","Runner","Priority","LeechA"),
            SparkleWarpPolicy.requirementRoster(linkedSetOf("Leader","Runner","Priority"),saved))
        val full=linkedSetOf("Leader","Runner","Priority","New")
        assertEquals(full,SparkleWarpPolicy.requirementRoster(full,saved))
        assertTrue(SparkleWarpPolicy.contestPause(true,false,true,false))
        assertTrue(SparkleWarpPolicy.contestPause(true,false,null,false))
        assertFalse(SparkleWarpPolicy.contestPause(true,false,true,true))
        assertFalse(SparkleWarpPolicy.contestPause(false,false,true,false))
        assertFalse(SparkleWarpPolicy.contestPause(true,false,false,false))
        assertFalse(SparkleWarpPolicy.contestPause(true,true,true,false))
    }
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
    @Test fun `only solo or leaders with room start a window and fallback begins exactly at twenty-four seconds`() {
        for (n in 0..3) assertTrue(SparkleWarpPolicy.canStart(true,n))
        assertFalse(SparkleWarpPolicy.canStart(true,4))
        assertFalse(SparkleWarpPolicy.canStart(false,2))
        assertFalse(SparkleWarpPolicy.returningPhase(1000,24999))
        assertTrue(SparkleWarpPolicy.returningPhase(1000,25000))
        val invites=WarpInvitations()
        invites.start(25000)
        invites.add("Flitter",listOf("Priority","Other"))
        assertEquals("Priority",invites.next(1000,emptySet()))
        invites.discardPending()
        assertNull(invites.next(21000,emptySet()),"Returning players bypass the paced Warp List queue")
        assertEquals("p OldA OldB OldC",SparkleWarpPolicy.returningCommand(listOf("OldA","OldB","OldC"),setOf("Leader"),1))
        assertEquals("p OldA OldB",SparkleWarpPolicy.returningCommand(listOf("OldA","OldB","OldC"),setOf("Leader","Priority"),2))
        assertEquals("p OldB",SparkleWarpPolicy.returningCommand(listOf("OldA","OldB","oldb","Bad Name"),setOf("Leader","Runner","OldA"),3))
        assertNull(SparkleWarpPolicy.returningCommand(listOf("OldA"),setOf("Leader","Runner","Third","Fourth"),4))
        assertNull(invites.next(25000,emptySet()))
    }
    @Test fun `returning party retry targets only missing players before the warp deadline`() {
        val invites = ReturningPartyInvites()
        val names = listOf("OldA", "OldB")
        assertEquals("p OldA OldB", invites.next(20_000, 25_000, names, setOf("Leader"), 1))
        assertNull(invites.next(21_999, 25_000, names, setOf("Leader"), 1))
        assertEquals("p OldB", invites.next(22_000, 25_000, names, setOf("Leader", "OldA"), 2))
        assertNull(invites.next(24_000, 25_000, names, setOf("Leader", "OldA"), 2))
        invites.reset()
        assertNull(invites.next(20_000, 25_000, names, setOf("Leader", "OldA", "OldB"), 3))
        assertEquals("p OldB", invites.next(24_000, 25_000, names, setOf("Leader", "OldA"), 2))
        assertNull(invites.next(26_000, 25_000, names, setOf("Leader"), 1))
    }
    @Test fun `leech accepts actual multiline server invitations but never player quotes`() {
        val invite="-----------------------------------------------------\\n[MVP++] _cj has invited you to join their party!\\nYou have 60 seconds to accept. Click here to join!\\n-----------------------------------------------------"
        assertEquals("_cj",SparkleWarpPolicy.inviter(invite))
        assertTrue(SparkleWarpPolicy.inviteAllowed("_CJ","JohnCEO, _cj; JohnFounder"))
        assertFalse(SparkleWarpPolicy.inviteAllowed("NotListed","JohnCEO _cj"))
        assertFalse(SparkleWarpPolicy.inviteAllowed("John","JohnCEO"))
        assertFalse(SparkleWarpPolicy.inviteAllowed("JohnCEO",""))
        assertNull(SparkleWarpPolicy.inviter("Party > Friend: [MVP++] _cj has invited you to join their party!"))
        assertNull(SparkleWarpPolicy.inviter("[MVP+] _cj: Other has invited you to join their party!"))
        val pending=LeechInvitation()
        pending.offer("_cj",0)
        assertNull(pending.next(0,null))
        assertEquals("p leave",pending.next(1,true))
        assertNull(pending.next(2,true))
        pending.leftParty()
        assertNull(pending.next(3,null))
        assertNull(pending.next(50,false))
        assertEquals("p accept _cj",pending.next(501,false))
        assertNull(pending.next(502,false))
        pending.offer("Other",5)
        assertEquals("p accept Other",pending.next(5,false))
        pending.offer("Expired",0)
        assertNull(pending.next(15000,false))
        pending.offer("Cancelled",0);pending.clear()
        assertNull(pending.next(1,false))
    }
    @Test fun `leech stays with a whitelisted leader and waits for unresolved leaders`() {
        val pending = LeechInvitation()
        pending.offer("Other", 0)
        assertNull(pending.next(1, true, protectedLeader = null))
        assertTrue(pending.pending)
        assertNull(pending.next(2, true, protectedLeader = true))
        assertFalse(pending.pending)
    }
}
