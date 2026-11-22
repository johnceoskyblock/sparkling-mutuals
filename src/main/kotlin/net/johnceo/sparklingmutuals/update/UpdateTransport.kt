package net.johnceo.sparklingmutuals.update

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal object FullUpdateManifest {
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).create()

    fun artifact(body: String, version: String, minecraft: String): UpdateInstall.Artifact {
        val manifest = gson.fromJson(body, JsonObject::class.java)
        require(manifest.get("version").asString == version) { "Wrong manifest version" }
        val row = manifest.getAsJsonArray("files").map { it.asJsonObject }.filter {
            it.get("edition").asString == "full" && it.get("minecraft").asString == minecraft
        }.singleOrNull() ?: error("Expected one Full artifact for Minecraft $minecraft")
        require(row.get("name").asString == "sparkling-mutuals-$version+mc$minecraft.jar") { "Wrong manifest filename" }
        val size = row.getAsJsonPrimitive("size")
        require(size.isNumber && size.asString.matches(Regex("[0-9]+"))) { "Invalid artifact size" }
        return UpdateInstall.Artifact(version, size.asLong, row.get("sha256").asString)
    }
}

internal object UpdateTransport {
    fun trustedAssetRedirect(uri: URI): Boolean =
        uri.scheme == "https" && uri.userInfo == null && uri.fragment == null && uri.port == -1 &&
            uri.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")

    fun metadata(uri: URI, limit: Int, githubApi: Boolean = false): String =
        read(uri, githubApi) { stream ->
            val data = stream.readNBytes(limit + 1)
            require(data.size <= limit) { "Release metadata exceeds the download limit" }
            data.toString(Charsets.UTF_8)
        }

    fun <T> read(source: URI, githubApi: Boolean = false, consume: (InputStream) -> T): T {
        val expectedHost = if (githubApi) "api.github.com" else "github.com"
        require(source.host == expectedHost && source.scheme == "https" && source.userInfo == null &&
            source.port == -1 && source.fragment == null) { "Unexpected update origin" }
        var uri = source
        repeat(4) {
            val connection = uri.toURL().openConnection() as HttpsURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Sparkling-Mutuals-Update")
            if (githubApi) connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                when (val response = connection.responseCode) {
                    200 -> return connection.inputStream.use(consume)
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: error("Missing update redirect")
                        uri = uri.resolve(location)
                        require(!githubApi && trustedAssetRedirect(uri)) { "Unexpected update redirect" }
                    }
                    else -> throw IOException("Update server returned HTTP $response")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Too many update redirects")
    }
}
