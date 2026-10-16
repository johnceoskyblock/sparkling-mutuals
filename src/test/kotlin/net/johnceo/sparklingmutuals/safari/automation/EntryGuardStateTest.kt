package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariEntryPresence
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryGuardStateTest {
    @Test fun `readiness applies only in Torrhus and kick presence only in Safari waiting`() {
        assertTrue(EntryGuardDecision.block(EntryStage.TORRHUS, false, true, true, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.TORRHUS, false, false, true, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, false, true, false, true, false))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, false, false, false, false))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, false, true, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, true, true, true, false, true))
        assertFalse(EntryGuardDecision.block(EntryStage.OTHER, false, true, true, false, true))
    }
    @Test fun `unknown roster or local player list loading cannot permit early entry`() {
        val presence = SafariEntryPresence()
        presence.update(1, true, false, Any(), null, setOf("self"), "self")
        assertFalse(presence.ready)
        presence.update(2, true, false, Any(), setOf("self"), emptySet(), "self")
        assertFalse(presence.ready)
        presence.update(3, true, false, Any(), setOf("self"), setOf("self"), "self")
        assertTrue(presence.ready, "confirmed solo is immediately usable")
    }
    @Test fun `kick does not expire or accept unchanged pre-report presence`() {
        val hold = EntryKickHold(); val world = Any()
        hold.report(world, "friend")
        repeat(100) { hold.observe(world, setOf("self", "friend"), setOf("self", "friend"), true) }
        assertTrue(hold.active)
        hold.observe(world, setOf("self", "friend"), setOf("self"), false)
        assertTrue(hold.active)
        hold.observe(world, setOf("self", "friend"), setOf("self", "friend"), true)
        assertFalse(hold.active)
    }
    @Test fun `kick member removal requires current ready roster and generic report needs new instance`() {
        val world = Any(); val hold = EntryKickHold()
        hold.report(world, "friend")
        hold.observe(world, setOf("self"), setOf("self"), false)
        assertTrue(hold.active)
        hold.observe(world, setOf("self"), setOf("self"), true)
        assertFalse(hold.active)
        hold.report(world, null)
        hold.observe(world, setOf("self"), setOf("self"), true)
        assertTrue(hold.active)
        hold.observe(Any(), null, emptySet(), false)
        assertTrue(hold.active)
        hold.observe(Any(), setOf("self"), setOf("self"), true)
        assertFalse(hold.active)
    }
    @Test fun `another member cycling does not recover the reported victim`() {
        val hold = EntryKickHold(); val world = Any()
        hold.report(world, "friend")
        hold.observe(world, setOf("self", "friend", "other"), setOf("self", "friend"), false)
        hold.observe(world, setOf("self", "friend", "other"), setOf("self", "friend", "other"), true)
        assertTrue(hold.active)
    }
    @Test fun `two reported victims require independent recovery and duplicate report does not erase absence`() {
        val hold = EntryKickHold(); val world = Any(); val members = setOf("self", "a", "b")
        hold.report(world, "a"); hold.report(world, "b")
        hold.observe(world, members, setOf("self", "a"), false)
        hold.observe(world, members, members, true)
        assertTrue(hold.active, "b returning must not clear a's stale evidence")
        hold.observe(world, members, setOf("self", "b"), false)
        hold.report(world, "a")
        hold.observe(world, members, members, true)
        assertFalse(hold.active)
    }
}
