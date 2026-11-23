package net.johnceo.sparklingmutuals.update

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI


class FullUpdateManifestTest {
    private val hash = "0123456789abcdef".repeat(4)
    private val name = "sparkling-mutuals-4.1.0-full+mc26.3.jar"

    private fun row(
        edition: String = "full", minecraft: String = "26.3", filename: String = name,
        size: String = "50000", sha256: String = hash,
    ) = """{"edition":"$edition","minecraft":"$minecraft","name":"$filename","size":$size,"sha256":"$sha256"}"""

    private fun manifest(vararg files: String, version: String = "4.1.0") =
        """{"version":"$version","files":[${files.joinToString()}]}"""

    @Test fun `loads the exact Full entry alongside other distribution entries`() {
        val result = FullUpdateManifest.artifact(manifest(
            row("standard", filename = "sparkling-mutuals-4.1.0+mc26.3.jar"),
            row(minecraft = "26.2", filename = "sparkling-mutuals-4.1.0-full+mc26.2.jar"),
            row(),
        ), "4.1.0", "26.3")
        assertEquals("4.1.0", result.version())
        assertEquals(50_000L, result.size())
        assertEquals(hash, result.sha256())
    }

    @Test fun `rejects missing duplicate wrong or malformed manifest entries`() {
        listOf(
            manifest(), manifest(row(), row()), manifest(row(), version = "4.1.1"),
            manifest(row("noauto", filename = "sparkling-mutuals-full-noautoupdate-4.1.0+mc26.3.jar")),
            manifest(row(filename = name.replace("-full", ""))),
            manifest(row(minecraft = "26.2", filename = "sparkling-mutuals-4.1.0+mc26.2.jar")),
            manifest(row(filename = "../$name")), manifest(row(sha256 = "broken")),
            manifest(row(size = "67108865")), manifest(row(size = "-1")),
            manifest(row(size = "0.5")), manifest(row(size = "\"50000\"")),
        ).forEach { fixture ->
            assertThrows(Exception::class.java, { FullUpdateManifest.artifact(fixture, "4.1.0", "26.3") }, fixture)
        }
    }

    @Test fun `allows redirects only to GitHub asset hosts over HTTPS`() {
        assertTrue(UpdateTransport.trustedAssetRedirect(URI("https://release-assets.githubusercontent.com/a/b?signature=opaque")))
        assertTrue(UpdateTransport.trustedAssetRedirect(URI("https://objects.githubusercontent.com/a/b")))
        listOf(
            "http://release-assets.githubusercontent.com/a",
            "https://release-assets.githubusercontent.com.evil.example/a",
            "https://release-assets.githubusercontent.com:8443/a",
            "https://release-assets.githubusercontent.com@evil.example/a",
            "https://evil.example@objects.githubusercontent.com/a",
            "https://api.github.com/a",
        ).forEach { assertFalse(UpdateTransport.trustedAssetRedirect(URI(it)), it) }
    }
}
