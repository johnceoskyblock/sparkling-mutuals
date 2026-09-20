package net.johnceo.sparklingmutuals

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors

object MutualsManager {

    private val executor = Executors.newSingleThreadExecutor()

    private const val CACHE_DURATION_MS = 60_000L

    private data class CachedSparklingData(
        val critters: Set<String>,
        val timestamp: Long
    )

    private val sparklingCache =
        mutableMapOf<String, CachedSparklingData>()

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

    fun findMutuals() {

        val partyMembers = PartyManager.getMembers()

        if (partyMembers.isEmpty()) {
            sendPartyMessage(
                "Mutual Timesave Sparkling Critters: No party members detected."
            )
            return
        }

        executor.execute {

            try {

                val allSparklingSets = mutableListOf<Set<String>>()

                for (uuid in partyMembers) {

                    val sparklingCritters =
                        getPlayerSparklingCritters(uuid)

                    if (sparklingCritters == null) {

                        sendClientMessage(
                            "Failed to retrieve sparkling critters for $uuid."
                        )

                        return@execute
                    }

                    allSparklingSets.add(sparklingCritters)
                }

                if (allSparklingSets.isEmpty()) {
                    sendPartyMessage(
                        "Mutual Timesave Sparkling Critters: None"
                    )
                    return@execute
                }

                // Find sparkling critters that EVERY
                // party member has.
                val mutualSparklingCritters =
                    allSparklingSets.reduce { mutuals, playerCritters ->
                        mutuals.intersect(playerCritters)
                    }

                val results = mutableListOf<String>()

                // Check individual Timesave critters.
                for (critter in individualTimesaveCritters) {

                    if (critter in mutualSparklingCritters) {
                        results.add(formatCritterName(critter))
                    }
                }

                // All Birds requires Bluebird,
                // Parakeet AND Macaw to all be mutual.
                val allBirds =
                    "BLUEBIRD" in mutualSparklingCritters &&
                            "PARAKEET" in mutualSparklingCritters &&
                            "MACAW" in mutualSparklingCritters

                if (allBirds) {
                    results.add("All Birds")
                }

                val message =
                    if (results.isEmpty()) {
                        "Mutual Timesave Sparkling Critters: None"
                    } else {
                        "Mutual Timesave Sparkling Critters: " +
                                results.joinToString(", ")
                    }

                sendPartyMessage(message)

            } catch (e: Exception) {

                println(
                    "❌ Mutual sparkling lookup failed: $e"
                )

                sendClientMessage(
                    "Failed to calculate mutual sparkling critters."
                )
            }
        }
    }

    private fun getPlayerSparklingCritters(
        playerUuid: String
    ): Set<String>? {

        val now = System.currentTimeMillis()

        val cached = sparklingCache[playerUuid]

        if (
            cached != null &&
            now - cached.timestamp < CACHE_DURATION_MS
        ) {
            println(
                "Using cached sparkling data for $playerUuid"
            )

            return cached.critters
        }

        println(
            "Fetching fresh sparkling data for $playerUuid"
        )

        val profileUuid =
            HypixelApi.getCurrentProfileUuid(playerUuid)
                ?: return null

        val sparklingCritters =
            HypixelApi.getSparklingCritters(
                playerUuid,
                profileUuid
            )
                ?: return null

        sparklingCache[playerUuid] =
            CachedSparklingData(
                critters = sparklingCritters,
                timestamp = now
            )

        return sparklingCritters
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