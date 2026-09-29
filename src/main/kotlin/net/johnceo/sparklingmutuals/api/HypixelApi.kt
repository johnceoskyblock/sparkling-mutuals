package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.johnceo.sparklingmutuals.config.ConfigManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object HypixelApi {

    private val client = HttpClient.newHttpClient()

    fun getCurrentProfileUuid(uuid: String): String? {
        return try {
            val data = get(
                "https://api.hypixel.net/v2/skyblock/profiles?key=${ConfigManager.apiKey}&uuid=$uuid"
            )

            if (!data.get("success").asBoolean || !data.has("profiles") || data.get("profiles").isJsonNull) {
                throw RuntimeException("No SkyBlock profiles found")
            }

            data.getAsJsonArray("profiles")
                .firstOrNull { it.asJsonObject.get("selected")?.asBoolean == true }
                ?.asJsonObject
                ?.get("profile_id")
                ?.asString
                ?: throw RuntimeException("No selected SkyBlock profile found")
        } catch (e: Exception) {
            println("❌ Selected profile lookup failed for UUID $uuid: $e")
            null
        }
    }

    fun getSparklingCritters(playerUuid: String, profileUuid: String): Set<String>? {
        return try {
            val member = getMember(playerUuid, profileUuid)
            val safari = member.getAsJsonObject("safari")
                ?: throw RuntimeException("Safari data not found")

            safari.getAsJsonArray("discovered_sparkling_critters")
                .map { it.asString }
                .toSet()
        } catch (e: Exception) {
            println("❌ Sparkling critter fetch failed for UUID $playerUuid: $e")
            null
        }
    }

    fun getPlayerUuid(username: String): String? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$username"))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            when (response.statusCode()) {
                204, 404 -> throw RuntimeException("Minecraft player not found: $username")
                200 -> JsonParser.parseString(response.body()).asJsonObject.get("id").asString
                else -> throw RuntimeException("Mojang API error: HTTP ${response.statusCode()}")
            }
        } catch (e: Exception) {
            println("❌ UUID lookup failed for $username: $e")
            null
        }
    }

    fun getSafariTickets(playerUuid: String, profileUuid: String): Map<String, Long>? {
        return try {
            val tickets = getMember(playerUuid, profileUuid)
                .getAsJsonObject("safari")
                ?.getAsJsonObject("tickets")
                ?: throw RuntimeException("Safari tickets data not found")

            mapOf(
                "basic" to tickets.get("basic").asLong,
                "economy" to tickets.get("economy").asLong,
                "premium" to tickets.get("premium").asLong,
                "first_class" to tickets.get("first_class").asLong
            )
        } catch (e: Exception) {
            println("❌ Safari ticket lookup failed for UUID $playerUuid: $e")
            null
        }
    }

    private fun getMember(playerUuid: String, profileUuid: String): JsonObject {
        val data = get(
            "https://api.hypixel.net/v2/skyblock/profile?key=${ConfigManager.apiKey}&profile=$profileUuid"
        )

        val profile = data.getAsJsonObject("profile")
        val members = profile.getAsJsonObject("members")
        val memberUuid = playerUuid.replace("-", "")

        return members.get(memberUuid)?.takeIf { !it.isJsonNull }?.asJsonObject
            ?: throw RuntimeException("Member data not found for UUID $playerUuid")
    }

    private fun get(url: String): JsonObject {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("Hypixel API error: HTTP ${response.statusCode()}")
        }

        return JsonParser.parseString(response.body()).asJsonObject
    }
}