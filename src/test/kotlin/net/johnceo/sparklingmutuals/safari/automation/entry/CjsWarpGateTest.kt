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
        g.world(Any(),600);assertFalse(g.allows(EntryFixtures.context(),listOf(EntryPeer(EntryFixtures.leader,"Leader")),10500))
    }
}
