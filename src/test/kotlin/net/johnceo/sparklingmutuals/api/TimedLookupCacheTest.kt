package net.johnceo.sparklingmutuals.api

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TimedLookupCacheTest {
    @Test fun `successful lookups last an hour but failures retry`() {
        var now=0L; var calls=0
        val cache=TimedLookupCache<String,Int>(clock={now})
        fun read()=cache.load("player") { ++calls }
        assertEquals(1,read()); now=3_599_999; assertEquals(1,read())
        now=3_600_000; assertEquals(2,read())
        assertThrows(IllegalStateException::class.java) { cache.load("other") { error("failed") } }
        assertEquals(3,cache.load("other") { ++calls })
        cache.clear();assertEquals(4,read())
    }
    @Test fun `simultaneous reads share one lookup without holding the lock`() {
        val cache=TimedLookupCache<String,Int>();val entered=CountDownLatch(1);val release=CountDownLatch(1)
        val first=CompletableFuture.supplyAsync { cache.load("a") { entered.countDown();release.await();7 } }
        entered.await()
        val second=CompletableFuture.supplyAsync { cache.load("a") { error("duplicate") } }
        cache.put("b",9);release.countDown()
        assertEquals(7,first.join());assertEquals(7,second.join());assertEquals(9,cache.peek("b"))
    }
}
