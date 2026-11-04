package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class EntryReportExchangeTest {
    private val c = EntryFixtures.context()
    private val peer = EntryPeer(EntryFixtures.leader, "Leader")
    private fun requester(): EntryReportRequester = EntryReportRequester { EntryFixtures.token }.also { it.beginArrival(c, 1000) }

    @Test fun `late join gets one query without seeing the original announcement`() {
        val r = requester()
        assertNotNull(r.request(c, 1100))
        assertNull(r.request(c, 1200))
        val sighting = r.receive(c, peer, EntryFixtures.live(age = 100), 1300)!!
        assertTrue(sighting.valid(c, 5999))
        assertFalse(sighting.valid(c, 6000), "age plus round trip consumes the five-second budget")
        assertNull(r.receive(c, peer, EntryFixtures.live(), 1400))
        r.beginArrival(c, 1400)
        assertNull(r.request(c, 1500), "same arrival cannot replenish a consumed request")
    }

    @Test fun `reject forged stale or mismatched replies without accepting their claim`() {
        val replies = listOf(
            EntryFixtures.live(nonce = "c".repeat(32)), EntryFixtures.live().copy(requester = "Other"),
            EntryFixtures.live().copy(server = "miniOther"), EntryFixtures.live().copy(realm = "alpha")
        )
        replies.forEach { bad ->
            val r = requester(); r.request(c, 1100)
            assertNull(r.receive(c, peer, bad, 1200))
            assertNotNull(r.receive(c, peer, EntryFixtures.live(), 1300), "invalid input does not consume a genuine response")
        }
        val r = requester(); r.request(c, 1100)
        assertNull(r.receive(c, EntryPeer(EntryFixtures.stranger, "Stranger"), EntryFixtures.live(), 1200))
        assertNull(r.receive(c.copy(world = 2), peer, EntryFixtures.live(), 1300))
        assertNull(r.receive(c, peer, EntryFixtures.live(), 1400), "context mismatch cancels rather than revives")
    }

    @Test fun `deadline NONE old observation and rollback terminate without retry`() {
        val expired = requester(); expired.request(c, 1100)
        assertNull(expired.receive(c, peer, EntryFixtures.live(), 4100))
        assertNull(expired.request(c, 4200))
        val none = requester(); none.request(c, 1100)
        assertNull(none.receive(c, peer, EntryMessage.None("Self", EntryFixtures.token, "main", "mini88A"), 1200))
        assertNull(none.receive(c, peer, EntryFixtures.live(), 1300))
        val old = requester(); old.request(c, 1100)
        assertNull(old.receive(c, peer, EntryFixtures.live(age = 4900), 1300))
        val rollback = requester(); rollback.request(c, 1100)
        assertNull(rollback.receive(c, peer, EntryFixtures.live(), 1099))
        assertNull(rollback.receive(c, peer, EntryFixtures.live(), 1200))
        val late = requester()
        assertNull(late.request(c, 13000))
        val overflow = EntryReportRequester { EntryFixtures.token }; overflow.beginArrival(c, Long.MAX_VALUE - 10)
        assertNotNull(overflow.request(c, Long.MAX_VALUE - 5))
        assertNull(overflow.receive(c, peer, EntryFixtures.live(), Long.MAX_VALUE))
    }

    @Test fun `changed party continuity invalidates while equal refreshed snapshot retains budgets`() {
        val r = requester(); r.request(c, 1100)
        assertNotNull(r.receive(c.copy(party = EntryParty(c.party.leader, c.party.members, 1)), peer, EntryFixtures.live(), 1200))
        val changed = requester(); changed.request(c, 1100)
        assertNull(changed.receive(c.copy(party = EntryParty(c.party.leader, c.party.members, 2)), peer, EntryFixtures.live(), 1200))
    }

    @Test fun `live cache handles capture identity replacement freshness and context changes`() {
        val cache = EntrySightings { EntryFixtures.event }
        val id = UUID.randomUUID()
        assertTrue(cache.observe(c, id, "Bluebird", 1000))
        assertEquals("Bluebird", cache.freshest(c, 1100)?.species)
        cache.captured(c, "Bluebird")
        assertNull(cache.freshest(c, 1200))
        assertFalse(cache.observe(c, id, "Bluebird", 1300), "captured lingering identity is not a new sighting")
        assertTrue(cache.observe(c, UUID.randomUUID(), "Bluebird", 1400))
        assertNotNull(cache.freshest(c, 6399))
        assertNull(cache.freshest(c, 6400))
        assertNull(cache.freshest(c.copy(world = 2), 6500))
        assertFalse(cache.observe(c, UUID.randomUUID(), "unknown", 6600))
    }

    @Test fun `leader answers only a verified same destination member using current local evidence`() {
        val host = EntryFixtures.host()
        val cache = EntrySightings { EntryFixtures.event }; cache.observe(host, UUID.randomUUID(), "Bluebird", 1000)
        val responder = EntryReportResponder(cache)
        val query = EntryMessage.Query(EntryFixtures.token, "main", "mini88A")
        val member = EntryPeer(EntryFixtures.self, "Self")
        assertFalse(responder.offer(host, EntryPeer(EntryFixtures.stranger, "Other"), query, 1100, true, true, true))
        assertFalse(responder.offer(host, member, query.copy(server = "miniOther"), 1100, true, true, true))
        assertFalse(responder.offer(host, member, query, 1100, false, true, true))
        assertFalse(responder.offer(host, member, query, 1100, true, false, true))
        assertFalse(responder.offer(host, member, query, 1100, true, true, false))
        assertTrue(responder.offer(host, member, query, 1100, true, true, true))
        assertFalse(responder.offer(host, member, query, 1200, true, true, true))
        val reply = responder.poll(host, 1300, true, true, true)!!
        assertEquals(member, reply.peer)
        assertEquals(EntryMessage.Live("Self", EntryFixtures.token, "main", "mini88A", "Bluebird", 300, EntryFixtures.event), reply.message)
        assertNull(responder.poll(host, 1400, true, true, true))
        assertFalse(responder.offer(host, member, query.copy(nonce = "c".repeat(32)), 1500, true, true, true))
    }

    @Test fun `queued reply rechecks captures and expiry and invalidates old host context`() {
        val host = EntryFixtures.host(); val member = EntryPeer(EntryFixtures.self, "Self")
        val query = EntryMessage.Query(EntryFixtures.token, "main", "mini88A")
        val cache = EntrySightings { EntryFixtures.event }; cache.observe(host, UUID.randomUUID(), "Bluebird", 1000)
        val responder = EntryReportResponder(cache)
        responder.offer(host, member, query, 1100, true, true, true)
        cache.captured(host, "Bluebird")
        assertTrue(responder.poll(host, 1200, true, true, true)!!.message is EntryMessage.None)
        val late = EntryReportResponder(cache); late.offer(host, member, query, 2000, true, true, true)
        assertNull(late.poll(host, 5000, true, true, true))
        val moved = EntryReportResponder(cache); moved.offer(host, member, query, 2100, true, true, true)
        assertNull(moved.poll(host.copy(world = 2), 2200, true, true, true))
        assertNull(moved.poll(host, 2300, true, true, true))
    }

    @Test fun `multiple captured identities never become fresh again and cap fails closed`() {
        val cache = EntrySightings { EntryFixtures.event }
        val old = UUID.randomUUID()
        cache.observe(c, old, "Bluebird", 1000); cache.captured(c, "Bluebird")
        cache.observe(c, UUID.randomUUID(), "Bluebird", 1100); cache.captured(c, "Bluebird")
        assertFalse(cache.observe(c, old, "Bluebird", 1200), "an older lingering captured model stays retired")
        repeat(127) { i ->
            cache.observe(c, UUID.randomUUID(), "Bluebird", 1300L + i)
            cache.captured(c, "Bluebird")
        }
        assertFalse(cache.observe(c, UUID.randomUUID(), "Bluebird", 2000), "retirement overflow cannot forget captured proof")
        assertNull(cache.freshest(c, 2100))
    }

    @Test fun `disabling after enqueue cancels replies and rollback cannot resurrect them`() {
        val host = EntryFixtures.host(); val member = EntryPeer(EntryFixtures.self, "Self")
        val cache = EntrySightings { EntryFixtures.event }; cache.observe(host, UUID.randomUUID(), "Bluebird", 1000)
        val r = EntryReportResponder(cache)
        r.offer(host, member, EntryMessage.Query(EntryFixtures.token, "main", "mini88A"), 1100, true, true, true)
        assertNull(r.poll(host, 1200, false, true, true))
        assertNull(r.poll(host, 1300, true, true, true))
        val back = EntryReportResponder(cache)
        back.offer(host, member, EntryMessage.Query(EntryFixtures.token, "main", "mini88A"), 1400, true, true, true)
        assertNull(back.poll(host, 1399, true, true, true))
        assertNull(back.poll(host, 1500, true, true, true))
    }
}
