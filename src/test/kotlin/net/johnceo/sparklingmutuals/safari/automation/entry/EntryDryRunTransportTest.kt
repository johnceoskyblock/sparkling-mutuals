package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryDryRunTransportTest {
    private val leader = UUID(0, 1)
    private val a = EntryPeer(UUID(0, 2), "GuestA")
    private val b = EntryPeer(UUID(0, 3), "GuestB")
    private val context = EntryContext(leader, "Leader", 1, 1, 1, "main", "mini1", EntryParty(leader, setOf(leader, a.id, b.id), 0))
    private val frame = EntryDryRunFrame(context, true, false, true, 0)
    private fun replies(): Pair<EntryDryRunSession, EntryDryRunOutput.Party> {
        val session = EntryDryRunSession(); session.tick(frame, 0)
        session.receive(frame, a, EntryMessage.Query("a".repeat(32), "main", "mini1"), 10000)
        session.receive(frame, b, EntryMessage.Query("b".repeat(32), "main", "mini1"), 10000)
        return session to session.tick(frame, 10000).filterIsInstance<EntryDryRunOutput.Party>().single()
    }

    @Test fun `stall after first proposal cannot bunch actual replies one millisecond apart`() {
        val (session, first) = replies()
        var at = 11499L
        val sender = EntryDryRunTransport { at }
        val sent = mutableListOf<String>()
        assertTrue(sender.send(first, { frame }, { listOf(a, b) }, sent::add))
        val second = session.tick(frame, 11500).filterIsInstance<EntryDryRunOutput.Party>().single()
        at = 11500
        assertFalse(sender.send(second, { frame }, { listOf(a, b) }, sent::add))
        assertEquals(1, sent.size)
    }

    @Test fun `queued replies require same unique current profile and original UUID at send`() {
        val (session, _) = replies()
        val second = session.tick(frame, 11500).filterIsInstance<EntryDryRunOutput.Party>().single()
        for (profiles in listOf(emptyList(), listOf(a), listOf(EntryPeer(a.id, "GuestB")),
            listOf(b, EntryPeer(a.id, "GUESTB")), listOf(EntryPeer(b.id, "Renamed")))) {
            val sent = mutableListOf<String>()
            assertFalse(EntryDryRunTransport { 11500 }.send(second, { frame }, { profiles }, sent::add), profiles.toString())
            assertTrue(sent.isEmpty())
        }
        val sent = mutableListOf<String>()
        assertTrue(EntryDryRunTransport { 11500 }.send(second, { frame }, { listOf(a, b) }, sent::add))
        assertEquals(1, sent.size)
    }

    @Test fun `cooldown uses completion time and uncertain sends do not rearm it`() {
        val (session, first) = replies()
        var at = 10000L
        val sender = EntryDryRunTransport { at }
        assertFalse(sender.send(first, { frame }, { listOf(a, b) }) { at = 11499; throw IllegalStateException("uncertain transport") })
        val second = session.tick(frame, 11500).filterIsInstance<EntryDryRunOutput.Party>().single()
        at = 11500
        assertFalse(sender.send(second, { frame }, { listOf(a, b) }) { fail("must not send after uncertainty") })
    }
}
