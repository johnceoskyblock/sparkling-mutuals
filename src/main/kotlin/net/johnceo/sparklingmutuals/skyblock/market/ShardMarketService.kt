package net.johnceo.sparklingmutuals.skyblock.market

import java.util.Collections
import java.util.concurrent.Executor

internal enum class HistoryPart(val refreshMillis: Long) { RECENT(600_000), WEEK(3_600_000) }
internal data class HistoryJob(val productId: String,val part: HistoryPart)
internal data class HistoryReply(val status: Int,val body: String,val retryAfterMillis: Long?=null)
internal fun interface MarketCancel { fun cancel() }
/** Implementations must also cap streamed HTTP bodies before buffering; never call from rendering. */
internal fun interface HistoryTransport { fun request(job: HistoryJob,reply: (HistoryReply)->Unit): MarketCancel }

/** Client demand ticks coalesce onto one supplied worker. No owned thread or periodic collector. */
internal class ShardMarketService(
    allowedIds: Set<String>,
    private val clock: ()->Long,
    private val executor: Executor,
    private val transport: HistoryTransport,
    private val parse: (HistoryJob,String,Long)->List<PriceSample>,
    private val store: MarketHistoryStore,
    private val quotes: ()->QuoteSnapshot?,
    private val refreshQuotes: ()->Unit,
    private val eligibility: (String)->EligibilityReport?,
    private val elapsedClock: ()->Long={System.nanoTime()/1_000_000},
) {
    private val allowed=allowedIds.toSet().also {require(it.size<=37)}
    private var generation=-1L
    private var epoch=0L
    private var enabled=false
    private var wanted=emptySet<String>()
    private var loaded=false
    private var workQueued=false
    private var nextLaunch=Long.MIN_VALUE
    private val queue=linkedSetOf<HistoryJob>()
    private val due=mutableMapOf<HistoryJob,Long>()
    private val failures=mutableMapOf<HistoryJob,Int>()
    private var history=emptyMap<String,List<PriceSample>>()
    private var published=emptyMap<String,Recommendation>()
    private val latches=mutableMapOf<String,AdviceLatch>()
    private var targetAt=Long.MIN_VALUE
    private val targets=mutableMapOf<String,TargetResult>()
    private val latestHistory=mutableMapOf<String,Long>()
    private data class Calibration(val fingerprint: String,val parameters: ModelParameters,val model: Int,val policy: Int)
    private val calibrations=mutableMapOf<String,Calibration>()
    private val modelKeys=mutableMapOf<String,String>()
    private class Active(val job: HistoryJob,val epoch: Long,var cancel: MarketCancel?=null)
    private var active: Active?=null
    private var pending: Pair<Active,HistoryReply>?=null

    @Synchronized fun updateDemand(enabled: Boolean,inSkyblock: Boolean,products: Set<String>,generation: Long) {
        if(generation<this.generation)return
        val selection=products.intersect(allowed)
        val on=enabled && inSkyblock && selection.isNotEmpty()
        if(generation!=this.generation || !on) {
            invalidate()
            this.generation=generation
        }
        this.enabled=on
        wanted=if(on)selection.toSet()else emptySet()
        queue.removeAll {it.productId !in wanted}
        due.keys.removeAll {it.productId !in wanted}
        failures.keys.removeAll {it.productId !in wanted}
        latches.keys.retainAll(wanted)
        if(active?.job?.productId?.let {it !in wanted}==true) {
            active?.cancel?.cancel();active=null;pending=null
        }
        if(on)requestWork()
    }

    @Synchronized fun stop(generation: Long) {
        if(generation<this.generation)return
        this.generation=generation
        invalidate();enabled=false;wanted=emptySet()
    }

    private fun invalidate() {
        epoch++
        active?.cancel?.cancel();active=null;pending=null
        queue.clear();due.clear();failures.clear();latches.clear();targets.clear();latestHistory.clear();calibrations.clear();modelKeys.clear()
        published=emptyMap();history=emptyMap();loaded=false;targetAt=Long.MIN_VALUE
    }

    private fun requestWork() {
        if(workQueued)return
        workQueued=true
        try {executor.execute { synchronized(this) {workQueued=false;process()} }}
        catch(_: RuntimeException) {workQueued=false}
    }

    private fun process() {
        if(!enabled)return
        val now=clock()
        if(!loaded) {history=store.read(now);loaded=true}
        val response=pending
        if(response!=null) {
            pending=null
            val (owner,reply)=response
            if(active===owner && owner.epoch==epoch && owner.job.productId in wanted) {
                accept(owner.job,reply,now)
                active=null
            }
        }
        refreshQuotes() // The shared quote transport owns its one-minute GET throttle.
        publish(now)
        wanted.forEach {id -> HistoryPart.entries.forEach {part ->
            val job=HistoryJob(id,part)
            if(active?.job!=job)queue.add(job)
        }}
        if(active!=null || elapsedClock()<nextLaunch)return
        val job=queue.firstOrNull {(due[it] ?: Long.MIN_VALUE)<=now} ?: return
        queue.remove(job)
        nextLaunch=elapsedClock()+1000
        val owner=Active(job,epoch)
        active=owner
        try {
            val cancel=transport.request(job) {reply -> synchronized(this) {
                if(enabled && active===owner && owner.epoch==epoch && job.productId in wanted && pending==null) {
                    pending=owner to reply;requestWork()
                }
            }}
            if(active===owner)owner.cancel=cancel else cancel.cancel()
        } catch(_: Exception) {
            if(active===owner) {pending=owner to HistoryReply(0,"");requestWork()}
        }
    }

    private fun accept(job: HistoryJob,reply: HistoryReply,now: Long) {
        val samples=if(reply.status==200 && reply.body.length<=2*1024*1024 &&
            reply.body.toByteArray(Charsets.UTF_8).size<=2*1024*1024)
            runCatching {parse(job,reply.body,now)}.getOrDefault(emptyList()) else emptyList()
        val expected=if(job.part==HistoryPart.RECENT)ShardHistoryParsing.STEP else ShardHistoryParsing.COARSE
        val accepted=samples.filter {it.resolutionMillis==expected}
        if(accepted.isEmpty()) {
            val count=(failures[job] ?: 0)+1
            val backoff=minOf(900_000L,15_000L shl minOf(count-1,6))
            val delay=maxOf(backoff,reply.retryAfterMillis?.coerceAtLeast(0) ?: 0)
            due[job]=if(delay>Long.MAX_VALUE-now)Long.MAX_VALUE else now+delay
            if(count>=7) {failures.remove(job);due[job]=maxOf(due.getValue(job),now+job.part.refreshMillis)}
            else failures[job]=count
            return
        }
        val safe=store.sanitize(history+(job.productId to (history[job.productId].orEmpty()+accepted)),now)
        val merged=ShardHistoryParsing.normalize(safe[job.productId].orEmpty(),now)
        history=store.sanitize(safe+(job.productId to (merged.fine+merged.week)),now)
        store.write(history,now)
        failures.remove(job)
        due[job]=now+job.part.refreshMillis
        targetAt=Long.MIN_VALUE
    }

    private fun publish(now: Long) {
        val recalculate=targetAt==Long.MIN_VALUE || now<targetAt || now-targetAt>=300_000
        val current=quotes()
        val result=linkedMapOf<String,Recommendation>()
        wanted.forEach {id ->
            val report=eligibility(id)
            val calibration=report?.let {Calibration(it.fingerprint,it.parameters,it.modelVersion,it.policyVersion)}
            if(calibration!=calibrations[id]) {
                latches.remove(id);targets.remove(id)
                if(calibration==null) {calibrations.remove(id);modelKeys.remove(id)}
                else {calibrations[id]=calibration;modelKeys[id]="$generation:$calibration"}
            }
            val normalized=ShardHistoryParsing.normalize(history[id].orEmpty(),now,requireAvailability=true)
            val evidence=normalized.fine+normalized.week
            val qualified=report?.eligible==true && report.modelVersion==ShardTargetModel.VERSION &&
                report.policyVersion==ShardReplay.POLICY_VERSION && report.verifiedEvidence && report.usesKnownAvailability &&
                evidence.isNotEmpty() && evidence.all {it.basis==PriceBasis.VERIFIED_BEST_ASK && it.availableAt!=null}
            if(recalculate || id !in targets) {
                targets[id]=ShardTargetModel.target(normalized,now,report?.parameters ?: ModelParameters())
                latestHistory[id]=normalized.fine.lastOrNull()?.at ?: 0L
            }
            val last=latches[id]
            val quote=current?.quotes?.get(id)?.takeIf {last==null || it.serverAt>=last.serverAt}
            val target=targets.getValue(id)
            val advice=ShardTargetModel.observe(last,quote,target,now,qualified)
            latches[id]=advice
            result[id]=Recommendation(id,advice.state,advice.target,quote?.serverAt ?: 0L,
                advice.unknown?.name ?: if(target.falling)"FALLING_MARKET_EXIT" else "HISTORICAL_TARGET",quote?.ask,modelKeys[id])
        }
        if(recalculate)targetAt=now
        published=Collections.unmodifiableMap(result)
    }

    /** Cached read only. Freshness can remove a color without triggering network, parsing or disk work. */
    @Synchronized fun snapshot(now: Long): Map<String,Recommendation> {
        if(!enabled)return emptyMap()
        return Collections.unmodifiableMap(published.mapValues { (id,advice) ->
            val staleQuote=advice.quoteAt<=0 || now<advice.quoteAt-60_000 || now-advice.quoteAt>180_000
            val staleHistory=(latestHistory[id] ?: 0L).let {it<=0 || now-it>900_000}
            if(advice.state!=AdviceState.UNKNOWN && (staleQuote || staleHistory))
                advice.copy(state=AdviceState.UNKNOWN,target=null,reason=if(staleQuote)"STALE_QUOTE" else "STALE_HISTORY")
            else advice
        })
    }
}
