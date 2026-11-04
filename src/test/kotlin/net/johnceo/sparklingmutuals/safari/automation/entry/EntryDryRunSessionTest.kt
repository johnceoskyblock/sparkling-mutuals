package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryDryRunSessionTest {
    private val guest = UUID(0, 1)
    private val leader = UUID(0, 2)
    private val other = UUID(0, 3)
    private val creature = UUID(0, 100)
    private val party = EntryParty(leader, setOf(guest, leader, other, UUID(0, 4)), 0)
    private val gc = EntryContext(guest, "Guest", 1, 1, 1, "main", "mini1", party)
    private val lc = gc.copy(account = leader, selfName = "Leader")
    private val gf = EntryDryRunFrame(gc, true, true, false, 25000)
    private val lf = EntryDryRunFrame(lc, true, false, true, 0)
    private fun List<EntryDryRunOutput>.wire() = filterIsInstance<EntryDryRunOutput.Party>().map { it.message }
    private fun query(session: EntryDryRunSession, frame: EntryDryRunFrame = gf, now: Long = 25000) =
        session.tick(frame, now).wire().single() as EntryMessage.Query
    private fun reply(query: EntryMessage.Query, age: Long = 0) = EntryMessage.Live("Guest", query.nonce, "main", "mini1", "Doomspiral", age, "b".repeat(32))

    @Test fun `late warped guest uses a fresh real leader response and exposes only diagnostics`() {
        val g = EntryDryRunSession(); val l = EntryDryRunSession()
        l.tick(lf, 0)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 24900)
        val q = query(g)
        l.receive(lf, EntryPeer(guest, "Guest"), q, 25020)
        val response = l.tick(lf, 25020).wire().single() as EntryMessage.Live
        assertEquals(120, response.ageMs)
        g.receive(gf, EntryPeer(leader, "Leader"), response, 25040)
        assertTrue(g.tick(gf, 25040).any { it is EntryDryRunOutput.Notice && "would check" in it.text })
        assertTrue(g.tick(gf, 25050).isEmpty())
        assertTrue(g.tick(gf, 30100).any { it is EntryDryRunOutput.Notice && "expired" in it.text })
        assertTrue(g.tick(gf, 30200).wire().isEmpty())
    }

    @Test fun `unknown hint cannot authorize and request has no retry`() {
        val g = EntryDryRunSession(); val q = query(g)
        assertEquals("mini1", q.server)
        assertTrue(g.tick(gf, 26000).wire().isEmpty())
        assertTrue(g.tick(gf, 28000).any { it is EntryDryRunOutput.Notice && "timed out" in it.text })
        assertTrue(g.tick(gf, 29000).wire().isEmpty())
    }

    @Test fun `wrong leader server or nonce cannot consume valid reply`() {
        val g = EntryDryRunSession(); val q = query(g)
        g.receive(gf, EntryPeer(other, "Other"), reply(q), 25010)
        g.receive(gf, EntryPeer(leader, "Leader"), reply(q).copy(server = "mini2"), 25020)
        g.receive(gf, EntryPeer(leader, "Leader"), reply(q).copy(nonce = "c".repeat(32)), 25030)
        assertFalse(g.tick(gf, 25040).any { it is EntryDryRunOutput.Notice && "would check" in it.text })
        g.receive(gf, EntryPeer(leader, "Leader"), reply(q), 25050)
        assertTrue(g.tick(gf, 25050).any { it is EntryDryRunOutput.Notice && "would check" in it.text })
    }

    @Test fun `capture at entrance withdraws ready evidence and fences in flight response`() {
        val g = EntryDryRunSession(); val q = query(g)
        g.receive(gf, EntryPeer(leader, "Leader"), reply(q), 25010)
        g.tick(gf, 25010)
        g.captured(gf, "Doomspiral", 25020)
        assertFalse(g.tick(gf, 25030).any { it is EntryDryRunOutput.Notice && "would check" in it.text })
        assertTrue(g.debug().any { "captured" in it })
        val next = EntryDryRunSession(); val q2 = query(next)
        next.captured(gf, "Doomspiral", 25010)
        next.receive(gf, EntryPeer(leader, "Leader"), reply(q2), 25020)
        assertFalse(next.tick(gf, 25030).any { it is EntryDryRunOutput.Notice && "would check" in it.text })
    }

    @Test fun `empty completed scan withdraws unloaded sightings but does not retire them`() {
        val l = EntryDryRunSession(); l.tick(lf, 0)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 24900)
        val q = EntryMessage.Query("a".repeat(32), "main", "mini1")
        l.receive(lf, EntryPeer(guest, "Guest"), q, 25000)
        l.scan(lf, emptyList(), 25010)
        assertTrue(l.tick(lf, 25020).wire().single() is EntryMessage.None)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 25030)
        l.receive(lf, EntryPeer(other, "Other"), q, 26000)
        assertTrue(l.tick(lf, 26520).wire().single() is EntryMessage.Live)
    }

    @Test fun `capture before queued response never sends old live evidence`() {
        val l = EntryDryRunSession(); l.tick(lf, 0)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 24900)
        l.receive(lf, EntryPeer(guest, "Guest"), EntryMessage.Query("a".repeat(32), "main", "mini1"), 25000)
        l.captured(lf, "Doomspiral", 25010)
        assertTrue(l.tick(lf, 25020).wire().single() is EntryMessage.None)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 25030)
        l.receive(lf, EntryPeer(other, "Other"), EntryMessage.Query("b".repeat(32), "main", "mini1"), 26000)
        assertTrue(l.tick(lf, 26520).wire().single() is EntryMessage.None)
    }

    @Test fun `three simultaneous queries preserve pacing and third expires without retry`() {
        val l = EntryDryRunSession(); l.tick(lf, 0)
        for ((i, id) in listOf(guest, other, UUID(0, 4)).withIndex())
            l.receive(lf, EntryPeer(id, "Peer$i"), EntryMessage.Query("a".repeat(32), "main", "mini1"), 25000)
        assertEquals(1, l.tick(lf, 25000).wire().size)
        assertEquals(0, l.tick(lf, 26499).wire().size)
        assertEquals(1, l.tick(lf, 26500).wire().size)
        assertEquals(0, l.tick(lf, 28000).wire().size)
    }

    @Test fun `leader may establish fresh observations after invited roster changes`() {
        val l = EntryDryRunSession(); l.tick(lf, 0)
        l.scan(lf, listOf(EntryDryRunObservation(creature, "Doomspiral")), 19000)
        val changed = lf.copy(context = lc.copy(party = EntryParty(leader, party.members, 2)))
        l.tick(changed, 24000)
        l.scan(changed, listOf(EntryDryRunObservation(creature, "Doomspiral")), 24900)
        l.receive(changed, EntryPeer(guest, "Guest"), EntryMessage.Query("a".repeat(32), "main", "mini1"), 25000)
        assertTrue(l.tick(changed, 25010).wire().single() is EntryMessage.Live)
    }

    @Test fun `disable verification gaps and changed context cannot rearm a spent arrival`() {
        for (change in listOf(gf.copy(enabled = false), gf.copy(context = null),
            gf.copy(context = gc.copy(party = EntryParty(other, party.members, 1))), gf.copy(context = gc.copy(server = "mini2")))) {
            val g = EntryDryRunSession(); val q = query(g)
            g.tick(change, 25010)
            g.receive(gf, EntryPeer(leader, "Leader"), reply(q), 25020)
            assertTrue(g.tick(gf, 25030).wire().isEmpty())
            assertFalse(g.debug().any { "would check" in it })
        }
    }

    @Test fun `late enable and context readiness share original arrival deadline`() {
        val g = EntryDryRunSession()
        g.tick(gf.copy(context = null), 25000)
        assertTrue(g.tick(gf, 37000).wire().isEmpty())
        val h = EntryDryRunSession(); h.tick(gf.copy(enabled = false), 25000)
        assertTrue(h.tick(gf, 37000).wire().isEmpty())
    }

    @Test fun `new world permits a fresh query but clock rollback cancels current exchange`() {
        val g = EntryDryRunSession(); val q = query(g)
        g.tick(gf, 24999)
        g.receive(gf, EntryPeer(leader, "Leader"), reply(q), 25010)
        assertTrue(g.tick(gf, 25020).wire().isEmpty())
        val next = gf.copy(context = gc.copy(world = 2, arrival = 2), arrivedAt = 40000)
        assertEquals(1, g.tick(next, 40000).wire().size)
    }
}
