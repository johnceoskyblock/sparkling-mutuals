package net.johnceo.sparklingmutuals.safari.automation

import net.johnceo.sparklingmutuals.safari.SafariEntryPresence
import net.johnceo.sparklingmutuals.safari.SafariEntryRunState
import net.johnceo.sparklingmutuals.safari.SafariLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryGuardStateTest {
    @Test fun `readiness applies in Torrhus and kick presence applies to all Safari entry attempts`() {
        assertTrue(EntryGuardDecision.block(EntryStage.TORRHUS, true, true, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.TORRHUS, false, true, true, false))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, true, false, true, false))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, false, false, false))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, true, true, false))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, true, true, false, true))
        assertFalse(EntryGuardDecision.block(EntryStage.OTHER, true, true, false, true))
    }
    @Test fun `entry stays blocked indefinitely across run detection until every party member returns`() {
        val presence = SafariEntryPresence()
        val hold = EntryKickHold()
        val run = SafariEntryRunState()
        val world = Any()
        val members = setOf("self", "friend", "other")
        hold.report(world, "friend")
        fun click(now: Long, present: Set<String>): Boolean {
            presence.update(now, true, false, world, members, present, "self")
            hold.observe(world, members, present, presence.ready)
            return EntryGuardDecision.block(EntryStage.SAFARI, false, hold.active, presence.ready, false)
        }
        assertFalse(run.active(world, true, SafariLocation.UNKNOWN, null))
        assertTrue(click(0, setOf("self", "other")))
        assertTrue(run.active(world, true, SafariLocation.INSIDE, 0))
        repeat(100) { assertTrue(click(600_000L + it * 1_000, setOf("self", "other"))) }
        assertTrue(click(800_000, setOf("self", "friend")), "another missing member must keep entry blocked")
        assertFalse(click(900_000, members))
        assertTrue(click(1_000_000, setOf("self", "other")), "a later departure must block again")
        assertFalse(click(1_100_000, members))
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
    @Test fun `kick member removal requires current ready roster and new instance requires full presence`() {
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
    @Test fun `generic kick recovers on observed full party return in the same instance`() {
        val world = Any(); val hold = EntryKickHold(); val members = setOf("self", "a", "b")
        hold.report(world, null)
        repeat(100) { hold.observe(world, members, members, true) }
        assertTrue(hold.active, "unchanged pre-report presence cannot prove recovery")
        hold.observe(world, members, setOf("self"), false)
        hold.report(world, null)
        hold.observe(world, members, setOf("self", "a"), false)
        assertTrue(hold.active, "partial recovery cannot permit entry")
        hold.observe(world, null, members, false)
        assertTrue(hold.active, "unknown roster cannot prove recovery")
        hold.observe(world, members, members, true)
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
