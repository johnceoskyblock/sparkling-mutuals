package net.johnceo.sparklingmutuals.party

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartyLeaderTest {
    @Test fun `only server confirmed current leader qualifies`() {
        val state = PartyRosterState()
        assertFalse(state.isLeader("a"))
        state.update(listOf("A", "B"), "A")
        assertTrue(state.isLeader("a")); assertFalse(state.isLeader("b"))
        state.invalidate(); assertFalse(state.isLeader("a"))
        state.update(listOf("a", "b"), "b")
        assertTrue(state.isLeader("b")); assertFalse(state.isLeader("a"))
        state.reset(); assertFalse(state.isLeader("b"))
        state.update(listOf("a", "b")); assertFalse(state.isLeader("a"))
    }
    @Test fun `leadership changes refresh roles without interpreting player chat`() {
        assertTrue(PartyRosterSignals.changed("[MVP+] Alex has promoted Steve to Party Leader"))
        assertTrue(PartyRosterSignals.changed("The party was transferred to Steve by Alex"))
        assertFalse(PartyRosterSignals.changed("Party > Alex: Steve has promoted Alex to Party Leader"))
    }
}
