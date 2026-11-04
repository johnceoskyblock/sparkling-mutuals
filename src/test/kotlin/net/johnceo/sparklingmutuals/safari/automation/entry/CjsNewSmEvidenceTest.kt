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

    @Test fun `capture before the first scan cannot authorize a lingering local model`() {
        val session = EntryDryRunSession()
        session.sharedCaptures(frame, setOf("Doomspiral"), 1000)
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1001)
        session.sharedCaptures(frame, setOf("Doomspiral"), 1002)
        assertNull(session.fresh(frame, 1002, setOf(creature)))
    }

    @Test fun `capture before a leaders first scan never produces a LIVE reply`() {
        val session = EntryDryRunSession()
        val leaderFrame = frame.copy(context = context.copy(account = leader, selfName = "Leader"), atEntry = false, activeRun = true)
        session.sharedCaptures(leaderFrame, setOf("Doomspiral"), 1000)
        session.scan(leaderFrame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1001)
        session.receive(leaderFrame, EntryPeer(guest, "Guest"), EntryMessage.Query("a".repeat(32), "main", "mini1"), 1002)
        val response = session.tick(leaderFrame, 1003).filterIsInstance<EntryDryRunOutput.Party>().single().message
        assertTrue(response is EntryMessage.None)
    }

    @Test fun `context reset reapplies unmatched captures before the leaders next scan`() {
        val session = EntryDryRunSession()
        val leaderFrame = frame.copy(context = context.copy(account = leader, selfName = "Leader"), atEntry = false, activeRun = true)
        session.scan(leaderFrame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1000)
        session.sharedCaptures(leaderFrame, setOf("Doomspiral"), 1010)
        val changed = leaderFrame.copy(context = leaderFrame.context!!.copy(party = EntryParty(leader, party.members, 1)))
        session.sharedCaptures(changed, setOf("Doomspiral"), 1020)
        session.scan(changed, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1021)
        session.receive(changed, EntryPeer(guest, "Guest"), EntryMessage.Query("b".repeat(32), "main", "mini1"), 1022)
        val response = session.tick(changed, 1023).filterIsInstance<EntryDryRunOutput.Party>().single().message
        assertTrue(response is EntryMessage.None)
    }

    @Test fun `capture snapshot before a query also rejects a later LIVE reply for that species`() {
        val session = EntryDryRunSession()
        session.sharedCaptures(frame, setOf("Doomspiral"), 1000)
        val query = session.tick(frame, 1001).filterIsInstance<EntryDryRunOutput.Party>().single().message as EntryMessage.Query
        session.receive(frame, EntryPeer(leader, "Leader"), EntryMessage.Live("Guest", query.nonce, "main", "mini1", "Doomspiral", 0, "c".repeat(32)), 1010)
        assertNull(session.fresh(frame, 1010, emptySet()))
    }

    @Test fun `unmatched backend capture leaves other freshly identified species eligible`() {
        val session = EntryDryRunSession()
        session.sharedCaptures(frame, setOf("Doomspiral"), 1000)
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Gazer")), 1001)
        assertEquals("Gazer", session.fresh(frame, 1001, setOf(creature))?.species)
    }

    @Test fun `backend echo of a known chat capture preserves a genuinely different later identity`() {
        val session = EntryDryRunSession()
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1000)
        session.captured(frame, "Doomspiral", 1010)
        session.sharedCaptures(frame, setOf("Doomspiral"), 1020)
        val next = UUID(0, 101)
        session.scan(frame, listOf(EntryDryRunObservation(next, "Doomspiral")), 1030)
        assertNotNull(session.fresh(frame, 1030, setOf(next)))
        session.scan(frame, listOf(EntryDryRunObservation(creature, "Doomspiral")), 1040)
        assertNull(session.fresh(frame, 1040, setOf(creature)))
    }
}
