package net.johnceo.sparklingmutuals

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object HypixelApi {

    private val client = HttpClient.newHttpClient()

    fun getCurrentProfileUuid(uuid: String): String? {

        try {

            val url =
                "https://api.hypixel.net/v2/skyblock/profiles" +
                        "?key=${ConfigManager.apiKey}" +
                        "&uuid=$uuid"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            if (response.statusCode() != 200) {
                throw RuntimeException(
                    "Hypixel API error: HTTP ${response.statusCode()}"
                )
            }

            val data = com.google.gson.JsonParser
                .parseString(response.body())
                .asJsonObject

            if (
                !data.get("success").asBoolean ||
                !data.has("profiles") ||
                data.get("profiles").isJsonNull
            ) {
                throw RuntimeException(
                    "No SkyBlock profiles found"
                )
            }

            val profiles = data.getAsJsonArray("profiles")

            val selectedProfile = profiles.firstOrNull { profile ->
                profile.asJsonObject
                    .get("selected")
                    ?.asBoolean == true
            }

            if (selectedProfile == null) {
                throw RuntimeException(
                    "No selected SkyBlock profile found"
                )
            }

            return selectedProfile
                .asJsonObject
                .get("profile_id")
                .asString

        } catch (e: Exception) {

            println(
                "❌ Selected profile lookup failed for UUID $uuid: $e"
            )

            return null
        }
    }

    fun getSparklingCritters(
        playerUuid: String,
        profileUuid: String
    ): Set<String>? {

        try {

            val url =
                "https://api.hypixel.net/v2/skyblock/profile" +
                        "?key=${ConfigManager.apiKey}" +
                        "&profile=$profileUuid"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            if (response.statusCode() != 200) {
                throw RuntimeException(
                    "API error: HTTP ${response.statusCode()}"
                )
            }

            val data = com.google.gson.JsonParser
                .parseString(response.body())
                .asJsonObject

            val profile = data
                .getAsJsonObject("profile")

            val members = profile
                .getAsJsonObject("members")

            val memberUuid = playerUuid.replace("-", "")

            val member = members.get(memberUuid)

            if (member == null || member.isJsonNull) {
                throw RuntimeException(
                    "Member data not found for UUID $playerUuid"
                )
            }

            val memberObject = member.asJsonObject

            val safari = memberObject.get("safari")

            if (safari == null || safari.isJsonNull) {
                throw RuntimeException(
                    "Safari data not found"
                )
            }

            val sparklingCritters = safari
                .asJsonObject
                .getAsJsonArray("discovered_sparkling_critters")

            return sparklingCritters
                .map { it.asString }
                .toSet()

        } catch (e: Exception) {

            println(
                "❌ Sparkling critter fetch failed for UUID $playerUuid: $e"
            )

            return null
        }
    }

    fun getPlayerUuid(username: String): String? {

        try {

            val url =
                "https://api.mojang.com/users/profiles/minecraft/$username"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            if (response.statusCode() == 204 || response.statusCode() == 404) {
                throw RuntimeException(
                    "Minecraft player not found: $username"
                )
            }

            if (response.statusCode() != 200) {
                throw RuntimeException(
                    "Mojang API error: HTTP ${response.statusCode()}"
                )
            }

            val data = com.google.gson.JsonParser
                .parseString(response.body())
                .asJsonObject

            return data
                .get("id")
                .asString

        } catch (e: Exception) {

            println(
                "❌ UUID lookup failed for $username: $e"
            )

            return null
        }
    }

    fun getSafariTickets(
        playerUuid: String,
        profileUuid: String
    ): Map<String, Long>? {

        try {

            val url =
                "https://api.hypixel.net/v2/skyblock/profile" +
                        "?key=${ConfigManager.apiKey}" +
                        "&profile=$profileUuid"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build()

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )

            if (response.statusCode() != 200) {
                throw RuntimeException(
                    "Hypixel API error: HTTP ${response.statusCode()}"
                )
            }

            val data = com.google.gson.JsonParser
                .parseString(response.body())
                .asJsonObject

            val profile = data
                .getAsJsonObject("profile")

            val members = profile
                .getAsJsonObject("members")

            val memberUuid = playerUuid.replace("-", "")

            val member = members.get(memberUuid)

            if (member == null || member.isJsonNull) {
                throw RuntimeException(
                    "Member data not found for UUID $playerUuid"
                )
            }

            val safari = member
                .asJsonObject
                .getAsJsonObject("safari")

            if (safari == null) {
                throw RuntimeException(
                    "Safari data not found"
                )
            }

            val tickets = safari
                .getAsJsonObject("tickets")

            if (tickets == null) {
                throw RuntimeException(
                    "Safari tickets data not found"
                )
            }

            return mapOf(
                "basic" to tickets.get("basic").asLong,
                "economy" to tickets.get("economy").asLong,
                "premium" to tickets.get("premium").asLong,
                "first_class" to tickets.get("first_class").asLong
            )

        } catch (e: Exception) {

            println(
                "❌ Safari ticket lookup failed for UUID $playerUuid: $e"
            )

            return null
        }
    }
}