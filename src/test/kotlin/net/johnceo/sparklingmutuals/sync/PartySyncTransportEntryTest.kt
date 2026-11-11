package net.johnceo.sparklingmutuals.sync

import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncTransportEntryTest {
    private val self="00000000000000000000000000000001"
    private val peer="00000000000000000000000000000002"
    @Test fun `protocol two withholds replay until the matching game thread boundary and resumes the same visit`() {
        Fixture().use { server ->
            val transport=PartySyncTransport(server.auth,server.ws)
            val context=PartySyncTransport.Context("m88A",self,listOf(self,peer))
            val connection="10000000-0000-0000-0000-000000000001"
            val run="20000000-0000-0000-0000-000000000001"
            val events=mutableListOf<String>()
            transport.onMessage={ root,_ ->
                assertFalse(transport.ready,"replay is applied before readiness")
                events.add(root["type"].asString)
            }
            transport.onReady={ events.add("ready") }
            fun welcome(id: String) =
                """{"type":"welcome","self":"s","roster":["s","p"],"connectionId":"$id","features":["run-reconciliation-v1","critter-completion-v1","hotspots-v1","biome-reports-v1","room-sparklings-v1"]}"""
            fun boundary(type: String,id: String=connection) =
                """{"type":"$type","version":1,"connectionId":"$id","visitId":"${transport.activeVisitId}","runId":"$run","status":"joined"}"""
            try {
                transport.tick(context,System.currentTimeMillis())
                val socket=server.connections.poll(5,TimeUnit.SECONDS) ?: error("No upgrade")
                assertEquals("2",server.upgrades.poll(5,TimeUnit.SECONDS)?.get("x-safariutils-sync-protocol"))
                server.send(socket,welcome(connection))
                await { transport.drainIncoming();transport.coordinationAvailable }
                assertFalse(transport.ready)
                assertTrue(transport.canSend("H"),"adapter must permit the hello that establishes readiness")
                assertFalse(transport.canSend("A"),"gameplay must wait for replay")
                assertFalse(transport.canSend("C"),"completion must wait for replay")
                assertTrue(transport.send(1,"H","""{"version":1,"action":"hello"}"""))
                val hello=com.google.gson.JsonParser.parseString(server.read(socket)).asJsonObject
                val inner=com.google.gson.JsonParser.parseString(hello["payload"].asString).asJsonObject
                assertEquals(transport.activeVisitId,inner["visitId"].asString)
                assertTrue(inner["resumeRunId"].isJsonNull)
                server.send(socket,boundary("reconcile"))
                server.send(socket,"""{"type":"state","sender":"p","sequence":1,"kind":"N","payload":"1","runId":"$run"}""")
                server.send(socket,boundary("replay-complete","30000000-0000-0000-0000-000000000001"))
                await { transport.drainIncoming();transport.acceptedRunId==run }
                assertFalse(transport.ready);assertTrue(events.isEmpty())
                server.send(socket,"""{"type":"sparklings","payload":"0,0,1,0","runId":"$run"}""")
                server.send(socket,boundary("replay-complete"))
                await { transport.drainIncoming().ready }
                assertEquals(listOf("state","sparklings","ready"),events)
                assertTrue(transport.canSend("A"))
                assertTrue(transport.canSend("C"))
                val visit=transport.activeVisitId
                transport.close(false)
                transport.tick(context,System.currentTimeMillis())
                val next=server.connections.poll(5,TimeUnit.SECONDS) ?: error("No resumed upgrade")
                val nextConnection="40000000-0000-0000-0000-000000000001"
                server.send(next,welcome(nextConnection))
                await { transport.drainIncoming();transport.coordinationAvailable }
                assertEquals(visit,transport.activeVisitId)
                assertFalse(transport.ready)
                assertTrue(transport.canSend("H"),"reconnect hello must also bypass the gameplay gate")
                assertTrue(transport.send(2,"H","""{"version":1,"action":"hello"}"""))
                val resumed=com.google.gson.JsonParser.parseString(server.read(next)).asJsonObject
                assertEquals(run,com.google.gson.JsonParser.parseString(resumed["payload"].asString).asJsonObject["resumeRunId"].asString)
            } finally { transport.close(false) }
        }
    }
    @Test fun `real socket capture is drained before action and loss revokes generation`() {
        Fixture().use { server ->
            val transport=PartySyncTransport(server.auth,server.ws)
            val room=PartyRoomDiscoveries()
            transport.onReady={ val s=transport.roomStatus();room.welcome(s.generation,s.atomic) }
            transport.onMessage={ root,_ ->
                if(root.get("type")?.asString=="sparklings") room.receive(transport.roomStatus().generation,root.get("payload").asString)
            }
            try {
                transport.tick(PartySyncTransport.Context("m88A",self,listOf(self,peer)),System.currentTimeMillis())
                val socket=server.connections.poll(5,TimeUnit.SECONDS) ?: error("No WebSocket upgrade")
                server.send(socket,"""{"type":"welcome","self":"s","roster":["s","p"],"features":["room-sparklings-v1"]}""")
                await { transport.drainIncoming().ready }
                assertFalse(transport.critterCapability)
                assertNull(room.snapshot(transport.roomStatus().generation))
                server.send(socket,"""{"type":"sparklings","payload":"1,0,0,0"}""")
                await { transport.drainIncoming();room.snapshot(transport.roomStatus().generation)!=null }
                val epoch=transport.roomStatus().generation
                var calls=0
                assertTrue(transport.withCurrentRoom(epoch) { if(room.snapshot(epoch)!!.active.isEmpty()) false else {calls++;true} })
                server.send(socket,"""{"type":"sparklings","payload":"1,0,1,0"}""")
                await { (transport.developerSnapshot()["incomingQueue"] as Int)>0 }
                assertFalse(transport.withCurrentRoom(epoch) { if(room.snapshot(epoch)!!.active.isEmpty()) false else {calls++;true} })
                assertEquals(1,calls)
                server.send(socket,"not json")
                await { (transport.developerSnapshot()["incomingQueue"] as Int)>0 }
                assertFalse(transport.withCurrentRoom(epoch) { calls++;true })
                assertFalse(transport.roomStatus().ready)
                assertNotEquals(epoch,transport.roomStatus().generation)
                assertEquals(1,calls)
            } finally { transport.close(false) }
        }
    }
    @Test fun `socket close withdraws a previously ready action boundary`() {
        Fixture().use { server ->
            val transport=PartySyncTransport(server.auth,server.ws)
            try {
                transport.tick(PartySyncTransport.Context("m88A",self,listOf(self,peer)),System.currentTimeMillis())
                val socket=server.connections.poll(5,TimeUnit.SECONDS) ?: error("No upgrade")
                server.send(socket,"""{"type":"welcome","self":"s","roster":["s","p"],"features":["room-sparklings-v1"]}""")
                await { transport.drainIncoming().ready }
                val epoch=transport.roomStatus().generation
                socket.close()
                await { transport.roomStatus().generation!=epoch }
                var acted=false
                assertFalse(transport.withCurrentRoom(epoch) { acted=true;true });assertFalse(acted)
            } finally { transport.close(false) }
        }
    }
    private fun await(condition: () -> Boolean) {
        val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(!condition()) { check(System.nanoTime()<end) { "Timed out awaiting local transport" };LockSupport.parkNanos(1_000_000) }
    }
    /** Minimal real HTTP auth + RFC6455 server; no production credentials or endpoints. */
    private class Fixture: AutoCloseable {
        private val server=ServerSocket(0,8,java.net.InetAddress.getLoopbackAddress())
        private val sockets=java.util.concurrent.CopyOnWriteArrayList<Socket>()
        val auth=URI("http://127.0.0.1:${server.localPort}/auth")
        val ws=URI("ws://127.0.0.1:${server.localPort}/sync")
        val connections=LinkedBlockingQueue<Socket>()
        val upgrades=LinkedBlockingQueue<Map<String,String>>()
        init { thread(isDaemon=true,name="cj-local-ws") {
            while(!server.isClosed) {
                val socket=runCatching { server.accept() }.getOrNull() ?: break
                sockets.add(socket)
                thread(isDaemon=true) { runCatching { serve(socket) } }
            }
        } }
        private fun serve(socket: Socket) {
            socket.soTimeout=5000
            val input=socket.getInputStream();val bytes=ArrayList<Byte>();var text=""
            while(!text.endsWith("\r\n\r\n")) {
                val b=input.read();check(b>=0 && bytes.size<8192);bytes.add(b.toByte())
                text=bytes.toByteArray().toString(Charsets.US_ASCII)
            }
            val headers=text.lines().drop(1).mapNotNull { line -> line.indexOf(':').takeIf { it>0 }?.let {
                line.substring(0,it).lowercase() to line.substring(it+1).trim()
            } }.toMap()
            if(text.startsWith("POST ")) {
                repeat(headers["content-length"]!!.toInt()) { check(input.read()>=0) }
                val body="""{"token":"local-test-token","expiresInSeconds":3600}"""
                socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n"+body).toByteArray())
                socket.close()
            } else {
                upgrades.put(headers)
                val key=headers["sec-websocket-key"] ?: error("No WS key")
                val accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                socket.getOutputStream().write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n".toByteArray())
                socket.getOutputStream().flush();connections.put(socket)
            }
        }
        fun read(socket: Socket): String {
            val input=socket.getInputStream()
            check(input.read()>=0)
            var size=input.read() and 127
            if(size==126) size=(input.read() shl 8) or input.read()
            check(size in 0..8192)
            val mask=input.readNBytes(4)
            val body=input.readNBytes(size)
            body.indices.forEach { body[it]=(body[it].toInt() xor mask[it%4].toInt()).toByte() }
            return body.toString(Charsets.UTF_8)
        }
        fun send(socket: Socket, value: String) {
            val data=value.toByteArray(Charsets.UTF_8);val out=socket.getOutputStream()
            out.write(0x81)
            if(data.size<126) out.write(data.size) else { out.write(126);out.write(data.size shr 8);out.write(data.size and 255) }
            out.write(data);out.flush()
        }
        override fun close() { server.close();sockets.forEach { runCatching { it.close() } } }
    }
}
