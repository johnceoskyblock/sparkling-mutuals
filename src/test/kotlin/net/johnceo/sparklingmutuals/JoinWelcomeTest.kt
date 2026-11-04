package net.johnceo.sparklingmutuals

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JoinWelcomeTest {
    @Test fun `welcome waits for SkyBlock and the release check and shows once per connection`() {
        val state = JoinWelcomeState()
        assertFalse(state.shouldShow(false, true, 0))
        assertFalse(state.shouldShow(true, false, 1000))
        assertFalse(state.shouldShow(true, true, 2000))
        assertFalse(state.shouldShow(true, true, 2999))
        assertFalse(state.shouldShow(true, false, 4000))
        assertTrue(state.shouldShow(true, true, 4001))
        assertFalse(state.shouldShow(true, false, 5000))
        assertFalse(state.shouldShow(true, true, 60000), "lobby transfers must not repeat it")
        val reconnect = JoinWelcomeState()
        assertFalse(reconnect.shouldShow(true, true, 60000))
        assertTrue(reconnect.shouldShow(true, true, 62000))
        for (host in listOf("hypixel.net", "MC.HYPIXEL.NET:25565", "alpha.hypixel.net", "www.hypixel.net."))
            assertTrue(JoinWelcomeState.hypixel(host), host)
        for (host in listOf(null, "example.net", "nothypixel.net", "hypixel.net.example.net"))
            assertFalse(JoinWelcomeState.hypixel(host), host)
    }
}
