package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ShardMarketServiceTest {
    @Test fun `new shared quote reaches all ten cached products before pending history parsing`() {
        val ids=SaleProducts.names.keys.take(10).toSet()
        val cache=MarketHistoryStore(directory.resolve("priority.json"),ids)
        cache.write(ids.associateWith {data},now)
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        val requested=java.util.concurrent.CountDownLatch(1)
        val entered=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        val callback=java.util.concurrent.atomic.AtomicReference<((HistoryReply)->Unit)?>()
        val quote=java.util.concurrent.atomic.AtomicReference(QuoteSnapshot(now,
            ids.associateWith {MarketQuote(it,100.0,90.0,now)},emptyMap()))
        val s=ShardMarketService(ids,{now},worker,HistoryTransport {_,reply ->
            callback.set(reply);requested.countDown();MarketCancel {}
        },{_,_,_->entered.countDown();release.await(5,java.util.concurrent.TimeUnit.SECONDS);data},
            cache,{quote.get()},{},{null},experimental=true)
        try {
            s.updateDemand(true,true,ids,1)
            assertTrue(requested.await(3,java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(s.snapshot(now).values.all {it.state==AdviceState.SELL})
            val next=now+60000
            quote.set(QuoteSnapshot(next,ids.associateWith {MarketQuote(it,98.0,90.0,next)},emptyMap()))
            callback.get()!!(HistoryReply(200,"ok"))
            assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS))
            val current=s.snapshot(next)
            assertEquals(ids,current.keys)
            assertTrue(current.values.all {it.quoteAt==next && it.ask==98.0},
                "Fresh shared prices must publish before slow history parsing or disk writes")
            assertTrue(current.values.all {it.state==AdviceState.SELL},"Service still exposes confirmed advice")
            s.stop(2)
            assertTrue(s.snapshot(next).isEmpty(),"Interim view cannot outlive disabled demand")
        } finally {release.countDown();worker.shutdown();assertTrue(worker.awaitTermination(6,java.util.concurrent.TimeUnit.SECONDS))}
    }
    @Test fun `slow background parsing never blocks cached rendering or client disable`() {
        val worker=java.util.concurrent.Executors.newSingleThreadExecutor()
        val client=java.util.concurrent.Executors.newSingleThreadExecutor()
        val entered=java.util.concurrent.CountDownLatch(1)
        val release=java.util.concurrent.CountDownLatch(1)
        val requested=java.util.concurrent.CountDownLatch(1)
        val callback=java.util.concurrent.atomic.AtomicReference<((HistoryReply)->Unit)?>()
        val s=ShardMarketService(setOf(id),{now},worker,HistoryTransport {_,reply ->
            callback.set(reply);requested.countDown();MarketCancel {}
        },{_,_,_->entered.countDown();release.await(3,java.util.concurrent.TimeUnit.SECONDS);data},
            MarketHistoryStore(directory.resolve("slow.json"),setOf(id)),{null},{},{null})
        var responsive=false
        try {
            s.updateDemand(true,true,setOf(id),1)
            assertTrue(requested.await(2,java.util.concurrent.TimeUnit.SECONDS))
            callback.get()!!(HistoryReply(200,"ok"))
            assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS))
            val read=client.submit<Boolean> {s.snapshot(now);s.updateDemand(false,true,emptySet(),1);s.snapshot(now).isEmpty()}
            responsive=runCatching {read.get(300,java.util.concurrent.TimeUnit.MILLISECONDS)}.getOrDefault(false)
        } finally {release.countDown();worker.shutdown();client.shutdown();worker.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS);client.awaitTermination(3,java.util.concurrent.TimeUnit.SECONDS)}
        assertTrue(responsive,"Background work must not hold a lock required by rendering/client lifecycle")
        assertFalse(java.nio.file.Files.exists(directory.resolve("slow.json")),"Disable during parse must invalidate the pending write")
    }
    @TempDir lateinit var directory: Path
    private val id="SHARD_HIDEONWALL"
    private var now=MarketFixtures.NOW
    private val work=ArrayDeque<Runnable>()
    private data class Call(val job: HistoryJob,val reply: (HistoryReply)->Unit,var cancelled: Boolean=false)
    private val calls=mutableListOf<Call>()
    private var quoteRefreshes=0
    private var parseCalls=0
    private var data=MarketFixtures.samples()
    private var certificate: EligibilityReport?=null
    private var serverAt=now
    private var serverAsk=100.0
    private var elapsedOverride: Long?=null
    private fun service(ids: Set<String> = setOf(id),experimental: Boolean=false): ShardMarketService {
        val transport=HistoryTransport { job,reply ->
            val call=Call(job,reply)
            calls.add(call)
            MarketCancel {call.cancelled=true}
        }
        return ShardMarketService(ids,{now},Executor {work.add(it)},transport,
            { job,body,at -> parseCalls++; require(body=="ok"); if(job.part==HistoryPart.RECENT)data else ShardHistoryParsing.normalize(data,at).week },
            MarketHistoryStore(directory.resolve("prices.json"),ids),
            {QuoteSnapshot(serverAt,mapOf(id to MarketQuote(id,serverAsk,90.0,serverAt)),emptyMap())},
            {quoteRefreshes++},{certificate},elapsedClock={elapsedOverride ?: now},experimental=experimental)
    }
    private fun drain() {var count=0;while(work.isNotEmpty()){check(count++<100);work.removeFirst().run()}}
    private fun tick(service: ShardMarketService, products: Set<String> = setOf(id), generation: Long=1) {
        service.updateDemand(true,true,products,generation);drain()
    }
    private fun reply(index: Int,status: Int=200,body: String="ok",retryAfter: Long?=null) {
        calls[index].reply(HistoryReply(status,body,retryAfter));drain()
    }
    @Test fun `actual model pending confirmation preserves economic decline for grouped offer warning`() {
        val s=service(experimental=true);tick(s);reply(0)
        val watches=SellOfferWatch().apply {selectScope(MarketScope("1".repeat(32),"2".repeat(32)));observe(listOf(OfferEvidence(id,100.0,10,false)),now)}
        assertTrue(watches.update(s.snapshot(now),now).isEmpty())
        now+=60000;serverAt=now;serverAsk=98.0;tick(s)
        assertEquals(AdviceState.SELL,s.snapshot(now).getValue(id).state)
        assertTrue(watches.update(s.snapshot(now),now).isEmpty())
        repeat(3) {tick(s);assertTrue(watches.update(s.snapshot(now),now).isEmpty())}
        now+=60000;serverAt=now;tick(s)
        assertEquals(AdviceState.HOLD,s.snapshot(now).getValue(id).state)
        now+=1000;serverAt=now;tick(s) // Client may read the next HOLD snapshot rather than the transition frame.
        assertEquals(1,watches.update(s.snapshot(now),now).size)
        assertTrue(watches.update(s.snapshot(now),now).isEmpty())
    }
    @Test fun `experimental mode publishes references honestly without a forecast certificate`() {
        data=data.map {it.copy(basis=PriceBasis.UNVERIFIED_REFERENCE,availableAt=null)}
        val s=service(experimental=true);tick(s);reply(0)
        val advice=s.snapshot(now).getValue(id)
        assertEquals(AdviceState.SELL,advice.state)
        assertEquals("EXPERIMENTAL_RANGE",advice.reason)
        assertEquals(ExperimentalRange.KEY,advice.modelKey)
        assertNull(certificate)
    }

    @Test fun `ticks coalesce and history requests stay singular deduplicated and rate limited`() {
        val s=service()
        repeat(100){s.updateDemand(true,true,setOf(id,"UNRELATED"),1)}
        assertEquals(1,work.size)
        drain();assertEquals(1,calls.size)
        repeat(5){tick(s)}
        assertEquals(1,calls.size)
        reply(0)
        tick(s);assertEquals(1,calls.size)
        now+=1000;tick(s)
        assertEquals(2,calls.size)
        assertEquals(HistoryPart.WEEK,calls[1].job.part)
    }
    @Test fun `disable cancels transport and late callbacks cannot publish or write`() {
        val s=service();tick(s)
        s.updateDemand(false,true,setOf(id),1);drain()
        assertTrue(calls[0].cancelled)
        reply(0)
        assertFalse(Files.exists(directory.resolve("prices.json")))
        assertTrue(s.snapshot(now).isEmpty())
    }
    @Test fun `reconnect generation ignores old callback and stale stop requests`() {
        val s=service();tick(s)
        now+=1000
        tick(s,generation=2)
        assertEquals(2,calls.size)
        reply(0)
        assertFalse(Files.exists(directory.resolve("prices.json")))
        s.stop(1)
        reply(1)
        assertTrue(Files.exists(directory.resolve("prices.json")))
        assertEquals(setOf(id),s.snapshot(now).keys)
    }
    @Test fun `oversized and malformed responses preserve old cache without publication`() {
        val s=service();tick(s)
        reply(0,body="x".repeat(2*1024*1024+1))
        assertEquals(0,parseCalls)
        assertFalse(Files.exists(directory.resolve("prices.json")))
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
        now+=1000;tick(s)
        reply(calls.lastIndex,body="é".repeat(1_050_000))
        assertEquals(0,parseCalls,"UTF8 size cap must run before parser invocation")
    }
    @Test fun `retry after is respected and normal refresh intervals are distinct`() {
        val s=service();tick(s)
        reply(0,429,retryAfter=60_000)
        now+=59_999;tick(s);assertEquals(1,calls.size,"Whole provider respects Retry-After")
        now++;tick(s);assertEquals(HistoryPart.WEEK,calls.last().job.part)
        reply(1)
        now+=999;tick(s);assertEquals(2,calls.size)
        now++;tick(s);assertEquals(HistoryPart.RECENT,calls.last().job.part)
        reply(2)
        now+=599_999;tick(s);assertEquals(3,calls.size)
        now++;tick(s);assertEquals(4,calls.size)
        assertEquals(HistoryPart.RECENT,calls.last().job.part)
    }
    @Test fun `provider cooldown spans products demand generations and wall clock jumps using elapsed time`() {
        val other="SHARD_BLUEBIRD";val start=now;elapsedOverride=start
        val s=service(setOf(id,other));tick(s,setOf(id,other));reply(0,429,retryAfter=60000)
        elapsedOverride=start+1000;now+=1000;tick(s,setOf(id,other));assertEquals(1,calls.size)
        now+=7*86400000L;elapsedOverride=start+59999
        tick(s,setOf(other),generation=2);assertEquals(1,calls.size,"New demand and a forward wall-clock jump cannot bypass provider wait")
        elapsedOverride=start+60000;tick(s,setOf(other),generation=2)
        assertEquals(2,calls.size);assertEquals(other,calls.last().job.productId)
    }
    @Test fun `late throttled response still protects the provider without parsing or publishing retired work`() {
        val s=service();tick(s);s.updateDemand(false,false,emptySet(),1);drain()
        now+=1000;reply(0,429,retryAfter=60000)
        val parsed=parseCalls
        now+=1000;tick(s,generation=2);assertEquals(1,calls.size)
        assertEquals(parsed,parseCalls);assertFalse(Files.exists(directory.resolve("prices.json")))
        now+=59000;tick(s,generation=2);assertEquals(2,calls.size)
    }
    @Test fun `no certificate or unverified history remains unknown despite fresh quotes`() {
        val s=service();tick(s);reply(0)
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        certificate=ShardReplay.evaluate(points,now-3*MarketFixtures.DAY)
        assertTrue(certificate!!.eligible)
        now+=1000;tick(s);reply(calls.lastIndex) // Complete the initial weekly request.
        data=data.map {it.copy(basis=PriceBasis.UNVERIFIED_REFERENCE)}
        now=MarketFixtures.NOW+600_000;serverAt=now;tick(s);reply(calls.lastIndex)
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
        assertEquals("UNVALIDATED_MODEL",s.snapshot(now).getValue(id).reason)
    }
    @Test fun `valid synthetic evidence enables advice but read time staleness removes it without networking`() {
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        certificate=ShardReplay.evaluate(points,now-3*MarketFixtures.DAY)
        assertTrue(certificate!!.eligible)
        val s=service();tick(s);reply(0)
        assertEquals(AdviceState.SELL,s.snapshot(now).getValue(id).state)
        val refreshes=quoteRefreshes
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now+180_001).getValue(id).state)
        assertEquals(refreshes,quoteRefreshes)
        s.stop(2);drain()
        assertTrue(s.snapshot(now).isEmpty())
    }
    @Test fun `invalid delivery metadata cannot enable advice even with a passing report`() {
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        certificate=ShardReplay.evaluate(points,now-3*MarketFixtures.DAY)
        data=data.map {it.copy(observedAt=-1)}
        val s=service();tick(s);reply(0)
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
    }
    @Test fun `new calibration parameters replace cached targets and reset the state baseline`() {
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        val report=ShardReplay.evaluate(points,now-3*MarketFixtures.DAY)
        certificate=report.copy(parameters=ModelParameters(.1,1.0,0.0,0.0))
        data=MarketFixtures.samples(price={at -> if(at>now-90*60_000)200.0 else 100.0})
        val s=service();tick(s);reply(0)
        assertEquals(100.0,s.snapshot(now).getValue(id).target)
        certificate=report.copy(parameters=ModelParameters(.9,1.0,0.0,0.0))
        now+=1000;serverAt=now;tick(s)
        assertEquals(200.0,s.snapshot(now).getValue(id).target)
        assertEquals(AdviceState.HOLD,s.snapshot(now).getValue(id).state)
    }
    @Test fun `thirty seven products produce at most seventy four distinct queued refreshes`() {
        data=data.takeLast(24)
        val ids=(0 until 37).map {"SHARD_TEST_${('A'.code+it/26).toChar()}${('A'.code+it%26).toChar()}"}.toSet()
        val s=service(ids)
        tick(s,ids+"UNRELATED")
        repeat(74) { index -> reply(index);now+=1000;tick(s,ids) }
        assertEquals(74,calls.size)
        assertEquals(74,calls.map {it.job}.distinct().size)
        assertTrue(calls.all {it.job.productId in ids})
    }
    @Test fun `failure retry delay grows from fifteen seconds to the fifteen minute ceiling`() {
        val s=service();tick(s);reply(0,500)
        now+=1000;tick(s);reply(1)
        var lastRecent=MarketFixtures.NOW
        for(delay in listOf(15_000L,30_000,60_000,120_000,240_000,480_000,900_000)) {
            val count=calls.size
            now=lastRecent+delay-1;tick(s);assertEquals(count,calls.size)
            now++;tick(s);assertEquals(count+1,calls.size)
            assertEquals(HistoryPart.RECENT,calls.last().job.part)
            lastRecent=now;reply(calls.lastIndex,500)
        }
    }
    @Test fun `queued work disabled before execution cannot start transport or read a price file`() {
        val s=service()
        s.updateDemand(true,true,setOf(id),1)
        s.stop(2);drain()
        assertTrue(calls.isEmpty())
        assertFalse(Files.exists(directory.resolve("prices.json")))
    }
    @Test fun `parser failure does not persist or promote partial history`() {
        val s=service();tick(s);reply(0,body="truncated")
        assertEquals(1,parseCalls)
        assertFalse(Files.exists(directory.resolve("prices.json")))
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
    }
    @Test fun `weekly refresh does not repeat before its hour while recent refresh remains independent`() {
        val s=service();tick(s);reply(0)
        now+=1000;tick(s);reply(1)
        now=MarketFixtures.NOW+3_600_999;tick(s)
        assertEquals(HistoryPart.RECENT,calls.last().job.part)
        assertEquals(1,calls.count {it.job.part==HistoryPart.WEEK})
        reply(calls.lastIndex)
        now+=1000;tick(s)
        assertEquals(HistoryPart.WEEK,calls.last().job.part)
    }
    @Test fun `verified price labels without known delivery remain unavailable`() {
        val points=MarketFixtures.samples(14,price={at -> if((at/MarketFixtures.HOUR)%3==0L)99.0 else 101.0})
        certificate=ShardReplay.evaluate(points,now-3*MarketFixtures.DAY)
        data=data.map {it.copy(availableAt=null)}
        val s=service();tick(s);reply(0)
        assertEquals(AdviceState.UNKNOWN,s.snapshot(now).getValue(id).state)
        assertEquals("UNVALIDATED_MODEL",s.snapshot(now).getValue(id).reason)
    }
}
