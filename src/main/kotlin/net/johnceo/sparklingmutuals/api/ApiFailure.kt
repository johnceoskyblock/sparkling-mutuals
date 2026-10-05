package net.johnceo.sparklingmutuals.api

import com.google.gson.JsonParser

enum class ApiFailureKind { MISSING_KEY, INVALID_KEY, RATE_LIMIT, DATA_UNAVAILABLE, PLAYER_NOT_FOUND, UNAVAILABLE }

class ApiFailure(val kind: ApiFailureKind, message: String) : RuntimeException(message) {
    companion object {
        fun dataUnavailable() = ApiFailure(ApiFailureKind.DATA_UNAVAILABLE,
            "SkyBlock profile or Safari data is unavailable. Check the selected profile and API availability.")
        fun unavailable() = ApiFailure(ApiFailureKind.UNAVAILABLE,
            "The API is unavailable or the connection timed out. Try again shortly.")
        private fun invalidKey() = ApiFailure(ApiFailureKind.INVALID_KEY,
            "The Hypixel API key is invalid or access was denied. Update it with /apikey or in settings.")
        private fun rateLimit() = ApiFailure(ApiFailureKind.RATE_LIMIT,
            "The API request limit was reached. Wait before trying again.")

        fun fromResponse(status: Int, body: String): ApiFailure? {
            if (status == 429) return rateLimit()
            if (status == 401 || status == 403) return invalidKey()
            if (status != 200) return unavailable()
            return try {
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("success")?.asBoolean != true) {
                    val cause = json.get("cause")?.asString.orEmpty().lowercase()
                    when {
                        json.get("throttle")?.asBoolean == true || "limit" in cause || "throttl" in cause -> rateLimit()
                        "key" in cause || "unauthor" in cause -> invalidKey()
                        else -> unavailable()
                    }
                } else null
            } catch (_: Exception) { unavailable() }
        }
    }
}
