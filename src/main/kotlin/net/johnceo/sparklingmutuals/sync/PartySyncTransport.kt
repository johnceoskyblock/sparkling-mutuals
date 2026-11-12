package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.*
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.*
import org.slf4j.LoggerFactory

/** Delivery-queue time cannot renew a replayed completion snapshot. */
internal fun queuedCompletionAge(serverAge: Long, queuedNanos: Long): Long {
    val lifetime = net.johnceo.sparklingmutuals.safari.completion.SharedCritterProgress.TTL
    return minOf(lifetime, serverAge.coerceIn(0, lifetime) +
        (queuedNanos.coerceAtLeast(0) / 1_000_000).coerceAtMost(lifetime))
}

/** One asynchronous socket. Network callbacks enqueue bounded data; only tick mutates game state. */
internal class PartySyncTransport(
    private var authEndpoint: URI=URI.create("https://api-dev.serkosea.dev/v1/auth/uuid/session"),
    private var syncEndpoint: URI=URI.create("wss://api-dev.serkosea.dev/v1/sync")
) {
    private val primaryAuth=authEndpoint
    private val primarySync=syncEndpoint
    private val visit=PartySyncVisit()
    private var welcomed=false
    private val replay=ArrayDeque<Delivery>()
    var efficient=false; private set
    @Volatile var updateRequired=false; private set
    private val batch=mutableListOf<String>()
    private var completionPayload: String?=null
    private var completionRevision: Long?=null
    private var renewAt=0L
    private var renewalStarted: Long?=null
    private var helloRetryAt=0L
    private var replayDeadline: Long?=null
    private var resyncRequested=false
    var onCompletionResync: () -> Unit={}
    fun retryHello(now: Long,lastSent: Long) = if(efficient) !ready && now>=helloRetryAt && now-lastSent>=2_000
        else now-lastSent >= if(runCapability && !ready) 2_000 else 15_000

    private var completionOnly=false
    var backup=false; private set
    private var routingGeneration: String?=null
    private var visitNonce: String?=null
    @Synchronized fun useBackup(origin: URI,generation: String,nonce: String) {
        require(origin.scheme=="https" && origin.host!=null && generation.isNotBlank() && nonce.isNotBlank())
        close(false);token="";tokenOwner="";tokenUntil=0;retryAt=0;failures=0
        authEndpoint=origin.resolve("/v1/auth/uuid/session")
        syncEndpoint=URI("wss",null,origin.host,origin.port,"/v1/sync",null,null)
        if (!backup || routingGeneration != generation) visit.changeAuthority()
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
    private data class Delivery(val generation: Long,val text: String,val receivedAt: Long=System.nanoTime())
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
    var critterCapability=false; private set
    var runCapability=false; private set
    val coordinationAvailable get()=welcomed
    val acceptedRunId get()=visit.runId.takeIf { runCapability && visit.reconciled }
    val activeVisitId get()=visit.visitId
    val completionIdle get()=ready && (efficient || queued==0)
    fun newVisit() { visit.newVisit() }
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
    var onWelcome: () -> Unit={}
    var onMessage: (JsonObject,Map<String,String>) -> Unit={ _,_ -> }
    var onUnavailable: () -> Unit={}
    fun developerStatus(): String = when {
        updateRequired -> "Sync update required"
        ready -> if(backup) "Backup connected" else "Primary connected"
        welcomed && runCapability -> "Run ${visit.status}"
        socket != null -> "Awaiting welcome"
        connecting -> "Connecting"
        retryAt > System.currentTimeMillis() -> "Retry backoff"
        else -> "Idle"
    }

    fun developerSnapshot() = mapOf(
        "status" to developerStatus(), "generation" to generation, "failures" to failures,
        "retryAtMillis" to retryAt, "incomingQueue" to incoming.size, "outgoingQueue" to queued,
        "hotspotCapability" to hotspotCapability, "biomeCapability" to biomeCapability,
        "critterCapability" to critterCapability, "roomSparklings" to roomSparklings,
        "runCapability" to runCapability, "visitId" to visit.visitId, "runId" to visit.runId,
        "replayComplete" to visit.replayComplete,
        "mappedPlayers" to mapping.toMap(), "lastInboundAtMillis" to lastInboundAt
    )
    fun tick(next: Context,now: Long) {
        if(!next.lobby.matches(Regex("[A-Za-z0-9_-]{1,64}")) || PartySyncProtocol.compact(next.self)==null ||
            next.roster.size !in 1..4 || next.roster.toSet().size!=next.roster.size || next.self !in next.roster ||
            next.roster.any { PartySyncProtocol.compact(it)==null }) { close(false);return }
        if(updateRequired) return
        if(context!=next) { close(false); context=next; retryAt=0; failures=0 }
        drainIncoming()
        val current=socket
        if(current!=null) {
            if(replayDeadline?.let { now>=it }==true) { fail(generation); return }
            if(ready) flushBatch()
            if(efficient && renewalStarted?.let { now-it>=20_000 }==true) { fail(generation); return }
            if(efficient && welcomed && renewalStarted==null && now>=renewAt) renew(current,next.self,now)
            if(!welcomed && now-openedAt>12_000) { fail(generation); return }
            // A successful ping write alone does not prove the room is still reachable.
            if(welcomed && now-lastInboundAt>75_000) { fail(generation); return }
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
            if (PartySyncProtocol.text(root.get("type")) == "state" &&
                PartySyncProtocol.text(root.get("kind")) == "C") {
                val age = PartySyncProtocol.integer(root.get("ageMillis"), 0)
                if (age != null) root.addProperty("ageMillis",
                    queuedCompletionAge(age, System.nanoTime() - item.receivedAt))
            }
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
                    if(welcomed) { fail(attempt); return }
                    val map=PartySyncProtocol.welcome(root,next.self,next.roster)
                    if(map==null) { fail(attempt); return }
                    val features=root.get("features")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { PartySyncProtocol.text(it) }.orEmpty()
                    if(completionOnly && "critter-completion-v1" !in features) { fail(attempt);return }
                    if(backup && "failover-bootstrap-v1" !in features) { fail(attempt);return }
                    runCapability=!completionOnly && "run-reconciliation-v1" in features
                    if (runCapability && !visit.welcome(PartySyncProtocol.text(root.get("connectionId")) ?: "")) {
                        fail(attempt); return
                    }
                    efficient=runCapability && "efficient-transport-v1" in features
                    renewAt=openedAt+485_000
                    mapping=map; welcomed=true; ready=!runCapability; failures=0
                    hotspotCapability="hotspots-v1" in features; biomeCapability="biome-reports-v1" in features
                    critterCapability="critter-completion-v1" in features
                    roomSparklings="room-sparklings-v1" in features
                    logger.info("Party sync room ready: members={} hotspots={} biomeReports={}",map.size,hotspotCapability,biomeCapability)
                    net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Connected to party room · ${map.size} players")
                    onWelcome()
                    if (ready) onReady()
                }
                "reconcile" -> if (runCapability && welcomed) {
                    if (visit.reconcile(root)) {
                        ready=false
                        batch.clear()
                        completionRevision=null
                        val retry=PartySyncProtocol.integer(root.get("retryAfterMillis"),0,60_000) ?: 4_000
                        helloRetryAt=System.currentTimeMillis()+retry
                        replayDeadline=if(visit.reconciled) System.currentTimeMillis()+12_000 else null
                        replay.clear()
                    } else if (visit.matchesConnection(root)) fail(attempt)
                }
                "replay-complete" -> if (runCapability && welcomed && !ready && visit.matchesReplay(root)) {
                    while (replay.isNotEmpty()) {
                        val item = replay.removeFirst()
                        val frame = JsonParser.parseString(item.text).asJsonObject
                        val age = PartySyncProtocol.integer(frame.get("ageMillis"), 0)
                        if (age != null && PartySyncProtocol.text(frame.get("kind")) == "C")
                            frame.addProperty("ageMillis",queuedCompletionAge(age,System.nanoTime()-item.receivedAt))
                        onMessage(frame,mapping)
                        if (attempt != generation) return
                    }
                    replayDeadline=null
                    visit.finishReplay()
                    ready=true
                    onReady()
                }
                "renewed" -> if(efficient && renewalStarted!=null) {
                    val remaining=PartySyncProtocol.integer(root.get("expiresInMillis"),1,600_000)
                    if(remaining==null) { fail(attempt); return }
                    renewalStarted=null
                    renewAt=System.currentTimeMillis()+maxOf(1_000,remaining-115_000)
                }
                "completion-resync" -> if(efficient && ready && visit.accepts(root) &&
                    PartySyncProtocol.text(root.get("visitId"))==visit.visitId) {
                    completionRevision=null
                    resyncRequested=true
                    onCompletionResync()
                }
                "reconciling" -> if (backup && runCapability && welcomed && visit.accepts(root)) {
                    ready=false
                    replay.clear()
                    onMessage(root,mapping)
                } else if (backup && ready && !runCapability) onMessage(root,mapping)
                "hotspots-unavailable" -> { hotspotCapability=false; onUnavailable();fail(generation) }
                else -> {
                    if (runCapability) {
                        if (!visit.accepts(root)) return
                        if (!ready) {
                            if (replay.size >= 128) { fail(attempt); return }
                            replay.addLast(Delivery(attempt,root.toString()))
                            return
                        }
                    }
                    if(ready) onMessage(root,mapping)
                }
            }
    }
    private fun connect(desired: Context) {
        connecting=true
        val attempt=++generation
        val auth=sessionToken(desired.self,attempt)
        auth.thenCompose { bearer ->
            if(attempt!=generation) return@thenCompose CompletableFuture.failedFuture<WebSocket>(CancellationException())
            val builder=http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .header("Authorization","Bearer $bearer").header("X-SafariUtils-Lobby",desired.lobby)
                .header("X-SafariUtils-Player",desired.self).header("X-SafariUtils-Roster",desired.roster.joinToString(","))
            if(backup || completionOnly)builder.header("X-SM-Generation",routingGeneration!!).header("X-SM-Visit",visitNonce!!)
            if(!completionOnly)builder.header("X-SafariUtils-Sync-Protocol","3")
            builder.buildAsync(syncEndpoint,Listener(attempt))
        }.whenComplete { _,error -> if(error!=null) fail(attempt,error) }
    }
    private fun sessionToken(owner: String,attempt: Long): CompletableFuture<String> {
        val now=System.currentTimeMillis()
        return if(tokenOwner==owner && token.isNotBlank() && now<tokenUntil-120_000) CompletableFuture.completedFuture(token)
        else {
            val body=JsonObject().apply { addProperty("uuid",owner) }.toString()
            val request=HttpRequest.newBuilder(authEndpoint)
                .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build()
            http.sendAsync(request,HttpResponse.BodyHandlers.ofString()).thenApply { response ->
                if(response.statusCode()!=200) throw Admission(response.statusCode(),retryDelay(response.headers()))
                if(response.body().length>8192) error("Invalid authentication response")
                val value=JsonParser.parseString(response.body()).asJsonObject
                val bearer=PartySyncProtocol.text(value.get("token"),4096)?.takeIf { it.isNotBlank() && '\r' !in it && '\n' !in it } ?: error("Invalid authentication response")
                val seconds=PartySyncProtocol.integer(value.get("expiresInSeconds"),120,86400) ?: error("Invalid authentication response")
                synchronized(this) { if(attempt==generation) { token=bearer; tokenOwner=owner; tokenUntil=System.currentTimeMillis()+seconds*1000 } }
                bearer
            }
        }
    }
    private fun renew(current: WebSocket,owner: String,now: Long) {
        renewalStarted=now
        val attempt=generation
        sessionToken(owner,attempt).whenComplete { bearer,error ->
            synchronized(this) {
                if(attempt!=generation || current!==socket) return@synchronized
                if(error!=null) { fail(attempt,error); return@synchronized }
                val request=JsonObject().apply { addProperty("type","renew");addProperty("token",bearer) }
                enqueue { current.sendText(request.toString(),true) }.exceptionally { fail(attempt);null }
            }
        }
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
        if(status==426 && response?.headers()?.firstValue("x-safari-sync-update-required")?.orElse("")?.equals("true",true)==true)
            updateRequired=true
        if(status==401) { token=""; tokenUntil=0 }
        val delay=maxOf((2000L shl minOf(failures++,4)).coerceAtMost(30_000),
            response?.let { retryDelay(it.headers()) } ?: (cause as? Admission)?.delay ?: 0)
        generation++; socket?.abort(); socket=null; connecting=false; ready=false; welcomed=false; hotspotCapability=false; biomeCapability=false; critterCapability=false; runCapability=false; roomSparklings=false
        visit.disconnected(); replay.clear(); clearEfficient()
        incoming.clear(); mapping=emptyMap(); retryAt=System.currentTimeMillis()+delay
        // No exception, URI, headers, response bodies, bearer tokens or payload dumps in logs.
        logger.info("Party sync reconnect scheduled: status={} retryMs={}",status ?: "transport",delay)
        net.johnceo.sparklingmutuals.debug.SafariDeveloper.event("Retry in ${delay / 1000}s · ${status ?: "transport"}")
    }
    /** Reconciliation H messages must be able to establish readiness; gameplay waits for replay. */
    fun canSend(kind: String) = socket != null && (ready || welcomed && runCapability && (kind=="H" || backup && kind=="F"))
    fun sendCompletion(sequence: Long,payload: String): Boolean {
        val revision=completionRevision
        if(efficient && revision!=null && completionPayload==payload) {
            val refresh=JsonObject().apply {
                addProperty("version",1);addProperty("runId",acceptedRunId)
                addProperty("visitId",activeVisitId);addProperty("revision",revision)
            }
            return send(sequence,"F",refresh.toString())
        }
        val sent = if(resyncRequested) canSend("C") && flushBatch() &&
            sendText(PartySyncProtocol.envelope(sequence,"C",payload,false))
        else send(sequence,"C",payload)
        if(!sent) return false
        resyncRequested=false
        completionRevision=sequence
        completionPayload=payload
        return true
    }

    private fun clearEfficient() {
        efficient=false; renewalStarted=null; helloRetryAt=0
        batch.clear(); completionRevision=null; completionPayload=null; replayDeadline=null; resyncRequested=false
    }
    private fun batchText(frames: List<String>) = frames.joinToString(",", "{\"type\":\"batch\",\"frames\":[", "]}")
    private fun flushBatch(): Boolean {
        if(batch.isEmpty()) return true
        if(!sendText(batchText(batch))) return false
        batch.clear()
        return true
    }
    @Synchronized fun send(sequence: Long,kind: String,payload: String,catchUpOnly: Boolean=false): Boolean {
        if(!canSend(kind)) return false
        val scoped=if(runCapability && kind=="H") visit.operation(payload) else payload
        val frame=PartySyncProtocol.envelope(sequence,kind,scoped,catchUpOnly)
        if(efficient && ready && kind in setOf("A","N","U","C","F")) {
            if(batchText(listOf(frame)).toByteArray(Charsets.UTF_8).size>4096) return false
            if((batch.size>=8 || batchText(batch+frame).toByteArray(Charsets.UTF_8).size>4096) && !flushBatch()) return false
            batch.add(frame)
            return true
        }
        return flushBatch() && sendText(frame)
    }
    private fun sendText(text: String): Boolean {
        val current=socket ?: return false
        val now=System.currentTimeMillis()
        while(sent.firstOrNull()?.let { now-it>=10_000 }==true) sent.removeFirst()
        if(sent.size>=36 || queued>=64) return false
        val attempt=generation
        sent.addLast(now)
        enqueue { current.sendText(text,true) }.exceptionally { fail(attempt); null }
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
            PartySyncProtocol.envelope(leaveSequence,"H",if(runCapability)
                visit.operation("{\"version\":1,\"action\":\"leave\"}") else "{\"version\":1,\"action\":\"leave\"}") else null
        generation++; ready=false; welcomed=false; connecting=false; hotspotCapability=false; biomeCapability=false; critterCapability=false; runCapability=false; roomSparklings=false; socket=null
        visit.disconnected(); replay.clear(); clearEfficient()
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
