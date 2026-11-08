package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ThreadLocalRandom

/** Deliberately independent of sockets: only usable room state resets the failure window. */
internal class PartySyncRoutePolicy(val nonce: String) {
    var provider="primary"; private set
    var generation: String?=null; private set
    private var unavailableSince: Long?=null
    fun observe(usable: Boolean,now: Long) { if(usable)unavailableSince=null else if(unavailableSince==null)unavailableSince=now }
    fun unusableMillis(now: Long)=(unavailableSince?.let { (now-it).coerceIn(0,86_400_000) } ?: 0)
    fun accept(generation: String,provider: String,nonce: String,admitted: Boolean): Boolean {
        if(!admitted || nonce!=this.nonce || provider !in setOf("primary","backup") ||
            generation.isBlank() || generation.length>64 || this.generation?.let { it!=generation }==true ||
            this.provider=="backup" && provider!="backup")return false
        this.generation=generation;this.provider=provider;return true
    }
}

/** Only route identity/provider status is sent while primary works; callbacks never touch game state. */
internal class PartySyncRouting(private val origin: URI?=configuredOrigin()) {
    companion object {
        // Set to the verified workers.dev origin only after the authenticated deployed smoke test.
        private const val DEPLOYED_ORIGIN=""
        fun configuredOrigin(): URI? = (System.getProperty("sparklingmutuals.sync.backupOrigin") ?: DEPLOYED_ORIGIN)
            .takeIf { it.isNotBlank() }?.let { runCatching { URI.create(it) }.getOrNull() }
            ?.takeIf { it.scheme=="https" && it.host!=null && it.userInfo==null && it.rawQuery==null && it.fragment==null && it.path.orEmpty() in setOf("","/") }
    }
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private data class Result(val revision: Long,val context: PartySyncTransport.Context,val body: JsonObject?,val retry: Long)
    private val results=ArrayBlockingQueue<Result>(2)
    @Volatile private var revision=0L
    private var context: PartySyncTransport.Context?=null
    var policy=PartySyncRoutePolicy(UUID.randomUUID().toString()); private set
    private var nextAt=0L
    @Volatile private var busy=false
    @Volatile private var token=""
    @Volatile private var tokenUntil=0L
    @Volatile private var tokenOwner=""
    val provider get()=policy.provider
    val generation get()=policy.generation
    val nonce get()=policy.nonce
    val backupOrigin get()=origin
    fun tick(next: PartySyncTransport.Context,now: Long,usable: Boolean,eligible: Boolean): Boolean {
        policy.observe(usable,now)
        if(context!=next){revision++;context=next;busy=false;results.clear();nextAt=0}
        var changed=false
        repeat(2) {
            val result=results.poll() ?: return@repeat
            if(result.revision!=revision || result.context!=context)return@repeat
            busy=false;nextAt=maxOf(nextAt,now+result.retry)
            val o=result.body ?: return@repeat
            if(PartySyncProtocol.integer(o.get("version"),1,1)==null)return@repeat
            val gen=PartySyncProtocol.text(o.get("generation"),64) ?: return@repeat
            val provider=PartySyncProtocol.text(o.get("provider")) ?: return@repeat
            val nonce=PartySyncProtocol.text(o.get("nonce"),64) ?: return@repeat
            val admitted=PartySyncProtocol.bool(o.get("admitted")) ?: return@repeat
            val before=policy.provider
            if(policy.accept(gen,provider,nonce,admitted))changed=before!=policy.provider
        }
        if(origin!=null && eligible && next.roster.size>1 && !busy && now>=nextAt){
            nextAt=now+30_000+ThreadLocalRandom.current().nextLong(0,3001);busy=true
            val attempt=revision
            request(next,"check",policy.unusableMillis(now)).whenComplete { response,error ->
                val retry=(error as? CompletionException)?.cause as? Retry
                if(attempt==revision)results.offer(Result(attempt,next,response, retry?.delay ?: 0))
            }
        }
        return changed
    }
    private class Retry(val delay: Long): RuntimeException()
    private fun request(next: PartySyncTransport.Context,action: String,unusable: Long): CompletableFuture<JsonObject> {
        val origin=origin ?: return CompletableFuture.failedFuture(IllegalStateException())
        val body=JsonObject().apply {
            addProperty("version",1);addProperty("lobby",next.lobby);addProperty("self",next.self)
            add("roster",JsonArray().apply {next.roster.forEach(::add)});addProperty("nonce",policy.nonce)
            addProperty("unusableMs",unusable);addProperty("action",action)
        }.toString()
        val auth=if(tokenOwner==next.self && token.isNotBlank() && System.currentTimeMillis()<tokenUntil-120_000)
            CompletableFuture.completedFuture(token)
        else http.sendAsync(HttpRequest.newBuilder(origin.resolve("/v1/auth/uuid/session")).timeout(Duration.ofSeconds(10))
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"uuid\":\"${next.self}\"}")).build(),HttpResponse.BodyHandlers.ofString())
            .thenApply { r ->
                if(r.statusCode()!=200)throw Retry(retry(r))
                require(r.body().length<=8192)
                val o=JsonParser.parseString(r.body()).asJsonObject
                val t=PartySyncProtocol.text(o.get("token"),4096)?.takeIf { it.isNotBlank()&&'\r' !in it&&'\n' !in it } ?: error("Invalid session")
                val seconds=PartySyncProtocol.integer(o.get("expiresInSeconds"),120,86400) ?: error("Invalid session")
                token=t;tokenOwner=next.self;tokenUntil=System.currentTimeMillis()+seconds*1000;t
            }
        return auth.thenCompose { bearer ->http.sendAsync(HttpRequest.newBuilder(origin.resolve("/v1/route")).timeout(Duration.ofSeconds(10))
            .header("Content-Type","application/json").header("Authorization","Bearer $bearer").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString()) }
            .thenApply { r ->
                if(r.statusCode()==401){token="";tokenUntil=0}
                if(r.statusCode()!=200)throw Retry(retry(r))
                require(r.body().length<=2048);JsonParser.parseString(r.body()).asJsonObject
            }
    }
    private fun retry(response: HttpResponse<*>): Long = maxOf(30_000,response.headers().firstValue("Retry-After").orElse("").toLongOrNull()?.coerceIn(0,3600)?.times(1000) ?: 0)
    fun reset() {
        context?.let { if(origin!=null)request(it,"leave",0).exceptionally { null } }
        revision++;context=null;results.clear();busy=false;nextAt=0
        policy=PartySyncRoutePolicy(UUID.randomUUID().toString())
    }
}
