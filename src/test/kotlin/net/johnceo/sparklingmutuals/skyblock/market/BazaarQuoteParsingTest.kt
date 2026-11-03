package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.ByteBuffer
import java.util.concurrent.Flow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BazaarQuoteParsingTest {
    private val now = 1_800_000_000_000L
    private fun book(asks: String = "120,100,110", bids: String = "80,90,85") =
        """{"buy_summary":[${asks.split(',').joinToString { """{"amount":2,"orders":1,"pricePerUnit":$it}""" }}],"sell_summary":[${bids.split(',').joinToString { """{"amount":3,"orders":1,"pricePerUnit":$it}""" }}],"quick_status":{"sellPrice":87.5,"buyPrice":115.5}}"""
    private fun payload(product: String = book(), at: Long = now, success: Boolean = true) =
        """{"success":$success,"lastUpdated":$at,"products":{"SHARD_BLUEBIRD":$product}}"""

    @Test fun `competing ask and bid come from book extrema rather than weighted quick status`() {
        val snapshot = BazaarQuoteParsing.parse(payload(), now)!!
        val quote = snapshot.quotes.getValue("SHARD_BLUEBIRD")
        assertEquals(100.0, quote.ask)
        assertEquals(90.0, quote.bid)
        assertEquals(now, quote.serverAt)
        assertArrayEquals(doubleArrayOf(87.5,115.5), snapshot.quickPrices.getValue("SHARD_BLUEBIRD"))
    }

    @Test fun `invalid orders cannot become best prices and an incoherent product remains absent`() {
        val snapshot = BazaarQuoteParsing.parse(payload(book("0,-1,1e999,120", "0,-1,90")), now)!!
        assertEquals(120.0, snapshot.quotes.getValue("SHARD_BLUEBIRD").ask)
        assertTrue(BazaarQuoteParsing.parse(payload(book("80", "90")), now)!!.quotes.isEmpty())
        assertTrue(BazaarQuoteParsing.parse(payload(book("0,-1", "90")), now)!!.quotes.isEmpty())
    }

    @Test fun `malformed product does not remove a valid sibling or fabricate its own quote`() {
        val raw = """{"success":true,"lastUpdated":$now,"products":{"BAD":{},"SHARD_BLUEBIRD":${book()},"WRONG":null}}"""
        assertEquals(setOf("SHARD_BLUEBIRD"), BazaarQuoteParsing.parse(raw, now)!!.quotes.keys)
    }

    @Test fun `bad envelopes and implausible timestamps do not create fresh snapshots`() {
        for (raw in listOf("not json", "{}", payload(success=false), payload(at=now+60_001), payload(at=0)))
            assertNull(BazaarQuoteParsing.parse(raw, now),raw)
        val quote = BazaarQuoteParsing.parse(payload(at=now-180_001),now)!!.quotes.getValue("SHARD_BLUEBIRD")
        assertFalse(quote.freshAt(now))
        assertTrue(quote.freshAt(now-1))
    }

    @Test fun `zero amount and missing order side are not available offers`() {
        val raw = book().replace("\"amount\":2", "\"amount\":0")
        assertTrue(BazaarQuoteParsing.parse(payload(raw), now)!!.quotes.isEmpty())
        assertTrue(BazaarQuoteParsing.parse(payload("""{"quick_status":{"sellPrice":90,"buyPrice":100}}"""),now)!!.quotes.isEmpty())
    }

    @Test fun `bounded response cancels before buffering an oversized chunk`() {
        val subscriber = LimitedStringSubscriber(4)
        var cancelled = false
        subscriber.onSubscribe(object : Flow.Subscription {
            override fun request(n: Long) = Unit
            override fun cancel() { cancelled = true }
        })
        subscriber.onNext(listOf(ByteBuffer.wrap("abc".toByteArray())))
        subscriber.onNext(listOf(ByteBuffer.wrap("de".toByteArray())))
        assertTrue(cancelled)
        assertTrue(subscriber.body.toCompletableFuture().isCompletedExceptionally)
    }

    @Test fun `bounded response preserves UTF8 characters split across network buffers`() {
        val subscriber = LimitedStringSubscriber(4)
        subscriber.onSubscribe(object : Flow.Subscription { override fun request(n: Long)=Unit; override fun cancel()=Unit })
        val bytes = "£a".toByteArray(Charsets.UTF_8)
        subscriber.onNext(listOf(ByteBuffer.wrap(bytes.copyOfRange(0,1))))
        subscriber.onNext(listOf(ByteBuffer.wrap(bytes.copyOfRange(1,bytes.size))))
        subscriber.onComplete()
        assertEquals("£a", subscriber.body.toCompletableFuture().join())
    }
}
