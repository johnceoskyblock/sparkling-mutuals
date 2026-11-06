package buildlogic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.http.HttpRequest
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import groovy.json.JsonSlurper

class BetaReleaseClassificationTest {
    private val sha = "a".repeat(40)
    private fun body(publisher: HttpRequest.BodyPublisher): Map<*, *> {
        val bytes = ByteArrayOutputStream(); val complete = CompletableFuture<String>()
        publisher.subscribe(object : Flow.Subscriber<ByteBuffer> {
            override fun onSubscribe(subscription: Flow.Subscription) { subscription.request(Long.MAX_VALUE) }
            override fun onNext(value: ByteBuffer) { val next = ByteArray(value.remaining()); value.get(next); bytes.write(next) }
            override fun onError(error: Throwable) { complete.completeExceptionally(error) }
            override fun onComplete() { complete.complete(bytes.toString(Charsets.UTF_8)) }
        })
        return JsonSlurper().parseText(complete.get(5, TimeUnit.SECONDS)) as Map<*, *>
    }
    private fun files(version: String) = releaseFiles().map { it.copy(modVersion=version,
        path=Path.of("sparkling-mutuals-$version+mc${it.minecraft}.jar")) }
    private fun existing(version: String, draft: Boolean, prerelease: Boolean?): ExistingRelease {
        val api = GitHubReleaseApi(sha, ReleaseTransport { method, url, _, _, _ ->
            require(method == "GET")
            when {
                url.contains("/releases/tags/") -> mutableMapOf<String, Any>("id" to 123L,
                    "tag_name" to "v$version", "draft" to draft, "body" to "Written notes").also {
                        if (prerelease != null) it["prerelease"] = prerelease
                    }
                url.contains("/assets?") -> files(version).mapIndexed { i, f -> mapOf("id" to (i+1).toLong(),
                    "name" to f.path.fileName.toString(), "size" to f.size, "digest" to "sha256:${f.sha256}") }
                else -> throw AssertionError("Unexpected request")
            }
        })
        return requireNotNull(api.find("v$version")).state
    }
    @Test fun betaDraftIsPrereleaseAndCannotBecomeLatestWhileStableRetainsItsExistingDefault() {
        for ((tag, prerelease) in listOf("v3.3.0" to false, "v3.3.0-beta.1" to true)) {
            var request: Map<*, *>? = null; var publication: Map<*, *>? = null
            val api = GitHubReleaseApi(sha, ReleaseTransport { method, url, payload, _, _ ->
                when (method) {
                    "GET" -> mapOf("object" to mapOf("type" to "commit", "sha" to sha))
                    "POST" -> {
                        assertTrue(url.endsWith("/releases")); request = body(payload)
                        mapOf("id" to 123L, "tag_name" to tag, "draft" to true, "prerelease" to prerelease, "body" to "Notes")
                    }
                    "PATCH" -> { publication = body(payload); null }
                    else -> throw AssertionError("Unexpected request")
                }
            })
            api.createDraft(tag, "Notes")
            assertEquals(prerelease, request?.get("prerelease")); assertEquals(true, request?.get("draft"))
            assertEquals(if (prerelease) "false" else "true", request?.get("make_latest"))
            api.publish(123)
            assertEquals(false, publication?.get("draft"))
            assertEquals(if (prerelease) "false" else "true", publication?.get("make_latest"))
        }
    }
    @Test fun classificationMismatchRejectsBothDraftsAndPublishedNoops() {
        for (draft in listOf(true, false)) {
            assertThrows(IllegalArgumentException::class.java) {
                planRelease("v3.3.0", "3.3.0", files("3.3.0"), existing("3.3.0", draft, true))
            }
            assertThrows(IllegalArgumentException::class.java) {
                planRelease("v3.3.0-beta.1", "3.3.0-beta.1", files("3.3.0-beta.1"), existing("3.3.0-beta.1", draft, false))
            }
        }
    }
    @Test fun missingClassificationMetadataFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) { existing("3.3.0", true, null) }
    }
    @Test fun correctlyClassifiedBetaDraftAndPublishedNoopRemainEligible() {
        val version = "3.3.0-beta.1"
        assertEquals(ReleaseDecision.UPDATE_DRAFT, planRelease("v$version", version, files(version), existing(version, true, true)).decision)
        assertEquals(ReleaseDecision.PUBLISHED_NOOP, planRelease("v$version", version, files(version), existing(version, false, true)).decision)
    }
}
