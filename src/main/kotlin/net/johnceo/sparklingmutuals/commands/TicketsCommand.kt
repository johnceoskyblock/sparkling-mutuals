package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object TicketsCommand {

    private val executor = Executors.newSingleThreadExecutor()
    private var waitingForOwnMessage = false

    fun register() {
        ClientSendMessageEvents.CHAT.register { message ->
            extractIgn(message)?.let {
                waitingForOwnMessage = true
                lookupTickets(it)
            }
        }

        ClientReceiveMessageEvents.GAME.register { message, _ ->
            val text = message.string
            if (!text.contains("party", ignoreCase = true)) return@register

            val ign = extractIgn(text) ?: return@register

            if (waitingForOwnMessage) {
                waitingForOwnMessage = false
                return@register
            }

            lookupTickets(ign)
        }
    }

    private fun extractIgn(message: String): String? =
        Regex("""!(?:ticket|tickets)\s+([A-Za-z0-9_]{1,16})\b""")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)

    private fun lookupTickets(ign: String) {
        executor.execute {
            try {
                val playerUuid = HypixelApi.getPlayerUuid(ign)
                    ?: return@execute sendClientMessage("Could not find Minecraft player: $ign")

                val profileUuid = HypixelApi.getCurrentProfileUuid(playerUuid)
                    ?: return@execute sendClientMessage(
                        "Could not find a selected SkyBlock profile for $ign."
                    )

                val tickets = HypixelApi.getSafariTickets(playerUuid, profileUuid)
                    ?: return@execute sendClientMessage(
                        "Failed to retrieve Safari tickets for $ign."
                    )

                val result = buildString {
                    append("$ign: ")
                    append("Basic (${tickets["basic"] ?: 0}), ")
                    append("Economy (${tickets["economy"] ?: 0}), ")
                    append("Premium (${tickets["premium"] ?: 0}), ")
                    append("First-Class (${tickets["first_class"] ?: 0})")
                }

                sendPartyMessage(result)
            } catch (e: Exception) {
                println("❌ Ticket lookup failed for $ign: $e")
                sendClientMessage("Failed to retrieve Safari tickets for $ign.")
            }
        }
    }

    private fun sendPartyMessage(message: String) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.connection?.sendCommand("pc $message")
        }
    }

    private fun sendClientMessage(message: String) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendSystemMessage(Component.literal(message))
        }
    }
}