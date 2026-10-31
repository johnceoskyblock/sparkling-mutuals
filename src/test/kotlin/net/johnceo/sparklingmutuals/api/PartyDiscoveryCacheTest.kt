package net.johnceo.sparklingmutuals.api

import net.johnceo.sparklingmutuals.safari.PartySparklingState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PartyDiscoveryCacheTest {
    private val provider = ApiProvider.capture("", true)
    @Test fun `warping disbands retain cached discoveries and expected leecher requirements without repeated HTTP`() {
        val cache=PartyDiscoveryCache()
        val party=linkedSetOf("me","leechA","leechB","leechC")
        cache.select(party,provider)
        party.forEach { who -> cache.load(who,provider) { if(who == "me") setOf("DOOMSPIRAL") else emptySet() } }
        cache.retain(party,provider)
        repeat(3) {
            cache.select(setOf("me"),provider)
            assertNotNull(cache.known("leechA",provider))
            assertEquals(emptySet<String>(),cache.load("leechA",provider) { fail("Reserved members remain readable while a command selects the actual solo roster") })
            val expected=net.johnceo.sparklingmutuals.alerts.SparkleWarpPolicy.requirementRoster(setOf("me"),party)
            cache.select(expected,provider)
            val state=PartySparklingState()
            state.select(expected,provider)
            assertTrue(state.accept(expected,expected.associateWith { who -> cache.load(who,provider) { fail("Reparty must reuse cached discoveries") } },provider))
            assertTrue(state.needs("Doomspiral"),"Incense remains needed for the returning leechers")
            cache.caught(setOf("me"),"Doomspiral",provider)
            assertFalse("DOOMSPIRAL" in cache.known("leechA",provider)!!,"Expected members outside the run do not get catch credit")
            cache.select(party,provider)
            party.forEach { who -> cache.load(who,provider) { fail("Joining again must not fetch") } }
        }
        cache.select(setOf("me"),provider)
        cache.retain(emptySet(),provider)
        assertNull(cache.known("leechA",provider))
        cache.retain(party,provider)
        cache.clear()
        assertTrue(cache.retainedMembers(provider).isEmpty())
    }

    @Test fun `delayed profile observations cannot cross membership provider or account realm resets`() {
        val cache = PartyDiscoveryCache()
        assertNull(cache.observationToken("me", provider))
        cache.select(setOf("me"), provider)
        val original = cache.observationToken("me", provider)!!
        cache.observe("me", provider, setOf("BLUEBIRD"), original)
        assertEquals(setOf("BLUEBIRD"), cache.known("me", provider))
        cache.select(setOf("me", "friend"), provider)
        cache.observe("me", provider, setOf("PARAKEET"), original)
        assertTrue("PARAKEET" in cache.known("me", provider)!!)
        cache.select(emptySet(), provider)
        cache.select(setOf("me"), provider)
        cache.observe("me", provider, setOf("MACAW"), original)
        assertNull(cache.known("me", provider))
        val rejoined = cache.observationToken("me", provider)!!
        cache.clear() // Account or realm changed.
        cache.select(setOf("me"), provider)
        cache.observe("me", provider, setOf("MACAW"), rejoined)
        assertNull(cache.known("me", provider))
        val oldProvider = cache.observationToken("me", provider)!!
        cache.select(setOf("me"), ApiProvider.capture("another-key", false))
        cache.select(setOf("me"), provider)
        cache.observe("me", provider, setOf("MACAW"), oldProvider)
        assertNull(cache.known("me", provider))
        val current = cache.observationToken("me", provider)!!
        cache.observe("me", provider, setOf("MACAW"), current)
        assertEquals(setOf("MACAW"), cache.known("me", provider))
    }

    @Test fun `existing profile lookups repair stale bird requirements without another fetch`() {
        val cache=PartyDiscoveryCache()
        val members=setOf("me","friend")
        cache.select(members,provider)
        cache.load("me",provider) { setOf("BLUEBIRD","PARAKEET","MACAW") }
        cache.load("friend",provider) { setOf("BLUEBIRD","PARAKEET") }
        val state=PartySparklingState()
        state.select(members,provider)
        fun update() { assertTrue(state.accept(members,members.associateWith { cache.known(it,provider)!! },provider)) }
        update();assertTrue(state.needs("Macaw"))
        cache.observe("friend",provider,setOf("Bluebird","Parakeet","Macaw"))
        update();assertFalse(state.needs("Macaw"))
        cache.caught(members,"Wumpa",provider)
        cache.observe("friend",provider,emptySet())
        assertTrue("WUMPA" in cache.load("friend",provider) { fail("no additional HTTP") })
        assertTrue("MACAW" in cache.known("friend",provider)!!)
        cache.observe("outsider",provider,setOf("MACAW"))
        assertNull(cache.known("outsider",provider))
        cache.observe("friend",ApiProvider.capture("other-key",false),setOf("DOOMSPIRAL"))
        assertFalse("DOOMSPIRAL" in cache.known("friend",provider)!!)
    }

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
            cache.observe("me",provider,setOf("BLUEBIRD"))
        } finally { release.countDown() }
        assertEquals(setOf("MACAW","BLUEBIRD"), task.get(5, TimeUnit.SECONDS))
        assertEquals(setOf("MACAW","BLUEBIRD"), cache.known("me", provider))
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
