package net.johnceo.sparklingmutuals.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PartySyncRoutingHttpTest {
    private val a="1".repeat(32)
    private val b="2".repeat(32)
    private val context=PartySyncTransport.Context("m1",a,listOf(a,b))
    private fun await(action: ()->Boolean) {
        val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(!action()){check(System.nanoTime()<until);LockSupport.parkNanos(1_000_000)}
    }
    @Test fun `real async route sends identity only and healthy client follows monotonic backup choice`() {
        val requests=CopyOnWriteArrayList<JsonObject>()
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/v1/auth/uuid/session") { exchange ->
            val response="""{"token":"test-only-token","expiresInSeconds":600}""".toByteArray()
            exchange.sendResponseHeaders(200,response.size.toLong());exchange.responseBody.use {it.write(response)}
        }
        server.createContext("/v1/route") { exchange ->
            val body=JsonParser.parseString(exchange.requestBody.bufferedReader().readText()).asJsonObject;requests.add(body)
            val response=JsonObject().apply {addProperty("version",1);addProperty("generation","generation");addProperty("provider","backup");addProperty("admitted",true);add("nonce",body.get("nonce"))}.toString().toByteArray()
            exchange.sendResponseHeaders(200,response.size.toLong());exchange.responseBody.use {it.write(response)}
        }
        server.start()
        try {
            val routing=PartySyncRouting(URI.create("http://127.0.0.1:${server.address.port}"))
            val before=System.nanoTime();assertFalse(routing.tick(context,0,true,true));assertTrue(System.nanoTime()-before<TimeUnit.SECONDS.toNanos(1))
            await { routing.tick(context,1,true,true);routing.provider=="backup" }
            val body=requests.first();assertEquals(setOf("version","lobby","self","roster","nonce","unusableMs","action"),body.keySet())
            assertEquals(0,body.get("unusableMs").asInt);assertEquals("generation",routing.generation)
            routing.reset();assertEquals("primary",routing.provider)
        } finally {server.stop(0)}
    }
    @Test fun `solo and ineligible sessions do no routing and unhealthy routing never changes primary`() {
        val requests=CopyOnWriteArrayList<String>()
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->requests.add(exchange.requestURI.path);exchange.responseHeaders.add("Retry-After","120");exchange.sendResponseHeaders(503,-1);exchange.close() }
        server.start()
        try {
            val routing=PartySyncRouting(URI.create("http://127.0.0.1:${server.address.port}"))
            routing.tick(context.copy(roster=listOf(a)),0,false,true);routing.tick(context,0,false,false)
            Thread.sleep(30);assertTrue(requests.isEmpty())
            routing.tick(context,1,true,true);await {requests.isNotEmpty()}
            Thread.sleep(30);routing.tick(context,2,true,true);val count=requests.size
            routing.tick(context,60_000,true,true);Thread.sleep(30);assertEquals(count,requests.size);assertEquals("primary",routing.provider)
        } finally {server.stop(0)}
    }
}
