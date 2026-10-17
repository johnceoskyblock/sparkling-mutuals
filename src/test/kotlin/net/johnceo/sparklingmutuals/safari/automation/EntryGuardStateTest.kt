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
    @Test fun `tab presence wins after two second kick pause and missing members still block`() {
        val hold = EntryKickHold(); val world = Any()
        hold.report(world, 1000)
        assertTrue(hold.holding(1000))
        assertTrue(hold.holding(2999))
        assertFalse(hold.holding(3000))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, true, hold.holding(2999), true, true))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, true, hold.holding(3000), true, true))
        assertTrue(EntryGuardDecision.block(EntryStage.SAFARI, false, false, false, false))
        assertFalse(EntryGuardDecision.block(EntryStage.SAFARI, true, true, false, true, true))
        hold.report(world, 4000); hold.report(world, 5000)
        assertTrue(hold.holding(6999)); assertFalse(hold.holding(7000))
        hold.observe(Any())
        assertFalse(hold.holding(5500), "a previous instance cannot hold a new one")
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
}
