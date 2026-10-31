package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.honeyhives.Hive

object HypixelApi {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val profileCache=TimedLookupCache<Pair<ApiProvider,String>,JsonObject>()
    private val nameCache=TimedLookupCache<String,String>()
    private val discoveryUpdates=ProfileDiscoveryOverlay<Pair<ApiProvider,String>>()

    /** Lookups go through Shiny Hunter's shared API when no key of your own is set. */
    val provider get() = ApiProvider.capture()
    val usingCloud get() = provider.cloud

    private fun profiles(uuid: String, fresh: Boolean = false): JsonObject {
        val id=uuid.replace("-","").lowercase(java.util.Locale.ROOT)
        val source=provider
        val key=source to id
        val read={
            val body=if (source.cloud) CloudApi.profiles(id) else get("https://api.hypixel.net/v2/skyblock/profiles?uuid=$id")
            val selected=CloudProfiles.selectedProfileId(body) ?: throw ApiFailure.dataUnavailable()
            SafariProfile.discoveriesFrom(CloudProfiles.member(body,id,selected) ?: throw ApiFailure.dataUnavailable())
            body
        }
        val body=if (fresh) read().also { profileCache.put(key,it) } else profileCache.load(key,read)
        return discoveryUpdates.apply(key,id,body)
    }
    fun clearProfileCache() { profileCache.clear();discoveryUpdates.clear() }
    fun recordSparkling(participants: Set<String>,species: String) {
        val source=provider
        participants.forEach { uuid ->
            val id=uuid.replace("-","").lowercase(java.util.Locale.ROOT)
            val key=source to id
            val baseline=profileCache.peek(key)?.let { body ->
                CloudProfiles.selectedProfileId(body)?.let { CloudProfiles.member(body,id,it) }
            }?.get("safari")?.takeIf { it.isJsonObject }?.asJsonObject?.get("total_captured_sparkling_critters")?.takeIf {
                it.isJsonPrimitive && it.asJsonPrimitive.isNumber
            }?.asLong
            discoveryUpdates.caught(key,species,baseline)
        }
    }

    fun getCurrentProfileUuid(uuid: String, fresh: Boolean = false): String = data {
        CloudProfiles.selectedProfileId(profiles(uuid,fresh)) ?: throw ApiFailure.dataUnavailable()
    }

    fun getSparklingCritters(playerUuid: String, profileUuid: String): Set<String> = data {
        val source = provider
        val observation = net.johnceo.sparklingmutuals.commands.SafariLookup.discoveryObservation(playerUuid, source)
        SafariProfile.discoveriesFrom(getMember(playerUuid, profileUuid)).also {
            if (source == provider && observation != null) net.johnceo.sparklingmutuals.commands.SafariLookup.observeDiscoveries(playerUuid, source, it, observation)
        }
    }

    fun getPlayerUuid(username: String): String = nameCache.load(username.lowercase(java.util.Locale.ROOT)) {
        val response = send(HttpRequest.newBuilder()
            .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/$username")))
        if (response.statusCode() == 204 || response.statusCode() == 404) {
            throw ApiFailure(ApiFailureKind.PLAYER_NOT_FOUND, "Minecraft player not found: $username")
        }
        if (response.statusCode() == 429) throw ApiFailure(ApiFailureKind.RATE_LIMIT,
            "Player lookup request limit reached. Try again shortly.")
        if (response.statusCode() != 200) throw ApiFailure.unavailable()
        try { JsonParser.parseString(response.body()).asJsonObject.get("id").asString }
        catch (_: Exception) { throw ApiFailure.unavailable() }
    }

    fun getSafariTickets(playerUuid: String, profileUuid: String): Map<String, Long> = data {
        val tickets = getMember(playerUuid, profileUuid).getAsJsonObject("safari")
            ?.getAsJsonObject("tickets") ?: throw ApiFailure.dataUnavailable()
        listOf("basic", "economy", "premium", "first_class").associateWith { name ->
            tickets.get(name)?.takeUnless { it.isJsonNull }?.asLong ?: 0L
        }
    }

    fun getSafariProfile(playerUuid: String): SafariProfile = data {
        val source = provider
        val observation = net.johnceo.sparklingmutuals.commands.SafariLookup.discoveryObservation(playerUuid, source)
        SafariProfile.from(getMember(playerUuid, getCurrentProfileUuid(playerUuid))).also {
            if (source == provider && observation != null) net.johnceo.sparklingmutuals.commands.SafariLookup.observeDiscoveries(playerUuid, source, it.discoveries, observation)
        }
    }

    /** Hive id -> when it refills, for hives looted in the last hour on any session. */
    fun getHoneyRefills(playerUuid: String): HoneyRefills = data {
        val body=profiles(playerUuid,fresh=true)
        val profile=CloudProfiles.selectedProfileId(body) ?: throw ApiFailure.dataUnavailable()
        HoneyRefills(profile,HoneyRefills.read(CloudProfiles.member(body,playerUuid,profile) ?: throw ApiFailure.dataUnavailable()))
    }

    private fun getMember(playerUuid: String, profileUuid: String): JsonObject = data {
        CloudProfiles.member(profiles(playerUuid),playerUuid,profileUuid) ?: throw ApiFailure.dataUnavailable()
    }

    private fun get(url: String): JsonObject {
        val key = ConfigManager.apiKey.trim()
        if (key.isEmpty()) throw ApiFailure(ApiFailureKind.MISSING_KEY,
            "No Hypixel API key. Use /apikey or settings.")
        // Keep credentials out of URLs and diagnostic messages.
        val response = send(HttpRequest.newBuilder().uri(URI.create(url)).header("API-Key", key))
        ApiFailure.fromResponse(response.statusCode(), response.body())?.let { throw it }
        return JsonParser.parseString(response.body()).asJsonObject
    }

    private fun send(builder: HttpRequest.Builder): HttpResponse<String> = try {
        client.send(builder.timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString())
    } catch (_: Exception) { throw ApiFailure.unavailable() }

    private fun <T> data(read: () -> T): T = try { read() }
        catch (failure: ApiFailure) { throw failure }
        catch (_: Exception) { throw ApiFailure.malformedData() }
}
