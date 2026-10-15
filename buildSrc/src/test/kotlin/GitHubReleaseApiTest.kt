package buildlogic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI

class GitHubReleaseApiTest {
    private val sha = "a".repeat(40)
    private val tag = "v3.3.0"
    private fun release(id: Long, draft: Boolean = true) = mapOf("id" to id, "tag_name" to tag, "draft" to draft, "body" to "Maintainer notes")
    private fun asset() = mapOf("id" to 12L, "name" to "sparkling-mutuals-3.3.0+mc26.2.jar", "size" to 123L, "digest" to "sha256:${"a".repeat(64)}")

    @Test fun existingDraftOnALaterPageRetainsNotesAndPartialUploadsWithoutCreatingAgain() {
        val calls = mutableListOf<String>()
        val api = GitHubReleaseApi(sha, ReleaseTransport { method, url, _, _, _ ->
            val route = URI(url).let { it.path.substringAfter("/sparkling-mutuals") + (it.rawQuery?.let { q -> "?$q" } ?: "") }
            calls += "$method $route"
            when (route) {
                "/releases/tags/$tag" -> null // Published-release lookup cannot discover this draft.
                "/releases?per_page=100&page=1" -> (1..100).map { mapOf("id" to it, "tag_name" to "v0.0.$it", "draft" to false) }
                "/releases?per_page=100&page=2" -> listOf(release(777))
                "/releases/777" -> release(777)
                "/releases/777/assets?per_page=100&page=1" -> listOf(asset())
                else -> throw AssertionError("Unexpected request: $method $route")
            }
        })
        val found = requireNotNull(api.find(tag))
        assertEquals(777, found.id); assertTrue(found.state.draft); assertEquals("Maintainer notes",found.state.body)
        assertEquals(listOf(ReleaseAsset(12,"sparkling-mutuals-3.3.0+mc26.2.jar",123,"sha256:${"a".repeat(64)}")),found.state.assets)
        val before = calls.size
        assertEquals(found,api.find(tag))
        assertEquals(listOf("GET /releases/777","GET /releases/777/assets?per_page=100&page=1"),calls.drop(before))
        assertTrue(calls.all { it.startsWith("GET ") })
    }

    @Test fun newlyCreatedDraftIsReadBackByItsKnownIdInsteadOfPublishedTagLookup() {
        val calls = mutableListOf<String>()
        val api = GitHubReleaseApi(sha, ReleaseTransport { method, url, _, _, _ ->
            val route = URI(url).let { it.path.substringAfter("/sparkling-mutuals") + (it.rawQuery?.let { q -> "?$q" } ?: "") }
            calls += "$method $route"
            when ("$method $route") {
                "GET /git/ref/tags/$tag" -> mapOf("object" to mapOf("type" to "commit","sha" to sha))
                "POST /releases" -> release(778)
                "GET /releases/778" -> release(778)
                "GET /releases/778/assets?per_page=100&page=1" -> emptyList<Any>()
                "GET /releases/tags/$tag" -> null
                else -> throw AssertionError("Unexpected request: $method $route")
            }
        })
        api.createDraft(tag,"Maintainer notes")
        assertEquals(778,requireNotNull(api.find(tag)).id)
        assertFalse(calls.any { it.contains("/releases/tags/") })
    }

    @Test fun ambiguousSameTagDraftsAreRejectedRatherThanChosenArbitrarily() {
        val api = GitHubReleaseApi(sha, ReleaseTransport { method, url, _, _, _ ->
            require(method=="GET")
            if(url.contains("/releases/tags/")) null else listOf(release(1),release(2))
        })
        assertThrows(IllegalArgumentException::class.java) { api.find(tag) }
    }
}
