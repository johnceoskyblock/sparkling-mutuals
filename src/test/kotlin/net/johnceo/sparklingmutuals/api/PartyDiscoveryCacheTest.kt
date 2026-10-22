package net.johnceo.sparklingmutuals.api

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PartyDiscoveryCacheTest {
    private val provider = ApiProvider.capture("", true)

    @Test fun `roster additions fetch only new members and removal immediately releases their requirements`() {
        val cache = PartyDiscoveryCache()
        var fetches = 0
        cache.select(setOf("me", "old"), provider)
        cache.load("me", provider) { fetches++; setOf("GIMMIEGOLD") }
        cache.load("old", provider) { fetches++; emptySet() }
        cache.select(setOf("me", "new"), provider)
        assertEquals(setOf("GIMMIEGOLD"), cache.load("me", provider) { fail("retained member must not fetch") })
        cache.load("new", provider) { fetches++; emptySet() }
        assertEquals(3, fetches)
        val state = PartySparklingState()
        state.select(setOf("me", "new"), provider)
        state.accept(setOf("me", "new"), mapOf("me" to cache.known("me", provider)!!, "new" to cache.known("new", provider)!!), provider)
        assertTrue(state.needs("Gimmiegold"))
        cache.select(setOf("me"), provider)
        state.select(setOf("me"), provider)
        assertTrue(state.accept(setOf("me"), mapOf("me" to cache.known("me", provider)!!), provider))
        assertFalse(state.needs("Gimmiegold"))
        assertNull(cache.known("new", provider))
        cache.select(setOf("me", "new"), provider)
        cache.load("new", provider) { fetches++; emptySet() }
        assertEquals(4, fetches, "returning removed members get a fresh baseline")
    }

    @Test fun `party catch updates every participant and retains pre catch unique evidence without HTTP`() {
        val cache = PartyDiscoveryCache()
        val members = setOf("me", "friend")
        cache.select(members, provider)
        members.forEach { cache.load(it, provider) { emptySet() } }
        assertEquals(emptySet<String>(), cache.caught(members, "Mantis Shrimp", provider)["friend"])
        members.forEach { assertEquals(setOf("MANTIS_SHRIMP"), cache.load(it, provider) { fail("catch must not fetch") }) }
        assertEquals(setOf("MANTIS_SHRIMP"), cache.caught(members, "Mantis Shrimp", provider)["friend"])
        cache.caught(members, "Macaw", provider, setOf("me"))
        assertTrue("MACAW" in cache.known("me", provider)!!)
        assertFalse("MACAW" in cache.known("friend", provider)!!, "a member outside this run does not receive catch credit")
    }

    @Test fun `catch while initial fetch is pending survives the delayed API result`() {
        val cache = PartyDiscoveryCache()
        cache.select(setOf("me"), provider)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val task = CompletableFuture.supplyAsync {
            cache.load("me", provider) { started.countDown(); check(release.await(5, TimeUnit.SECONDS)); emptySet() }
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            assertNull(cache.caught(setOf("me"), "Macaw", provider)["me"])
        } finally { release.countDown() }
        assertEquals(setOf("MACAW"), task.get(5, TimeUnit.SECONDS))
        assertEquals(setOf("MACAW"), cache.known("me", provider))
    }

    @Test fun `partial failures retain successful members and provider changes invalidate baselines`() {
        val cache = PartyDiscoveryCache()
        cache.select(setOf("me", "friend"), provider)
        cache.load("me", provider) { setOf("GAZER") }
        assertThrows(ApiFailure::class.java) { cache.load("friend", provider) { throw ApiFailure.unavailable() } }
        assertEquals(setOf("GAZER"), cache.load("me", provider) { fail("successful members must survive retry") })
        assertEquals(emptySet<String>(), cache.load("friend", provider) { emptySet() })
        val changed = ApiProvider.capture("test-key", false)
        cache.select(setOf("me"), changed)
        assertNull(cache.known("me", changed))
        assertEquals(emptySet<String>(), cache.load("me", changed) { emptySet() })
    }
}
