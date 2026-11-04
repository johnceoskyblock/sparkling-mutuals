package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryDryRunEnvironmentTest {
    private val self = UUID(0, 1)
    private val leader = UUID(0, 2)
    private val party = EntryParty(leader, setOf(self, leader), 0)
    private val connection = Any()
    private val world = Any()
    private fun EntryDryRunEnvironment.current(p: EntryParty? = party) = context(self, "Guest", p)

    @Test fun `early location cannot survive a later world change`() {
        val env = EntryDryRunEnvironment()
        env.world(connection, world, 100)
        env.location("safari", "mini1", "main")
        assertNotNull(env.current())
        env.world(connection, Any(), 25000)
        assertNull(env.current())
        env.location("safari", "mini2", "main")
        assertEquals("mini2", env.current()!!.server)
        assertEquals(25000, env.arrivedAt)
    }

    @Test fun `duplicate metadata and equal party refresh preserve context and arrival time`() {
        val env = EntryDryRunEnvironment()
        env.world(connection, world, 100)
        env.location("safari", "mini1", "main")
        val initial = env.current()
        env.world(connection, world, 900)
        env.location("safari", "mini1", "main")
        assertEquals(initial, env.current(EntryParty(leader, setOf(leader, self), 0)))
        assertEquals(100, env.arrivedAt)
    }

    @Test fun `party verification gap changes continuity even when restored roster is equal`() {
        val env = EntryDryRunEnvironment()
        env.world(connection, world, 100)
        env.location("safari", "mini1", "main")
        val before = env.current()!!
        assertNull(env.current(null))
        assertNotEquals(before, env.current())
        env.location("foraging_3", "mini1", "main")
        assertNull(env.current())
        env.location("safari", "mini1", "main")
        assertNotEquals(before, env.current())
    }

    @Test fun `upstream party invalidation between ticks cannot restore old context`() {
        val env = EntryDryRunEnvironment()
        env.world(connection, world, 100)
        env.location("safari", "mini1", "main")
        val initial = env.current()
        assertNotEquals(initial, env.current(EntryParty(leader, party.members, 1)))
        val roster = net.johnceo.sparklingmutuals.party.PartyRosterState()
        roster.update(listOf(self.toString(), leader.toString()), leader.toString())
        val before = roster.continuity
        roster.update(listOf(leader.toString(), self.toString()), leader.toString())
        assertEquals(before, roster.continuity)
        roster.invalidate()
        roster.update(listOf(self.toString(), leader.toString()), leader.toString())
        assertTrue(roster.continuity > before)
    }

    @Test fun `server realm leader and account changes invalidate old context`() {
        val env = EntryDryRunEnvironment()
        env.world(connection, world, 0)
        env.location("safari", "mini1", "main")
        val initial = env.current()!!
        env.location("safari", "mini2", "main")
        assertNotEquals(initial, env.current())
        env.location("safari", "mini2", "alpha")
        assertNotEquals(initial, env.current())
        assertNotEquals(env.current(), env.current(EntryParty(self, setOf(self, leader), 0)))
        env.location("safari", "unsafe name", "main")
        assertNull(env.current())
        env.world(null, null, 100)
        assertNull(env.current())
    }

    @Test fun `party protocol accepts only one current roster name match`() {
        val wire = "SMSE1 Q ${"a".repeat(32)} main mini1"
        val raw = "Party > [MVP+] Leader: $wire"
        val profiles = listOf(EntryPeer(leader, "Leader"))
        assertEquals(leader, EntryPartySender.resolve(raw, profiles, party.members)!!.first.id)
        for (bad in listOf("[CHAT] $raw", "Guild > Leader: $wire", "Party > Guest: $raw", "$raw\n$raw"))
            assertNull(EntryPartySender.resolve(bad, profiles, party.members), bad)
        assertNull(EntryPartySender.resolve(raw, profiles, setOf(self)))
        assertNull(EntryPartySender.resolve(raw, profiles + EntryPeer(self, "LEADER"), party.members))
        assertNull(EntryPartySender.resolve(raw, emptyList(), party.members))
    }
}
