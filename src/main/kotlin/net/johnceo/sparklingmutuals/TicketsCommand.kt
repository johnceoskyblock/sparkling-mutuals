package net.johnceo.sparklingmutuals

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object TicketsCommand {

    private val executor = Executors.newSingleThreadExecutor()

    private var waitingForOwnMessage = false

    fun register() {
        registerOutgoingListener()
        registerIncomingListener()
    }

    private fun registerOutgoingListener() {

        ClientSendMessageEvents.CHAT.register { message ->

            val ign = extractIgn(message)

            if (ign != null) {

                waitingForOwnMessage = true

                lookupTickets(ign)
            }
        }
    }

    private fun registerIncomingListener() {

        ClientReceiveMessageEvents.GAME.register { message, _ ->

            val text = message.string

            if (!text.lowercase().contains("party")) {
                return@register
            }

            val ign = extractIgn(text)

            if (ign == null) {
                return@register
            }

            // Ignore the copy of our own command coming back from Hypixel.
            if (waitingForOwnMessage) {
                waitingForOwnMessage = false
                return@register
            }

            lookupTickets(ign)
        }
    }

    private fun extractIgn(message: String): String? {

        val regex =
            Regex("""!(?:ticket|tickets)\s+([A-Za-z0-9_]{1,16})\b""")

        return regex
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun lookupTickets(ign: String) {

        executor.execute {

            try {

                println("Fetching ticket data for $ign")

                val playerUuid =
                    HypixelApi.getPlayerUuid(ign)

                if (playerUuid == null) {
                    sendClientMessage(
                        "Could not find Minecraft player: $ign"
                    )
                    return@execute
                }

                val profileUuid =
                    HypixelApi.getCurrentProfileUuid(playerUuid)

                if (profileUuid == null) {
                    sendClientMessage(
                        "Could not find a selected SkyBlock profile for $ign."
                    )
                    return@execute
                }

                val tickets =
                    HypixelApi.getSafariTickets(
                        playerUuid,
                        profileUuid
                    )

                if (tickets == null) {
                    sendClientMessage(
                        "Failed to retrieve Safari tickets for $ign."
                    )
                    return@execute
                }

                val basic =
                    tickets["basic"] ?: 0

                val economy =
                    tickets["economy"] ?: 0

                val premium =
                    tickets["premium"] ?: 0

                val firstClass =
                    tickets["first_class"] ?: 0

                val result =
                    "$ign: Basic ($basic), " +
                            "Economy ($economy), " +
                            "Premium ($premium), " +
                            "First-Class ($firstClass)"

                sendPartyMessage(result)

            } catch (e: Exception) {

                println(
                    "❌ Ticket lookup failed for $ign: $e"
                )

                sendClientMessage(
                    "Failed to retrieve Safari tickets for $ign."
                )
            }
        }
    }

    private fun sendPartyMessage(
        message: String
    ) {
        Minecraft.getInstance().execute {

            val connection =
                Minecraft.getInstance().player?.connection
                    ?: return@execute

            connection.sendCommand(
                "pc $message"
            )
        }
    }

    private fun sendClientMessage(
        message: String
    ) {
        Minecraft.getInstance().execute {

            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal(message)
            )
        }
    }
}