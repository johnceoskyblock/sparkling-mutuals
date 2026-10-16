package net.johnceo.sparklingmutuals.safari.automation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EntryPartyInboxTest {
    private val world = Any()
    private fun event(text: String, sender: String = "farmer", kick: Boolean = false) =
        EntryPartyMessage(text, sender, world, kick, !kick)
    @Test fun `first message in a new world retains its fresh kick while discarding old kick evidence`() {
        val inbox = EntryPartyInbox()
        inbox.add(event("!dt food")); inbox.add(event("I was kicked", "old", true))
        val nextWorld = Any()
        inbox.add(event("I was kicked", "new", true).copy(world = nextWorld))
        inbox.observeWorld(nextWorld)
        assertEquals(setOf("farmer", "new"), inbox.entries().map { it.second.sender }.toSet())
        assertTrue(inbox.pendingKick)
        assertTrue(inbox.pendingDowntime)
    }
    @Test fun `latest sender readiness state survives repeated text and more than thirty two messages`() {
        val inbox = EntryPartyInbox()
        inbox.add(event("!dt food")); inbox.add(event("r")); inbox.add(event("!dt food"))
        assertEquals(listOf("!dt food"), inbox.entries().map { it.second.text })
        repeat(100) { inbox.add(event("!dt reason $it")) }
        assertEquals(listOf("!dt reason 99"), inbox.entries().map { it.second.text })
        assertFalse(inbox.overflow)
    }
    @Test fun `world transfer retains downtime while discarding old instance kick context`() {
        val inbox = EntryPartyInbox()
        inbox.add(event("!dt food")); inbox.add(event("I was kicked", kick = true))
        inbox.transfer()
        assertEquals(listOf("!dt food"), inbox.entries().map { it.second.text })
        assertFalse(inbox.pendingKick)
        assertTrue(inbox.pendingDowntime)
    }
    @Test fun `bounded overflow can recover only after every current member personally declares ready`() {
        val inbox = EntryPartyInbox(2)
        inbox.add(event("!dt", "a")); inbox.add(event("!dt", "b")); inbox.add(event("!dt", "c"))
        assertTrue(inbox.overflow)
        val roster = setOf("id-a", "id-b", "id-c")
        inbox.validated("id-a", true, roster); inbox.validated("id-b", true, roster)
        assertTrue(inbox.overflow)
        inbox.validated("id-c", true, roster)
        assertFalse(inbox.overflow)
    }
    @Test fun `readiness cannot recover lost kick evidence but a transfer discards old instance kick context`() {
        val inbox = EntryPartyInbox(1)
        inbox.add(event("I was kicked", "a", true)); inbox.add(event("kick", "b", true))
        inbox.entries().forEach { inbox.remove(it.first) }
        inbox.validated("a", true, setOf("a", "b")); inbox.validated("b", true, setOf("a", "b"))
        assertTrue(inbox.pendingKick)
        assertFalse(inbox.pendingDowntime, "kick protection is independent of the downtime switch")
        inbox.transfer()
        assertFalse(inbox.pendingKick)
    }
}
