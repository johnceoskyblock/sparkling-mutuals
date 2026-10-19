package net.johnceo.sparklingmutuals.safari.resources

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StartingResourceInboxTest {
    @Test fun `Mojang undashed identity and roster UUID resolve to one canonical sender`() {
        val dashed = "12345678-90ab-cdef-1234-567890abcdef"
        assertEquals(dashed, StartingResourceInbox.canonicalUuid(dashed.uppercase()))
        assertEquals(dashed, StartingResourceInbox.canonicalUuid(dashed.replace("-", "")))
        assertNull(StartingResourceInbox.canonicalUuid("Friend"))
    }
    private val scope = "1234567890abcdef1234567890abcdef"
    @Test fun `only current verified party sender and observed run marker admit early quantities`() {
        val inbox = StartingResourceInbox()
        val owner = Any()
        for (envelope in listOf("Guild > Friend:", "From Friend:", "Party > Stranger:", "Party > Friend: quoted"))
            inbox.add(owner, "$envelope [SM] Starting items: 2x Bag of Seeds [run:$scope]", 0)
        inbox.add(owner, "Party > Friend: [SM] Starting items: 2x Bag of Seeds [run:$scope]", 0)
        val accepted = mutableListOf<String>()
        inbox.drain(owner, mapOf("friend" to "id"), setOf("other"), 1, { true }) { id, _ -> accepted.add(id) }
        assertTrue(accepted.isEmpty())
        inbox.add(owner, "Party > Friend: [SM] Starting items: 2x Bag of Seeds [run:$scope]", 2000)
        inbox.drain(owner, emptyMap(), setOf("id"), 2001, { true }) { id, _ -> accepted.add(id) }
        assertTrue(accepted.isEmpty(), "unresolved UUID stays pending")
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 2002, { false }) { id, _ -> accepted.add(id) }
        assertTrue(accepted.isEmpty(), "unknown/old run marker cannot mutate this run")
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 2003, { it == scope }) { id, _ -> accepted.add(id) }
        assertEquals(listOf("id"), accepted)
    }
    @Test fun `duplicate GAME CHAT messages deliver once and old owner work is discarded`() {
        val inbox = StartingResourceInbox()
        val owner = Any()
        repeat(2) { inbox.add(owner, "Party > Friend: hd", 0) }
        var count = 0
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 1, { true }) { _, _ -> count++ }
        assertEquals(1, count)
        inbox.add(owner, "Party > Friend: hd", 2)
        inbox.drain(Any(), mapOf("friend" to "id"), setOf("id"), 3, { true }) { _, _ -> count++ }
        assertEquals(1, count)
    }
    @Test fun `new unscoped start reports reject but legacy live notices remain compatible`() {
        val inbox = StartingResourceInbox()
        val owner = Any()
        assertFalse(inbox.add(owner, "Party > Friend: [SM] Starting items: none", 0))
        assertTrue(inbox.add(owner, "Party > Friend: [SM] Use: 1x Shining Coin", 0))
        var count = 0
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 1, { false }) { _, _ -> count++ }
        assertEquals(1, count)
    }
    @Test fun `second event delivery after draining does not refresh an old completion stamp`() {
        val inbox = StartingResourceInbox()
        val owner = Any()
        var count = 0
        inbox.add(owner, "Party > Friend: hd", 0)
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 0, { true }) { _, _ -> count++ }
        inbox.add(owner, "Party > Friend: hd", 1)
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 1, { true }) { _, _ -> count++ }
        assertEquals(1, count)
        inbox.add(owner, "Party > Friend: hd", 4000)
        inbox.drain(owner, mapOf("friend" to "id"), setOf("id"), 4000, { true }) { _, _ -> count++ }
        assertEquals(2, count, "a genuinely updated normal completion word remains possible")
    }
}
