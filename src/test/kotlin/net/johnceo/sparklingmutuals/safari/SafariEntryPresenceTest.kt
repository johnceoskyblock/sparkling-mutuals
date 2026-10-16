package net.johnceo.sparklingmutuals.safari

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SafariEntryPresenceTest {
    @Test fun `confirmed run entry survives missing sidebar and map until exit or another instance`() {
        val state = SafariEntryRunState()
        val world = Any()
        assertTrue(state.active(world, true, SafariLocation.INSIDE, 0))
        assertTrue(state.active(world, true, SafariLocation.UNKNOWN, null))
        assertTrue(state.active(world, true, SafariLocation.OUTSIDE, null))
        assertFalse(state.active(world, true, SafariLocation.ENTRANCE, null))
        assertTrue(state.active(world, true, SafariLocation.INSIDE, 1))
        assertFalse(state.active(Any(), true, SafariLocation.UNKNOWN, null))
        assertTrue(state.active(world, true, SafariLocation.INSIDE, 2))
        assertFalse(state.active(world, false, SafariLocation.UNKNOWN, null))
    }
    @Test fun `island transfer message alone is not an active run and the entrance never stops scanning`() {
        assertFalse(SafariEntryPresence.inRun(SafariLocation.INSIDE, null))
        assertFalse(SafariEntryPresence.inRun(SafariLocation.ENTRANCE, 0))
        assertFalse(SafariEntryPresence.inRun(SafariLocation.OUTSIDE, null))
        for (area in 0..4) assertTrue(SafariEntryPresence.inRun(SafariLocation.INSIDE, area))
    }
    private val instance = Any()
    private val party = setOf("me", "friend")
    @Test fun `arrival never grants an early loading bypass`() {
        val state = SafariEntryPresence()
        state.update(100, true, false, instance, party, setOf("me"), "me")
        state.update(3099, true, false, instance, party, setOf("me"), "me")
        assertEquals(setOf("friend"), state.missing)
        assertFalse(state.ready)
        state.update(3100, true, false, instance, party, emptySet(), "me")
        assertEquals(party, state.missing)
        assertFalse(state.ready)
        state.update(3101, true, false, instance, party, setOf("me"), "me")
        assertEquals(setOf("friend"), state.missing)
    }
    @Test fun `presence is continuously reevaluated as members warp in and leave`() {
        val state = SafariEntryPresence()
        state.update(0, true, false, instance, party, setOf("me"), "me")
        assertFalse(state.update(3000, true, false, instance, party, setOf("me"), "me"))
        assertFalse(state.update(3100, true, false, instance, party, setOf("me"), "me"))
        assertTrue(state.update(3200, true, false, instance, party, party, "me"))
        assertTrue(state.missing.isEmpty())
        state.update(3300, true, false, instance, party, setOf("me"), "me")
        assertEquals(setOf("friend"), state.missing)
    }
    @Test fun `outside island and active runs never produce a missing party hold`() {
        val state = SafariEntryPresence()
        state.update(0, false, false, instance, party, setOf("me"), "me")
        state.update(10000, false, false, instance, party, setOf("me"), "me")
        assertTrue(state.missing.isEmpty())
        state.update(11000, true, false, instance, party, setOf("me"), "me")
        state.update(14000, true, false, instance, party, setOf("me"), "me")
        assertFalse(state.missing.isEmpty())
        state.update(14001, true, true, instance, party, setOf("me"), "me")
        assertTrue(state.missing.isEmpty())
    }
    @Test fun `a changed instance requires its current player list without grace`() {
        val state = SafariEntryPresence()
        state.update(0, true, false, instance, party, setOf("me"), "me")
        state.update(3000, true, false, instance, party, setOf("me"), "me")
        val next = Any()
        state.update(4000, true, false, next, party, setOf("me"), "me")
        assertEquals(setOf("friend"), state.missing)
        assertFalse(state.ready)
        state.update(7000, true, false, next, party, setOf("me"), "me")
        assertFalse(state.missing.isEmpty())
    }
    @Test fun `UUID formatting and case do not mark present members missing and roster departures release`() {
        val state = SafariEntryPresence()
        val roster = setOf("AB-CD", "EF-GH")
        state.update(0, true, false, instance, roster, setOf("abcd", "efgh"), "AB-CD")
        state.update(3000, true, false, instance, roster, setOf("abcd", "efgh"), "AB-CD")
        assertTrue(state.missing.isEmpty())
        state.update(4000, true, false, instance, roster, setOf("abcd"), "abcd")
        assertEquals(setOf("efgh"), state.missing)
        state.update(4100, true, false, instance, setOf("abcd"), setOf("abcd"), "abcd")
        assertTrue(state.missing.isEmpty())
        state.update(4200, true, false, instance, null, setOf("abcd"), "abcd")
        assertTrue(state.missing.isEmpty())
    }
}
