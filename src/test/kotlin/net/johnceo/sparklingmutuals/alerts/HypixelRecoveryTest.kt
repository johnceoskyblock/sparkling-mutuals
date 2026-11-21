package net.johnceo.sparklingmutuals.alerts

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class HypixelRecoveryTest {
    @TempDir lateinit var directory: Path

    @Test fun recoverySwitchesAreOptInAndSaveIndependently() {
        ConfigManager.init(directory)
        val settings = SafariSettings()
        assertFalse(settings.warp.autoReconnectHypixel)
        assertFalse(settings.warp.autoJoinSkyBlock)

        settings.warp.autoReconnectHypixel = true
        settings.apply()
        ConfigManager.init(directory)
        assertTrue(ConfigManager.autoReconnectHypixel)
        assertFalse(ConfigManager.autoJoinSkyBlock)

        SafariSettings().apply {
            warp.autoReconnectHypixel = false
            warp.autoJoinSkyBlock = true
        }.apply()
        ConfigManager.init(directory)
        assertFalse(ConfigManager.autoReconnectHypixel)
        assertTrue(ConfigManager.autoJoinSkyBlock)
    }

    @Test fun limboRoutesThroughPrototypeAndMainLobbyRequiresReconnect() {
        assertEquals("lobby prototype", HypixelLobby.fromLocation("limbo", null, null)?.skyBlockCommand(false))
        assertEquals("play sb", HypixelLobby.fromLocation("lobby1", "PROTOTYPE", "prototype1")?.skyBlockCommand(false))
        assertEquals("play sb", HypixelLobby.fromSidebar("PROTOTYPE")?.skyBlockCommand(false))
        assertEquals("play sb", HypixelLobby.MAIN.skyBlockCommand(true))
        assertNull(HypixelLobby.MAIN.skyBlockCommand(false))
        assertNull(HypixelLobby.fromLocation("lobby2", "BEDWARS", "bedwarslobby2"))
        assertNull(HypixelLobby.fromLocation("mini123", "SKYBLOCK", null))
    }

    @Test fun limboFallbackRecognizesServerAnnouncementsWithoutMatchingPartyChat() {
        assertTrue(HypixelLobby.limboAnnouncement("You were spawned in Limbo.\n/limbo for more information."))
        assertTrue(HypixelLobby.limboAnnouncement("An exception occurred in your connection, so you have been routed to limbo!"))
        assertFalse(HypixelLobby.limboAnnouncement("Party > Someone: You were spawned in Limbo."))
    }

    @Test fun reconnectBacksOffAndStopsOnDenials() {
        assertEquals(listOf(5000L, 10000L, 20000L, 40000L, 60000L, 60000L),
            (0..5).map(HypixelRecovery::reconnectDelay))
        assertTrue(HypixelRecovery.retryableDisconnect("Connection timed out"))
        assertFalse(HypixelRecovery.retryableDisconnect("You are temporarily banned!"))
        assertFalse(HypixelRecovery.retryableDisconnect("Failed to login: Invalid session"))
        assertFalse(HypixelRecovery.retryableDisconnect(Component.translatable("disconnect.quitting")))
        assertFalse(HypixelRecovery.retryableDisconnect(Component.translatable("disconnect.transfer")))
    }

    @Test fun configurationDisconnectCanStartAConnectionFromTheMenu() {
        val retry = HypixelReconnect<String>()
        val reconfiguring = Any()
        assertFalse(retry.waiting(reconfiguring, reconfiguring = true))
        val failedTransfer = Any()
        // The failed configuration listener provides the server even with no play listener.
        retry.disconnected("mc.hypixel.net", failedTransfer, 1000)
        assertNull(retry.poll(failedTransfer, 5999) { true })
        assertEquals("mc.hypixel.net", retry.poll(failedTransfer, 6000) { true })
        assertEquals(1, retry.attempts)
    }

    @Test fun networkAndLoginFailuresKeepTheRetryBackoff() {
        val retry = HypixelReconnect<String>()
        val firstFailure = Any()
        retry.disconnected("mc.hypixel.net", firstFailure, 0)
        assertEquals("mc.hypixel.net", retry.poll(firstFailure, 5000) { true })
        val connect = Any()
        retry.connecting(connect)
        assertTrue(retry.waiting(connect, reconfiguring = false))
        assertTrue(retry.waiting(Any(), reconfiguring = true))

        val secondFailure = Any()
        retry.disconnected("mc.hypixel.net", secondFailure, 6000)
        assertNull(retry.poll(secondFailure, 15999) { true })
        assertEquals("mc.hypixel.net", retry.poll(secondFailure, 16000) { true })
        retry.connecting(Any())

        val handshakeFailure = Any()
        assertNull(retry.poll(handshakeFailure, 17000) { true })
        assertNull(retry.poll(handshakeFailure, 36999) { true })
        assertEquals("mc.hypixel.net", retry.poll(handshakeFailure, 37000) { true })
        assertEquals(3, retry.attempts)
    }

    @Test fun cancelAndDeniedLoginCannotStartAnotherRetry() {
        val retry = HypixelReconnect<String>()
        val failure = Any()
        retry.disconnected("mc.hypixel.net", failure, 0)
        assertEquals("mc.hypixel.net", retry.poll(failure, 5000) { true })
        retry.connecting(Any())
        assertFalse(retry.waiting(Any(), reconfiguring = false))
        retry.reset()
        assertNull(retry.poll(failure, 60000) { true })

        retry.disconnected("mc.hypixel.net", failure, 60000)
        assertEquals("mc.hypixel.net", retry.poll(failure, 65000) { true })
        retry.connecting(Any())
        val denied = Any()
        assertNull(retry.poll(denied, 66000) { false })
        assertNull(retry.poll(denied, 120000) { true })
    }
}
