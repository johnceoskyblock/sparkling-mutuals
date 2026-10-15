package buildlogic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

abstract class PublishReleaseTask : DefaultTask() {
    @get:Input abstract val modVersion: Property<String>
    @get:Input abstract val targetProperties: MapProperty<String, String>
    @get:InputDirectory abstract val distributionDirectory: DirectoryProperty

    @TaskAction fun publish() {
        val env = System.getenv()
        val version = modVersion.get()
        require(env["CI"] == "true" && env["GITHUB_EVENT_NAME"] == "push" && env["GITHUB_REF"] == "refs/tags/v$version" &&
            env["GITHUB_REPOSITORY"] == "johnceoskyblock/sparkling-mutuals") { "Publishing is restricted to this repository's matching tag-push workflow" }
        val sha = requireNotNull(env["GITHUB_SHA"])
        require(sha.matches(Regex("[a-f0-9]{40}"))) { "Missing immutable workflow commit" }
        val targets = targetProperties.get().map { (target, props) ->
            val (fabric, hm, moul) = props.split('|')
            TargetSpec(target, fabric, hm, moul)
        }
        val files = verifyDistribution(distributionDirectory.get().asFile.listFiles()!!.filter { it.extension == "jar" }.map { it.toPath() }, targets, version)
        val token = requireNotNull(env["GITHUB_TOKEN"]?.takeIf { it.isNotBlank() }) { "Missing GitHub token" }
        val api = GitHubReleaseApi(token, sha)
        api.verifyTag("v$version")
        val decision = publishVerifiedRelease(api, "v$version", version, files)
        logger.lifecycle("Release v$version: $decision")
    }
}

/** Fixed official hosts, bounded responses, no credential-bearing redirects or response-body logging. */
internal class GitHubReleaseApi(private val token: String, private val commit: String) : ReleaseApi {
    private val repository = "johnceoskyblock/sparkling-mutuals"
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val root = "https://api.github.com/repos/$repository"

    private fun request(method: String, url: String, body: HttpRequest.BodyPublisher = HttpRequest.BodyPublishers.noBody(), contentType: String = "application/json", allowMissing: Boolean = false): Any? {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host in setOf("api.github.com", "uploads.github.com") && uri.userInfo == null)
        val response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5))
            .header("Authorization", "Bearer $token").header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28").header("Content-Type", contentType).method(method, body).build(), HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { input ->
            if (allowMissing && response.statusCode() == 404) return null
            require(response.statusCode() in 200..299) { "GitHub $method request failed (${response.statusCode()}); draft remains unpublished if staging was incomplete" }
            val bytes = input.readNBytes(4 * 1024 * 1024 + 1)
            require(bytes.size <= 4 * 1024 * 1024) { "GitHub response exceeded limit" }
            return if (bytes.isEmpty()) null else JsonSlurper().parseText(bytes.toString(StandardCharsets.UTF_8))
        }
    }
    private fun jsonBody(value: Any) = HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(value))
    private fun obj(value: Any?) = value as? Map<*, *> ?: throw IllegalArgumentException("Invalid GitHub response")
    private fun id(value: Any?) = (value as? Number)?.toLong() ?: throw IllegalArgumentException("Missing GitHub identifier")
    private fun asset(value: Any?): ReleaseAsset {
        val a = obj(value)
        return ReleaseAsset(id(a["id"]), a["name"] as String, id(a["size"]), a["digest"] as? String)
    }
    fun verifyTag(tag: String) {
        var target = obj(obj(request("GET", "$root/git/ref/tags/$tag"))["object"])
        repeat(5) {
            when (target["type"]) {
                "commit" -> { require(target["sha"] == commit) { "Tag no longer points to the tested workflow commit" }; return }
                "tag" -> target = obj(obj(request("GET", "$root/git/tags/${target["sha"]}"))["object"])
                else -> throw IllegalArgumentException("Tag does not point to a commit")
            }
        }
        throw IllegalArgumentException("Too many annotated tag indirections")
    }
    override fun find(tag: String): RemoteRelease? {
        val release = request("GET", "$root/releases/tags/$tag", allowMissing = true)?.let(::obj) ?: return null
        val releaseId = id(release["id"])
        val assets = mutableListOf<ReleaseAsset>()
        var page = 1
        while (true) {
            require(page <= 100) { "Too many release asset pages" }
            val batch = request("GET", "$root/releases/$releaseId/assets?per_page=100&page=$page") as? List<*> ?: throw IllegalArgumentException("Missing assets")
            assets += batch.map(::asset)
            if (batch.size < 100) break
            page++
        }
        require(release["tag_name"] == tag) { "Release tag mismatch" }
        return RemoteRelease(releaseId, ExistingRelease(release["draft"] == true, release["body"] as? String, assets))
    }
    override fun createDraft(tag: String, body: String): RemoteRelease {
        verifyTag(tag)
        val created = obj(request("POST", "$root/releases", jsonBody(mapOf("tag_name" to tag, "target_commitish" to commit,
            "name" to tag, "body" to body, "draft" to true, "prerelease" to false))))
        require(created["draft"] == true && created["tag_name"] == tag) { "Expected same-tag draft" }
        return RemoteRelease(id(created["id"]), ExistingRelease(true, created["body"] as? String, emptyList()))
    }
    override fun generatedNotes(tag: String): String = obj(request("POST", "$root/releases/generate-notes", jsonBody(mapOf("tag_name" to tag, "target_commitish" to commit))))["body"] as String
    override fun updateBody(id: Long, body: String) { request("PATCH", "$root/releases/$id", jsonBody(mapOf("body" to body))) }
    override fun deleteAsset(id: Long) { request("DELETE", "$root/releases/assets/$id") }
    override fun upload(id: Long, file: VerifiedJar): ReleaseAsset {
        val name = URLEncoder.encode(file.path.fileName.toString(), StandardCharsets.UTF_8)
        return asset(request("POST", "https://uploads.github.com/repos/$repository/releases/$id/assets?name=$name",
            HttpRequest.BodyPublishers.ofFile(file.path), "application/java-archive"))
    }
    override fun publish(id: Long) { request("PATCH", "$root/releases/$id", jsonBody(mapOf("draft" to false))) }
}
