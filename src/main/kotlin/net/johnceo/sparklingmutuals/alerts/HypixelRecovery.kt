package net.johnceo.sparklingmutuals.alerts

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.JoinWelcomeState
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.hud.SkyblockSidebar
import net.johnceo.sparklingmutuals.mixin.DisconnectedScreenAccessor
import net.johnceo.sparklingmutuals.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import org.slf4j.LoggerFactory

/** Recovers the connection separately from transfers within Hypixel. All state belongs to the client thread. */
internal object HypixelRecovery {
    private val logger = LoggerFactory.getLogger("sparkling-mutuals/recovery")
    private var lastServer: ServerData? = null
    private var disconnectScreen: DisconnectedScreen? = null
    private var connectingScreen: ConnectScreen? = null
    private var nextReconnectAt = 0L
    private var reconnectAttempts = 0

    private var connection: ClientPacketListener? = null
    private var reportedLobby: HypixelLobby? = null
    private var routeAfterReconnect = false
    private var routeUntil = 0L
    private var lobby: HypixelLobby? = null
    private var nextTransferAt = 0L

    fun register() {
        // SafariIsland already subscribes to the location packet for the mod.
        HypixelPacketEvents.LOCATION_UPDATE.register { packet ->
            if (packet !is LocationUpdateS2CPacket) return@register
            val client = Minecraft.getInstance()
            val source = client.connection ?: return@register
            client.execute {
                if (client.connection === source && JoinWelcomeState.hypixel(client.currentServer?.ip)) {
                    trackConnection(source, now())
                    reportedLobby = HypixelLobby.fromLocation(packet.serverName(), packet.serverType().orElse(null),
                        packet.lobbyName().orElse(null))
                    // A transfer may land in a lobby of the same type.
                    if (reportedLobby == null) lobby = null
                }
            }
        }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (overlay) return@register
            val announcement = SafariRules.strip(message.string)
            if (!HypixelLobby.limboAnnouncement(announcement)) return@register
            val client = Minecraft.getInstance()
            val source = client.connection ?: return@register
            client.execute {
                if (client.connection === source && JoinWelcomeState.hypixel(client.currentServer?.ip)) {
                    trackConnection(source, now())
                    reportedLobby = HypixelLobby.LIMBO
                }
            }
        }
    }

    fun tick(client: Minecraft) {
        val now = now()
        val source = client.connection
        val server = client.currentServer
        if (source != null && server != null && JoinWelcomeState.hypixel(server.ip)) {
            // A connection may exist before the first world/player is ready.
            trackConnection(source, now)
            if (client.player == null || client.level == null) return
            lastServer = server.takeIf { ConfigManager.autoReconnectHypixel }
            clearRetry()
            tickSkyBlock(client, source, now)
            return
        }
        connection = null
        reportedLobby = null
        lobby = null
        routeAfterReconnect = false
        routeUntil = 0
        val screen = client.smScreen
        if (!ConfigManager.autoReconnectHypixel || source != null || client.level != null) {
            cancelReconnect()
        } else if (screen is DisconnectedScreen) {
            tickReconnect(client, screen, now)
        } else if (screen == null || screen !== connectingScreen) {
            cancelReconnect()
        }
    }

    private fun tickReconnect(client: Minecraft, screen: DisconnectedScreen, now: Long) {
        val server = lastServer ?: return
        val details = screen as DisconnectedScreenAccessor
        if (screen !== disconnectScreen) {
            if (!retryableDisconnect(details.recoveryDetails().reason().string)) {
                cancelReconnect()
                return
            }
            disconnectScreen = screen
            connectingScreen = null
            nextReconnectAt = now + reconnectDelay(reconnectAttempts)
        }
        if (now < nextReconnectAt) return
        reconnectAttempts++
        logger.info("Reconnecting to Hypixel (attempt {})", reconnectAttempts)
        ConnectScreen.startConnecting(details.recoveryParent(), client, ServerAddress.parseString(server.ip), server, false, null)
        connectingScreen = client.smScreen as? ConnectScreen
        if (connectingScreen == null) cancelReconnect()
    }

    private fun tickSkyBlock(client: Minecraft, source: ClientPacketListener, now: Long) {
        if (SkyblockSidebar.inSkyblock(client)) {
            lobby = null
            reportedLobby = null
            routeAfterReconnect = false
            return
        }
        if (!ConfigManager.autoJoinSkyBlock) {
            lobby = null
            routeAfterReconnect = false
            return
        }

        val title = SkyblockSidebar.title(client)
        val destination = HypixelLobby.fromSidebar(title) ?: reportedLobby
        val returningFromReconnect = routeAfterReconnect && now < routeUntil
        val command = destination?.skyBlockCommand(returningFromReconnect)
        if (command == null) {
            lobby = null
            return
        }
        if (lobby != destination) {
            lobby = destination
            nextTransferAt = now + 3_000
        }
        if (now < nextTransferAt) return
        nextTransferAt = now + 20_000
        logger.info("Returning to SkyBlock from {} via /{}", destination, command)
        AutomatedCommands.send(source, command)
    }

    private fun trackConnection(source: ClientPacketListener, now: Long) {
        if (connection === source) return
        connection = source
        reportedLobby = null
        lobby = null
        routeAfterReconnect = ConfigManager.autoJoinSkyBlock && reconnectAttempts > 0
        routeUntil = now + 120_000
    }

    private fun cancelReconnect() {
        lastServer = null
        clearRetry()
    }

    private fun clearRetry() {
        disconnectScreen = null
        connectingScreen = null
        reconnectAttempts = 0
    }

    private fun now() = System.nanoTime() / 1_000_000

    internal fun reconnectDelay(attempts: Int): Long = (5_000L shl attempts.coerceIn(0, 4)).coerceAtMost(60_000)

    internal fun retryableDisconnect(reason: String): Boolean {
        val text = reason.lowercase()
        return listOf("you are banned", "you are permanently banned", "you are temporarily banned",
            "ban id:", "security alert", "invalid session", "outdated client", "incompatible client",
            "resource pack was declined", "failed to verify username").none { it in text }
    }
}

internal enum class HypixelLobby {
    LIMBO, PROTOTYPE, MAIN;

    fun skyBlockCommand(afterReconnect: Boolean): String? = when (this) {
        // Limbo can require a lobby transfer before regular game commands work.
        LIMBO -> "lobby prototype"
        PROTOTYPE -> "play sb"
        MAIN -> if (afterReconnect) "play sb" else null
    }

    companion object {
        fun fromLocation(server: String, type: String?, lobbyName: String?): HypixelLobby? = when {
            server.equals("limbo", true) -> LIMBO
            lobbyName?.startsWith("prototype", true) == true || lobbyName?.startsWith("ptl", true) == true ||
                type.equals("PROTOTYPE", true) && lobbyName != null -> PROTOTYPE
            lobbyName?.startsWith("mainlobby", true) == true ||
                type.equals("MAIN", true) && lobbyName != null -> MAIN
            else -> null
        }

        fun fromSidebar(title: String): HypixelLobby? = when (title.uppercase()) {
            "PROTOTYPE", "PROTOTYPE LOBBY" -> PROTOTYPE
            "LIMBO" -> LIMBO
            "HYPIXEL", "MAIN LOBBY" -> MAIN
            else -> null
        }

        fun limboAnnouncement(message: String) = message.lineSequence().any { line ->
            val text = line.trim().trimEnd('.', '!')
            text.equals("You were spawned in Limbo", true) ||
                text.equals("You were sent to Limbo for being AFK", true) ||
                text.equals("An exception occurred in your connection, so you have been routed to limbo", true)
        }
    }
}
