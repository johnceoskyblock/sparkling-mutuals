package net.johnceo.sparklingmutuals.alerts

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
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
    }
}
