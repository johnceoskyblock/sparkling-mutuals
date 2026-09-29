package net.johnceo.sparklingmutuals.commands

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.johnceo.sparklingmutuals.api.HypixelApi
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object MissingCommand {

    private val executor = Executors.newSingleThreadExecutor()
    private var waitingForOwnMessage = false

    private val timesaveCritters = listOf(
        "ROCKMITE",
        "SNOOZLE",
        "GEMZIE",
        "HONEYBUG",
        "GAZER",
        "GIMMIEGOLD",
        "DOOMSPIRAL",
        "WUMPA"
    )

    fun register() {
        ClientSendMessageEvents.CHAT.register { message ->
            extractIgn(message)?.let {
                waitingForOwnMessage = true
                lookupMissing(it)
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

            lookupMissing(ign)
        }
    }

    private fun extractIgn(message: String): String? =
        Regex("""!missing\s+([A-Za-z0-9_]{1,16})\b""")
            .find(message)
            ?.groupValues
            ?.getOrNull(1)

    private fun lookupMissing(ign: String) {
        executor.execute {
            try {
                val playerUuid = HypixelApi.getPlayerUuid(ign)
                    ?: return@execute sendClientMessage("Could not find Minecraft player: $ign")

                val profileUuid = HypixelApi.getCurrentProfileUuid(playerUuid)
                    ?: return@execute sendClientMessage(
                        "Could not find a selected SkyBlock profile for $ign."
                    )

                val sparklingCritters = HypixelApi.getSparklingCritters(playerUuid, profileUuid)
                    ?: return@execute sendClientMessage(
                        "Failed to retrieve sparkling critters for $ign."
                    )

                val missing = timesaveCritters
                    .filter { it !in sparklingCritters }
                    .map(::formatCritterName)
                    .toMutableList()

                if (
                    "BLUEBIRD" !in sparklingCritters ||
                    "PARAKEET" !in sparklingCritters ||
                    "MACAW" !in sparklingCritters
                ) {
                    missing.add("All Birds")
                }

                val message = if (missing.isEmpty()) {
                    "Missing Timesave Sparklings for $ign: None"
                } else {
                    "Missing Timesave Sparklings for $ign: ${missing.joinToString(", ")}"
                }

                sendPartyMessage(message)
            } catch (e: Exception) {
                println("❌ Missing sparkling lookup failed for $ign: $e")
                sendClientMessage("Failed to retrieve missing sparkling critters for $ign.")
            }
        }
    }

    private fun formatCritterName(critterId: String): String =
        when (critterId) {
            "ROCKMITE" -> "Rockmite"
            "SNOOZLE" -> "Snoozle"
            "GEMZIE" -> "Gemzie"
            "HONEYBUG" -> "Honeybug"
            "GAZER" -> "Gazer"
            "GIMMIEGOLD" -> "Gimmiegold"
            "DOOMSPIRAL" -> "Doomspiral"
            "WUMPA" -> "Wumpa"
            else -> critterId
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