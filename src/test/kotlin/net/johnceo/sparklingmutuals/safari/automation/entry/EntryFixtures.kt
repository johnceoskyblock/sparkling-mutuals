package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID

internal object EntryFixtures {
    val self = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val leader = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val stranger = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val token = "a".repeat(32)
    val event = "b".repeat(32)
    fun context() = EntryContext(self, "Self", 1, 1, 1, "main", "mini88A", EntryParty(leader, setOf(self, leader), 1))
    fun host() = context().copy(account = leader, selfName = "Leader")
    fun live(nonce: String = token, age: Long = 0) = EntryMessage.Live("Self", nonce, "main", "mini88A", "Bluebird", age, event)
}
