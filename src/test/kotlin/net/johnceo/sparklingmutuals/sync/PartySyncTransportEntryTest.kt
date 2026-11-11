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
                val key=headers["sec-websocket-key"] ?: error("No WS key")
                val accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                socket.getOutputStream().write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n".toByteArray())
                socket.getOutputStream().flush();connections.put(socket)
            }
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
