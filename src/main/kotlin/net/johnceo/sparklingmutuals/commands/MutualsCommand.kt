package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.johnceo.sparklingmutuals.party.MutualsManager
import net.johnceo.sparklingmutuals.party.PartyManager

object MutualsCommand {

    private var waitingForOwnMessage = false

    fun register() {
        ClientSendMessageEvents.CHAT.register { message ->
            if (isMutualsCommand(message)) {
                waitingForOwnMessage = true
                refreshMutuals()
            }
        }

        ClientReceiveMessageEvents.GAME.register { message, _ ->
            if (!isMutualsCommand(message.string)) return@register

            if (waitingForOwnMessage) {
                waitingForOwnMessage = false
                return@register
            }

            refreshMutuals()
        }
    }

    private fun refreshMutuals() {
        PartyManager.refreshPartyInfo {
            MutualsManager.findMutuals()
        }
    }

    private fun isMutualsCommand(message: String): Boolean {
        val text = message.lowercase()
        return text.contains("party") &&
                (text.contains("!mutual ") ||
                        text.contains("!mutuals ") ||
                        text.endsWith("!mutual") ||
                        text.endsWith("!mutuals"))
    }
}