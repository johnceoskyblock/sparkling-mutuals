package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsWarpGateTest {
    @Test fun `server party warp binds the next world and the verified leader profile`() {
        val g=CjsWarpGate();val hub=Any();val safari=Any();g.world(hub,0)
        g.observe("Party Leader, [MVP++] Leader, summoned you to their server.",100)
        assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),101))
        g.world(safari,200)
        assertFalse(g.allows(EntryFixtures.context(),emptyList(),210))
        assertTrue(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),220))
        g.world(Any(),300)
        assertFalse(g.allows(EntryFixtures.context().copy(world=2,arrival=2),listOf(EntryPeer(EntryFixtures.leader,"Leader")),310))
    }
    @Test fun `quotes expiry wrong leader and changed verified context cannot grant movement`() {
        val g=CjsWarpGate();g.world(Any(),0)
        g.observe("Party > Friend: Party Leader, Leader, summoned you to their server.",100)
        g.world(Any(),200);assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),210))
        g.observe("Party Leader, Leader, summoned you to their server.",300)
        g.world(Any(),400)
        assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Other")),410))
        assertTrue(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),420))
        assertFalse(g.allows(EntryFixtures.context().copy(server="miniOther"),listOf(EntryPeer(EntryFixtures.leader,"Leader")),430))
        assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),440))
        g.observe("Party Leader, Leader, summoned you to their server.",500)
        g.world(Any(),600);assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),12600))
    }
    @Test fun `bound arrival keeps authority for the full attempt despite slower party loading`() {
        val g=CjsWarpGate();g.world(Any(),0)
        g.observe("Party Leader, Leader, summoned you to their server.",100)
        g.world(Any(),650)
        val c=EntryFixtures.context();val peers=listOf(EntryPeer(EntryFixtures.leader,"Leader"))
        assertTrue(g.allows(c,peers,5422))
        val flow=CjsEntryFlow();val signal=CjsRoomSignal(c,7,setOf("Bluebird"))
        val frame=CjsEntryFrame(c,signal,1,true,650,true,true,false,true,true,
            manager=java.util.UUID(0,99))
        assertTrue(flow.tick(frame,5422) is CjsEntryEffect.Approach)
        // The old notice-based ten-second lease dies 9.45 seconds after arrival.
        assertTrue(g.allows(c,peers,10101))
        assertTrue(flow.tick(frame.copy(warpAllowed=g.allows(c,peers,10101)),10101) is CjsEntryEffect.Approach)
        assertTrue(g.allows(c,peers,12649))
        assertFalse(g.allows(c,peers,12650))
    }
    @Test fun `late transfer and duplicate notice cannot extend arrival authority`() {
        val g=CjsWarpGate();g.world(Any(),0)
        g.observe("Party Leader, Leader, summoned you to their server.",100)
        g.world(Any(),10100)
        val c=EntryFixtures.context();val peers=listOf(EntryPeer(EntryFixtures.leader,"Leader"))
        assertFalse(g.allows(c,peers,10101))
        val h=CjsWarpGate();h.world(Any(),0);h.observe("Party Leader, Leader, summoned you to their server.",100)
        h.world(Any(),650)
        assertTrue(h.allows(c,peers,1000))
        h.observe("Party Leader, Leader, summoned you to their server.",9000)
        assertFalse(h.allows(c,peers,12650))
    }
}
