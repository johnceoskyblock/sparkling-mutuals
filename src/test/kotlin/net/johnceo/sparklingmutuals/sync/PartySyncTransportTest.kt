package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonParser
import java.net.ServerSocket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncTransportTest {
    @Test fun `real asynchronous transport validates welcome serializes sends and flushes leave before close`() {
        val frames=CopyOnWriteArrayList<String>()
        val failure=AtomicReference<Throwable>()
        val self="0".repeat(31)+"1";val peer="0".repeat(31)+"2"
        ServerSocket(0).use { server ->
            server.soTimeout=8000
            val worker=Thread {
                try {
                    server.accept().use { auth ->
                        auth.soTimeout=8000
                        val input=auth.getInputStream().buffered()
                        fun line(): String = buildString { while(true) { val b=input.read();if(b<0 || b==10) break;if(b!=13) append(b.toChar()) } }
                        assertTrue(line().startsWith("POST /auth "))
                        val headers=generateSequence { line().takeIf { it.isNotEmpty() } }.associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
                        val body=String(input.readNBytes(headers.getValue("content-length").toInt()),StandardCharsets.UTF_8)
                        assertEquals("""{"uuid":"$self"}""",body)
                        val response="""{"token":"local-test-token","expiresInSeconds":900}"""
                        auth.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: ${response.toByteArray().size}\r\nConnection: close\r\n\r\n$response").toByteArray())
                    }
                    server.accept().use { socket ->
                        socket.soTimeout=8000
                        val input=socket.getInputStream().buffered();val output=socket.getOutputStream()
                        fun line(): String = buildString { while(true) { val b=input.read();if(b<0 || b==10) break;if(b!=13) append(b.toChar()) } }
                        assertTrue(line().startsWith("GET /sync "))
                        val headers=generateSequence { line().takeIf { it.isNotEmpty() } }.associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
                        assertEquals("Bearer local-test-token",headers["authorization"])
                        assertEquals("miniTest",headers["x-safariutils-lobby"])
                        assertEquals("$self,$peer",headers["x-safariutils-roster"])
                        val accept=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((headers.getValue("sec-websocket-key")+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                        output.write("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n".toByteArray())
                        val welcome="""{"type":"welcome","self":"a","roster":["a","b"],"features":["hotspots-v1","biome-reports-v1","critter-completion-v1"]}""".toByteArray()
                        output.write(129)
                        if (welcome.size < 126) output.write(welcome.size)
                        else { output.write(126); output.write(welcome.size ushr 8); output.write(welcome.size and 255) }
                        output.write(welcome);output.flush()
                        while(true) {
                            val opcode=input.read();if(opcode<0 || opcode and 15==8) break
                            var size=input.read() and 127
                            if(size==126) size=(input.read() shl 8) or input.read()
                            val mask=input.readNBytes(4);val body=input.readNBytes(size)
                            for(i in body.indices) body[i]=(body[i].toInt() xor mask[i%4].toInt()).toByte()
                            frames.add(String(body,StandardCharsets.UTF_8))
                        }
                    }
                } catch(error: Throwable) { failure.set(error) }
            }.apply { isDaemon=true;start() }
            val transport=PartySyncTransport(URI.create("http://localhost:${server.localPort}/auth"),URI.create("ws://localhost:${server.localPort}/sync"))
            val context=PartySyncTransport.Context("miniTest",self,listOf(self,peer))
            val deadline=System.currentTimeMillis()+6000
            while(!transport.ready && System.currentTimeMillis()<deadline && failure.get()==null) {
                transport.tick(context,System.currentTimeMillis());Thread.sleep(10)
            }
            failure.get()?.let { throw AssertionError("Local protocol server failed",it) }
            assertTrue(transport.ready)
            assertTrue(transport.hotspotCapability);assertTrue(transport.biomeCapability)
            assertTrue(transport.critterCapability)
            assertTrue(transport.send(1,"N","7"));assertTrue(transport.send(2,"S","1,0,0,0"))
            transport.close(true,3)
            assertFalse(transport.critterCapability)
            worker.join(4000)
            failure.get()?.let { throw AssertionError("Local protocol server failed",it) }
            assertEquals(3,frames.size)
            val parsed=frames.map { JsonParser.parseString(it).asJsonObject }
            assertEquals(listOf(1L,2L,3L),parsed.map { it.get("sequence").asLong })
            assertEquals(listOf("N","S","H"),parsed.map { it.get("kind").asString })
            assertEquals("leave",JsonParser.parseString(parsed.last().get("payload").asString).asJsonObject.get("action").asString)
        }
    }
}
