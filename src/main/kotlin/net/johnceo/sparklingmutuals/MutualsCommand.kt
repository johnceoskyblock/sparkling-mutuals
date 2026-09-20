package net.johnceo.sparklingmutuals

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents

object MutualsCommand {

    private var waitingForOwnMessage = false

    fun register() {
        registerOutgoingListener()
        registerIncomingListener()
    }

    private fun registerOutgoingListener() {

        ClientSendMessageEvents.CHAT.register { message ->

            if (isMutualsCommand(message)) {

                // The server will send our own party message back to us.
                // Ignore that incoming copy.
                waitingForOwnMessage = true

                PartyManager.refreshPartyInfo {
                    MutualsManager.findMutuals()
                }
            }
        }
    }

    private fun registerIncomingListener() {

        ClientReceiveMessageEvents.GAME.register { message, _ ->

            val text = message.string

            if (!isMutualsCommand(text)) {
                return@register
            }

            // Ignore our own command being sent back to us.
            if (waitingForOwnMessage) {
                waitingForOwnMessage = false
                return@register
            }

            // Another party member sent !mutual / !mutuals.
            PartyManager.refreshPartyInfo {
                MutualsManager.findMutuals()
            }
        }
    }

    private fun isMutualsCommand(message: String): Boolean {

        val text = message.lowercase()

        val isPartyMessage =
            text.contains("party")

        val isMutualsCommand =
            text.contains("!mutual ") ||
                    text.contains("!mutuals ") ||
                    text.endsWith("!mutual") ||
                    text.endsWith("!mutuals")

        return isPartyMessage && isMutualsCommand
    }
}
