package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.*
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.*
import org.slf4j.LoggerFactory

/** One asynchronous socket. Network callbacks enqueue bounded data; only tick mutates game state. */
internal class PartySyncTransport(
    private var authEndpoint: URI=URI.create("https://api-dev.serkosea.dev/v1/auth/uuid/session"),
    private var syncEndpoint: URI=URI.create("wss://api-dev.serkosea.dev/v1/sync")
) {
    private val primaryAuth=authEndpoint
    private val primarySync=syncEndpoint
    private var completionOnly=false
    var backup=false; private set
    private var routingGeneration: String?=null
    private var visitNonce: String?=null
    @Synchronized fun useBackup(origin: URI,generation: String,nonce: String) {
        require(origin.scheme=="https" && origin.host!=null && generation.isNotBlank() && nonce.isNotBlank())
        close(false);token="";tokenOwner="";tokenUntil=0;retryAt=0;failures=0
        authEndpoint=origin.resolve("/v1/auth/uuid/session")
        syncEndpoint=URI("wss",null,origin.host,origin.port,"/v1/sync",null,null)
        routingGeneration=generation;visitNonce=nonce;backup=true
    }
    /** Independent extension channel; it cannot select main provider or authorize entry. */
    @Synchronized fun useCompletion(origin: URI, generation: String, nonce: String) {
        if(completionOnly && routingGeneration==generation && visitNonce==nonce && authEndpoint==origin.resolve("/v1/auth/uuid/session")) return
        close(false); token=""; tokenOwner=""; tokenUntil=0; retryAt=0; failures=0
        authEndpoint=origin.resolve("/v1/auth/uuid/session")
        syncEndpoint=URI("wss",null,origin.host,origin.port,"/v1/completion-sync",null,null)
        routingGeneration=generation;visitNonce=nonce;completionOnly=true;backup=false
    }
    @Synchronized fun usePrimary() {
        if(!backup)return
        close(false);token="";tokenOwner="";tokenUntil=0;retryAt=0;failures=0
        authEndpoint=primaryAuth;syncEndpoint=primarySync;routingGeneration=null;visitNonce=null;backup=false
    }
    data class Context(val lobby: String,val self: String,val roster: List<String>)
    private data class Delivery(val generation: Long,val text: String)
    private val logger=LoggerFactory.getLogger("sparkling-mutuals/party-sync")
    private val http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val incoming=ArrayBlockingQueue<Delivery>(128)
    @Volatile private var generation=0L
    @Volatile private var socket: WebSocket?=null
    @Volatile private var connecting=false
    @Volatile private var retryAt=0L
    private var context: Context?=null
    @Volatile var ready=false; private set
    var hotspotCapability=false; private set
    var biomeCapability=false; private set
    @Volatile var roomSparklings=false; private set
    private var mapping=emptyMap<String,String>()
    private var failures=0
    private var openedAt=0L
    private var pingAt=0L
    @Volatile private var lastInboundAt=0L
    private var token=""
    private var tokenOwner=""
    private var tokenUntil=0L
    private var tail: CompletableFuture<*> = CompletableFuture.completedFuture(null)
    @Volatile private var queued=0
    private val sent=ArrayDeque<Long>()
    var onReady: () -> Unit={}
    var onMessage: (JsonObject,Map<String,String>) -> Unit={ _,_ -> }
    var onUnavailable: () -> Unit={}
    fun developerStatus(): String = when {
        ready -> if(backup) "Backup connected" else "Primary connected"
        socket != null -> "Awaiting welcome"
        connecting -> "Connecting"
        retryAt > System.currentTimeMillis() -> "Retry backoff"
        else -> "Idle"
    }

    fun developerSnapshot() = mapOf(
        "status" to developerStatus(), "generation" to generation, "failures" to failures,
        "retryAtMillis" to retryAt, "incomingQueue" to incoming.size, "outgoingQueue" to queued,
        "hotspotCapability" to hotspotCapability, "biomeCapability" to biomeCapability, "roomSparklings" to roomSparklings,
        "mappedPlayers" to mapping.toMap(), "lastInboundAtMillis" to lastInboundAt
    )
    fun tick(next: Context,now: Long) {
        if(!next.lobby.matches(Regex("[A-Za-z0-9_-]{1,64}")) || PartySyncProtocol.compact(next.self)==null ||
            next.roster.size !in 1..4 || next.roster.toSet().size!=next.roster.size || next.self !in next.roster ||
            next.roster.any { PartySyncProtocol.compact(it)==null }) { close(false);return }
        if(context!=next) { close(false); context=next; retryAt=0; failures=0 }
        drainIncoming()
        val current=socket
        if(current!=null) {
            if(!ready && now-openedAt>12_000) { fail(generation); return }
            // A successful ping write alone does not prove the room is still reachable.
            if(ready && now-lastInboundAt>75_000) { fail(generation); return }
            if(now>=pingAt) {
                pingAt=now+25_000
                val attempt=generation
                enqueue { current.sendPing(ByteBuffer.wrap(byteArrayOf(83,77))) }.exceptionally { fail(attempt); null }
            }
            return
        }
        if(connecting || now<retryAt) return
        connect(next)
    }
    data class RoomStatus(val generation: Long, val context: Context?, val ready: Boolean,
        val atomic: Boolean, val drained: Boolean)
    @Synchronized fun roomStatus() = RoomStatus(generation,context,ready,roomSparklings,incoming.isEmpty())
    /** Bounded even when a producer continuously supplies frames. Only called on the client thread. */
    @Synchronized fun drainIncoming(): RoomStatus {
        val next=context ?: return roomStatus()
        repeat(128) {
            val item=incoming.poll() ?: return roomStatus()
            if(item.generation!=generation) return@repeat
            val root=runCatching { JsonParser.parseString(item.text).asJsonObject }.getOrNull()
            if(root==null) { fail(item.generation);return roomStatus() }
            dispatch(item.generation,root,next)
        }
        return roomStatus()
    }
    /** Network delivery and permission consumption share this lock, never a cached ready boolean. */
    @Synchronized fun withCurrentRoom(expected: Long, action: () -> Boolean): Boolean {
        val status=drainIncoming()
        return status.generation==expected && status.ready && status.atomic && status.drained && status.context!=null && action()
    }
    @Synchronized private fun dispatch(attempt: Long, root: JsonObject, next: Context) {
        if (attempt != generation) return
            when(PartySyncProtocol.text(root.get("type"))) {
                "welcome" -> {
                    if(ready) { fail(attempt); return }
                    val map=PartySyncProtocol.welcome(root,next.self,next.roster)
                    if(map==null) { fail(attempt); return }
                    val features=root.get("features")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { PartySyncProtocol.text(it) }.orEmpty()
                    if(completionOnly && "critter-completion-v1" !in features) { fail(attempt);return }
                    if(backup && "failover-bootstrap-v1" !in features) { fail(attempt);return }
                    mapping=map; ready=true; failures=0
                    hotspotCapability="hotspots-v1" in features; biomeCapability="biome-reports-v1" in features
                    roomSparklings="room-sparklings-v1" in features
                    logger.info("Party sync room ready: members={} hotspots={} biomeReports={}",map.size,hotspotCapability,biomeCapability)
                    net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Connected to party room · ${map.size} players")
                    onReady()
                }
                "hotspots-unavailable" -> { hotspotCapability=false; onUnavailable();fail(generation) }
                else -> if(ready) onMessage(root,mapping)
            }
    }
    private fun connect(desired: Context) {
        connecting=true
        val attempt=++generation
        val now=System.currentTimeMillis()
        val auth=if(tokenOwner==desired.self && token.isNotBlank() && now<tokenUntil-120_000) CompletableFuture.completedFuture(token)
        else {
            val body=JsonObject().apply { addProperty("uuid",desired.self) }.toString()
            val request=HttpRequest.newBuilder(authEndpoint)
                .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()
            http.sendAsync(request,HttpResponse.BodyHandlers.ofString()).thenApply { response ->
                if(response.statusCode()!=200) throw Admission(response.statusCode(),retryDelay(response.headers()))
                if(response.body().length>8192) error("Invalid authentication response")
                val value=JsonParser.parseString(response.body()).asJsonObject
                val bearer=PartySyncProtocol.text(value.get("token"),4096)?.takeIf { it.isNotBlank() && '\r' !in it && '\n' !in it } ?: error("Invalid authentication response")
                val seconds=PartySyncProtocol.integer(value.get("expiresInSeconds"),120,86400) ?: error("Invalid authentication response")
                synchronized(this) { if(attempt==generation) { token=bearer; tokenOwner=desired.self; tokenUntil=System.currentTimeMillis()+seconds*1000 } }
                bearer
            }
        }
        auth.thenCompose { bearer ->
            if(attempt!=generation) return@thenCompose CompletableFuture.failedFuture<WebSocket>(CancellationException())
            val builder=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .header("Authorization","Bearer $bearer").header("X-SafariUtils-Lobby",desired.lobby)
                .header("X-SafariUtils-Player",desired.self).header("X-SafariUtils-Roster",desired.roster.joinToString(","))
            if(backup || completionOnly)builder.header("X-SM-Generation",routingGeneration!!).header("X-SM-Visit",visitNonce!!)
            builder.buildAsync(syncEndpoint,Listener(attempt))
        }.whenComplete { _,error -> if(error!=null) fail(attempt,error) }
    }
    private class Admission(val status: Int,val delay: Long): RuntimeException()
    private fun retryDelay(headers: HttpHeaders): Long {
        val value=headers.firstValue("Retry-After").orElse("")
        return value.toLongOrNull()?.coerceIn(0,3600)?.times(1000) ?: runCatching {
            (java.time.ZonedDateTime.parse(value,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis()).coerceIn(0,3_600_000)
        }.getOrDefault(0)
    }
    @Synchronized private fun fail(attempt: Long,error: Throwable?=null) {
        if(attempt!=generation) return
        var cause=error
        while(cause is CompletionException && cause.cause!=null) cause=cause.cause
        val response=(cause as? WebSocketHandshakeException)?.response
        val status=response?.statusCode() ?: (cause as? Admission)?.status
        if(status==401) { token=""; tokenUntil=0 }
        val delay=maxOf((2000L shl minOf(failures++,4)).coerceAtMost(30_000),
            response?.let { retryDelay(it.headers()) } ?: (cause as? Admission)?.delay ?: 0)
        generation++; socket?.abort(); socket=null; connecting=false; ready=false; hotspotCapability=false; biomeCapability=false; roomSparklings=false
        incoming.clear(); mapping=emptyMap(); retryAt=System.currentTimeMillis()+delay
        // No exception, URI, headers, response bodies, bearer tokens or payload dumps in logs.
        logger.info("Party sync reconnect scheduled: status={} retryMs={}",status ?: "transport",delay)
        net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Retry in ${delay / 1000}s · ${status ?: "transport"}")
    }
    fun send(sequence: Long,kind: String,payload: String,catchUpOnly: Boolean=false): Boolean {
        if(!ready) return false
        val current=socket ?: return false
        val now=System.currentTimeMillis()
        while(sent.firstOrNull()?.let { now-it>=10_000 }==true) sent.removeFirst()
        if(sent.size>=36 || queued>=64) return false
        val text=PartySyncProtocol.envelope(sequence,kind,payload,catchUpOnly)
        val attempt=generation
        sent.addLast(now)
        // The socket is captured here: accepted sends drain to their original room before close.
        enqueue { current.sendText(text,true) }
            .exceptionally { fail(attempt); null }
        net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Queued $kind #$sequence" +
            if (kind != "A") "" else if (catchUpOnly) " · catch-up" else " · live")
        return true
    }
    @Synchronized private fun enqueue(operation: () -> CompletableFuture<WebSocket>): CompletableFuture<*> {
        queued++
        val next=tail.handle { _,_ -> null }.thenCompose { operation() }
        tail=next.whenComplete { _,_ -> synchronized(this) { queued-- } }
        return next
    }
    /** Explicit exit is serialized after queued updates and before close. Reconnect never sends leave. */
    @Synchronized fun close(explicit: Boolean,leaveSequence: Long=0) {
        val old=socket
        val pending=tail
        val leave=if(explicit && ready && hotspotCapability && leaveSequence>0)
            PartySyncProtocol.envelope(leaveSequence,"H","{\"version\":1,\"action\":\"leave\"}") else null
        generation++; ready=false; connecting=false; hotspotCapability=false; biomeCapability=false; roomSparklings=false; socket=null
        incoming.clear(); mapping=emptyMap(); sent.clear(); context=null
        if(old!=null) pending.handle { _,_ -> null }.thenCompose {
            if(leave!=null) old.sendText(leave,true) else CompletableFuture.completedFuture(old)
        }.thenCompose { old.sendClose(WebSocket.NORMAL_CLOSURE,"visit ended") }.orTimeout(2,TimeUnit.SECONDS).exceptionally { old.abort(); null }
        tail=CompletableFuture.completedFuture(null)
    }
    private inner class Listener(private val attempt: Long): WebSocket.Listener {
        private val text=StringBuilder()
        override fun onOpen(webSocket: WebSocket) {
            synchronized(this@PartySyncTransport) {
                if(attempt!=generation) { webSocket.abort(); return }
                socket=webSocket; connecting=false; openedAt=System.currentTimeMillis(); lastInboundAt=openedAt; pingAt=openedAt+25_000
            }
            webSocket.request(1)
        }
        override fun onText(webSocket: WebSocket,data: CharSequence,last: Boolean): CompletionStage<*>? {
            if(attempt!=generation) { webSocket.request(1); return null }
            lastInboundAt=System.currentTimeMillis()
            if(text.length+data.length>8192) { fail(attempt); return null }
            text.append(data)
            if(last) {
                val value=text.toString(); text.setLength(0)
                synchronized(this@PartySyncTransport) {
                    if(attempt==generation && (value.toByteArray(Charsets.UTF_8).size>8192 ||
                        !incoming.offer(Delivery(attempt,value)))) fail(attempt)
                }
            }
            webSocket.request(1); return null
        }
        override fun onPing(webSocket: WebSocket,message: ByteBuffer): CompletionStage<*>? {
            if(attempt!=generation) { webSocket.request(1);return null }
            lastInboundAt=System.currentTimeMillis()
            if(queued>=64) { fail(attempt);webSocket.abort();return null }
            val bytes=ByteBuffer.allocate(message.remaining()).put(message).flip()
            val reply=enqueue { webSocket.sendPong(bytes) }; webSocket.request(1); return reply
        }
        override fun onPong(webSocket: WebSocket,message: ByteBuffer): CompletionStage<*>? {
            if(attempt==generation) lastInboundAt=System.currentTimeMillis()
            webSocket.request(1); return null
        }
        override fun onClose(webSocket: WebSocket,statusCode: Int,reason: String): CompletionStage<*>? { fail(attempt); return null }
        override fun onError(webSocket: WebSocket,error: Throwable) { fail(attempt,error) }
    }
}
