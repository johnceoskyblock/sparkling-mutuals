package net.johnceo.sparklingmutuals

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object MissingCommand {

    private val executor = Executors.newSingleThreadExecutor()

    private var waitingForOwnMessage = false

    private val individualTimesaveCritters = listOf(
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
        registerOutgoingListener()
        registerIncomingListener()
    }

    private fun registerOutgoingListener() {

        ClientSendMessageEvents.CHAT.register { message ->

            val ign = extractIgn(message)

            if (ign != null) {

                waitingForOwnMessage = true

                lookupMissing(ign)
            }
        }
    }

    private fun registerIncomingListener() {

        ClientReceiveMessageEvents.GAME.register { message, _ ->

            val text = message.string

            println("Received game message: $text")

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

            lookupMissing(ign)
        }
    }

    private fun extractIgn(message: String): String? {

        val regex =
            Regex("""!(?:missing)\s+([A-Za-z0-9_]{1,16})\b""")

        return regex
            .find(message)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun lookupMissing(ign: String) {

        executor.execute {

            try {

                println("Fetching missing sparkling data for $ign")

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

                val sparklingCritters =
                    HypixelApi.getSparklingCritters(
                        playerUuid,
                        profileUuid
                    )

                if (sparklingCritters == null) {
                    sendClientMessage(
                        "Failed to retrieve sparkling critters for $ign."
                    )
                    return@execute
                }

                val missing =
                    individualTimesaveCritters
                        .filter { it !in sparklingCritters }
                        .map { formatCritterName(it) }
                        .toMutableList()

                // All Birds is one combined timesave.
                // If the player is missing any of the three birds,
                // they are considered to be missing the All Birds timesave.
                val hasAllBirds =
                    "BLUEBIRD" in sparklingCritters &&
                            "PARAKEET" in sparklingCritters &&
                            "MACAW" in sparklingCritters

                if (!hasAllBirds) {
                    missing.add("All Birds")
                }

                val message =
                    if (missing.isEmpty()) {
                        "Missing Timesave Sparklings for $ign: None"
                    } else {
                        "Missing Timesave Sparklings for $ign: " + missing.joinToString(", ")
                    }

                sendPartyMessage(message)

            } catch (e: Exception) {

                println(
                    "❌ Missing sparkling lookup failed for $ign: $e"
                )

                sendClientMessage(
                    "Failed to retrieve missing sparkling critters for $ign."
                )
            }
        }
    }

    private fun formatCritterName(
        critterId: String
    ): String {
        return when (critterId) {
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
