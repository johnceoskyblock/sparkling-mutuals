package net.johnceo.sparklingmutuals.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReleaseUpdateCheckerTest {
    private fun row(version: String, mc: String = "26.3", flags: String = "") =
        """{"tag_name":"v$version","name":"v$version","body":null,"assets":[{"name":"sparkling-mutuals-$version+mc$mc.jar"}]$flags}"""

    @Test fun `download links must identify the exact repository tag and Full Minecraft asset`() {
        val prefix = "https://github.com/johnceoskyblock/sparkling-mutuals/releases/download/v4.1.0/"
        fun body(asset: String, manifest: String) = """[{"tag_name":"v4.1.0","assets":[{"name":"sparkling-mutuals-4.1.0+mc26.3.jar","browser_download_url":"$asset"},{"name":"build-manifest.json","browser_download_url":"$manifest"}]}]"""
        val name = "sparkling-mutuals-4.1.0+mc26.3.jar"
        val valid = ReleaseUpdateChecker.newest(body(prefix+name.replace("+", "%2B"), prefix+"build-manifest.json"), "26.3")!!
        assertEquals(prefix+name, valid.asset.toString())
        assertNotNull(valid.manifest)
        for (url in listOf("https://example.com/$name", prefix.replace("v4.1.0", "v4.2.0")+name,
            prefix+name.replace("26.3", "26.2"), prefix+name+"?download=1")) {
            assertNull(ReleaseUpdateChecker.newest(body(url, prefix+"build-manifest.json"), "26.3")!!.asset)
        }
        assertNull(ReleaseUpdateChecker.newest(body(prefix+name, "https://example.com/build-manifest.json"), "26.3")!!.manifest)
    }

    @Test fun `update callback runs only for a newer compatible stable release`() {
        var body = "[${row("4.0.0")}]"
        val updates = mutableListOf<String>()
        val checker = ReleaseUpdateChecker("4.0.0", "26.3", { body }, { it() }, onUpdate = { updates.add(it.tag) })
        checker.check()
        body = "[${row("4.1.0")}]"
        checker.check(force = true)
        assertEquals(listOf("v4.1.0"), updates)
    }

    @Test fun `final stable release upgrades the matching installed beta while preview releases remain excluded`() {
        val body = "[${row("4.0.0")},${row("4.1.0-beta.1", flags=",\"prerelease\":true")}]"
        val checker = ReleaseUpdateChecker("4.0.0-beta.1", "26.3", { body }, { it() })
        checker.check()
        assertEquals("Update available", checker.state.label)
        assertEquals("v4.0.0", checker.state.release?.tag)
    }

    @Test fun `version precedence compares preview identifiers numerically and ignores build metadata`() {
        assertTrue(ReleaseUpdateChecker.compareVersions("4.0.0", "4.0.0-beta.1")!! > 0)
        assertTrue(ReleaseUpdateChecker.compareVersions("4.0.0-beta.1", "4.0.0")!! < 0)
        assertTrue(ReleaseUpdateChecker.compareVersions("4.0.0-beta.11", "4.0.0-beta.2")!! > 0)
        assertEquals(0, ReleaseUpdateChecker.compareVersions("4.0.0-beta.1+mc26.3", "v4.0.0-beta.1+other"))
        assertEquals(0, ReleaseUpdateChecker.compareVersions("4.0.0+mc26.3", "v4.0.0+other"))
    }

    @Test fun `new release replaces current status without reopening settings and manual check bypasses TTL`() {
        var now = 0L
        var body = "[${row("4.0.0")}]"
        val tasks = mutableListOf<() -> Unit>()
        val checker = ReleaseUpdateChecker("4.0.0", "26.3", { body }, { tasks.add(it) }, { now })
        checker.check(); checker.check(force = true)
        assertEquals(1, tasks.size)
        tasks.removeAt(0)()
        assertEquals("Up to Date", checker.state.label)
        body = "[${row("4.0.1")}]"
        now = 299_999
        checker.check(); assertTrue(tasks.isEmpty())
        now++
        checker.check(); tasks.removeAt(0)()
        assertEquals("Update available", checker.state.label)
        assertEquals("v4.0.1", checker.state.release?.tag)
        body = "[${row("4.1.0")}]"
        checker.check(force = true); tasks.removeAt(0)()
        assertEquals("v4.1.0", checker.state.release?.tag)
    }

    @Test fun `failed refresh cannot leave up to date status and retries after one minute`() {
        var now = 0L
        var fail = false
        var calls = 0
        val checker = ReleaseUpdateChecker("4.0.0", "26.3", {
            calls++; if (fail) error("HTTP failure") else "[${row("4.0.0")}]"
        }, { it() }, { now })
        checker.check(); fail = true
        checker.check(force = true)
        assertEquals("Couldn't check", checker.state.label)
        now = 59_999; checker.check(); assertEquals(2, calls)
        fail = false; now++; checker.check()
        assertEquals(3, calls); assertEquals("Up to Date", checker.state.label)
    }

    @Test fun `release selection rejects drafts previews missing targets malformed tags and sorts numerically`() {
        val body = "[${row("3.10.0")},${row("3.9.9")},${row("5.0.0",flags=",\"draft\":true")}," +
            "${row("6.0.0",flags=",\"prerelease\":true")},${row("7.0.0","26.2")},${row("invalid")}]"
        assertEquals("v3.10.0", ReleaseUpdateChecker.newest(body,"26.3")?.tag)
        assertNull(ReleaseUpdateChecker.newest("[${row("4.0.0","26.2")}]","26.3"))
        assertTrue(ReleaseUpdateChecker.compareVersions("v4.0.0","3.6.4")!! > 0)
        assertEquals(0,ReleaseUpdateChecker.compareVersions("v4.0.0","4.0.0+mc26.3"))
    }
}
