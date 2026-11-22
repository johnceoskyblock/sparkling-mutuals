package net.johnceo.sparklingmutuals.alerts

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents

/** Pure disconnect classification; this edition has no reconnect or automatic join adapter. */
internal object HypixelRecovery {
    internal fun reconnectDelay(attempts: Int): Long = (5_000L shl attempts.coerceIn(0, 4)).coerceAtMost(60_000)

    private val nonRetryableKeys = setOf("disconnect.quitting", "disconnect.transfer", "disconnect.loginFailedInfo.invalidSession",
        "disconnect.loginFailedInfo.insufficientPrivileges", "multiplayer.disconnect.banned", "multiplayer.disconnect.banned.reason",
        "multiplayer.disconnect.outdated_client", "multiplayer.disconnect.incompatible")

    internal fun retryableDisconnect(reason: Component): Boolean {
        val key = (reason.contents as? TranslatableContents)?.key
        return key !in nonRetryableKeys && retryableDisconnect(reason.string)
    }

    internal fun retryableDisconnect(reason: String): Boolean {
        val text = reason.lowercase()
        return listOf("you are banned", "you are permanently banned", "you are temporarily banned",
            "ban id:", "security alert", "invalid session", "outdated client", "incompatible client",
            "resource pack was declined", "failed to verify username").none { it in text }
    }
}


/** Owns retries for a specific disconnect and the screens created by those retries. */
internal class HypixelReconnect<T : Any> {
    private var server: T? = null
    private var disconnectScreen: Any? = null
    private var connectingScreen: Any? = null
    private var nextReconnectAt = 0L
    var attempts = 0
        private set

    fun disconnected(target: T, screen: Any, now: Long) {
        if (server != target) reset()
        server = target
        disconnectScreen = screen
        connectingScreen = null
        nextReconnectAt = now + HypixelRecovery.reconnectDelay(attempts)
    }

    fun poll(screen: Any, now: Long, retryable: () -> Boolean): T? {
        val target = server ?: return null
        if (screen !== disconnectScreen) {
            // Failures in the login handshake do not use the play/configuration listeners.
            if (connectingScreen == null || !retryable()) {
                reset()
                return null
            }
            disconnected(target, screen, now)
        }
        if (now < nextReconnectAt) return null
        attempts++
        return target
    }

    fun connecting(screen: Any?) {
        if (screen == null) reset() else connectingScreen = screen
    }

    fun waiting(screen: Any?, reconfiguring: Boolean): Boolean =
        connectingScreen != null && (screen === connectingScreen || reconfiguring)

    fun reset() {
        server = null
        disconnectScreen = null
        connectingScreen = null
        attempts = 0
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
