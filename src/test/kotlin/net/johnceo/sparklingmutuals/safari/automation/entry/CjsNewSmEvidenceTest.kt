package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.johnceo.sparklingmutuals.sync.PartySyncState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjsNewSmEvidenceTest {
    private val guest = UUID(0, 1)
    private val leader = UUID(0, 2)
    private val creature = UUID(0, 100)
    private val party = EntryParty(leader, setOf(guest, leader), 0)
    private val context = EntryContext(guest, "Guest", 1, 1, 1, "main", "mini1", party)
    private val frame = EntryDryRunFrame(context, true, true, false, 1000)
    private fun room() = PartySyncState(guest.toString().replace("-", ""), party.members.mapTo(hashSetOf()) { it.toString().replace("-", "") })

    @Test fun `retained room discovery never becomes a fresh movement sighting`() {
        val room = room(); val session = EntryDryRunSession()
        assertTrue(room.receiveRoomSparklings("134217728,0,0,0")) // Doomspiral discovered, not captured.
        session.sharedCaptures(frame, room.capturedSpecies(), 1000)
        assertNull(session.fresh(frame, 1001, emptySet()))
        assertTrue(session.tick(frame, 1001).any { it is EntryDryRunOutput.Party && it.message is EntryMessage.Query })
        session.sharedCaptures(frame, room.capturedSpecies(), 3000) // A repeated/reconnect snapshot is not a new observation.
        assertNull(session.fresh(frame, 3000, emptySet()))
    }

    @Test fun `backend capture between scan and START withdraws a loaded local identity`() {
        val room = room(); val session = EntryDryRunSession()
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1000)
        assertNotNull(session.fresh(frame, 1001, setOf(creature)))
        assertTrue(room.receiveRoomSparklings("0,0,134217728,0"))
        assertEquals(setOf("Doomspiral"), room.capturedSpecies())
        session.sharedCaptures(frame, room.capturedSpecies(), 1010)
        assertNull(session.fresh(frame, 1010, setOf(creature)))
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1020)
        assertNull(session.fresh(frame, 1020, setOf(creature)), "The same captured identity cannot revive on a later scan")
    }

    @Test fun `backend capture fences a leader reply already in flight`() {
        val room = room(); val session = EntryDryRunSession()
        val query = session.tick(frame, 1000).filterIsInstance<EntryDryRunOutput.Party>().single().message as EntryMessage.Query
        assertTrue(room.receiveRoomSparklings("0,0,134217728,0"))
        session.sharedCaptures(frame, room.capturedSpecies(), 1010)
        session.receive(frame, EntryPeer(leader, "Leader"), EntryMessage.Live("Guest", query.nonce, "main", "mini1", "Doomspiral", 0, "b".repeat(32)), 1020)
        assertNull(session.fresh(frame, 1020, emptySet()))
        assertTrue(session.debug().any { "captured" in it })
    }

    @Test fun `unchanged captured snapshot does not retire a genuinely new later identity`() {
        val room = room(); val session = EntryDryRunSession()
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1000)
        room.receiveRoomSparklings("0,0,134217728,0")
        session.sharedCaptures(frame, room.capturedSpecies(), 1010)
        val next = UUID(0, 101)
        session.scan(frame, listOf(EntryDryRunObservation(next, "Doomspiral")), 1020)
        session.sharedCaptures(frame, room.capturedSpecies(), 1021)
        assertNotNull(session.fresh(frame, 1021, setOf(next)))
    }

    @Test fun `new visit reapplies retained captures and never carries old evidence`() {
        val session = EntryDryRunSession()
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1000)
        session.sharedCaptures(frame, setOf("Doomspiral"), 1010)
        val next = frame.copy(context = context.copy(world = 2, arrival = 2), arrivedAt = 2000)
        session.scan(next, listOf(EntryDryRunObservation(creature, "Doomspiral")), 2000)
        session.sharedCaptures(next, setOf("Doomspiral"), 2010)
        assertNull(session.fresh(next, 2010, setOf(creature)))
    }

    @Test fun `all current readiness holds prevent approach without a Shift bypass`() {
        fun ready(safari: Boolean = true, roster: Boolean = true, present: Boolean = true,
            pending: Boolean = false, kick: Boolean = false, entryHold: Boolean = false) =
            CjsApproachReadiness.allows(safari, roster, present, pending, kick, entryHold)
        assertTrue(ready())
        assertFalse(ready(safari = false))
        assertFalse(ready(roster = false))
        assertFalse(ready(present = false))
        assertFalse(ready(pending = true))
        assertFalse(ready(kick = true))
        assertFalse(ready(entryHold = true), "The current upstream entry hold cannot be bypassed by walking")
    }
}
