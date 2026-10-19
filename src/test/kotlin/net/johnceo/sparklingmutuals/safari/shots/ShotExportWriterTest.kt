package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.*
import java.io.*
import java.util.concurrent.*
import com.google.gson.JsonParser

class ShotExportWriterTest {
    @TempDir lateinit var directory: Path
    private fun writer(budget: ShotByteBudget, token: String = "session-test", stream: (Path)->OutputStream = { Files.newOutputStream(it,StandardOpenOption.CREATE_NEW) }) =
        ShotExportWriter.open(directory,token,budget,{},stream).also { it.ready.get(2,TimeUnit.SECONDS) }
    @Test fun `closed stream is finalized without overwriting and counts real encoded bytes`() {
        val budget=ShotByteBudget();val writer=writer(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget)
        session.accept(ShotStamp(0,0),ShotTestFixtures.input());session.stop(ShotStamp(1,0),ShotStopReason.EXPORT)
        val result=writer.result.get(3,TimeUnit.SECONDS)
        assertNotNull(result.path);assertTrue(result.complete)
        assertEquals(session.status().encodedBytes,Files.size(result.path!!))
        assertFalse(Files.exists(directory.resolve("session-test.jsonl.part")))
        assertEquals(0,budget.usedBytes)
        assertEquals("footer",JsonParser.parseString(Files.readAllLines(result.path).last()).asJsonObject["type"].asString)
    }
    @Test fun `previous export remains intact when final destination already exists`() {
        val old=directory.resolve("session-test.jsonl");Files.writeString(old,"previous export")
        val budget=ShotByteBudget();val writer=writer(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget);session.stop(ShotStamp(0,0),ShotStopReason.EXPORT)
        val result=writer.result.get(3,TimeUnit.SECONDS)
        assertEquals("previous export",Files.readString(old));assertFalse(result.complete)
        assertTrue(Files.exists(directory.resolve("session-test.jsonl.part")));assertEquals(0,budget.usedBytes)
    }
    @Test fun `IO failure stops producer and retains partial data with a safe error category`() {
        val budget=ShotByteBudget();val writer=writer(budget,stream={ path ->
            Files.newOutputStream(path,StandardOpenOption.CREATE_NEW).also { it.close() }
            object:OutputStream() { override fun write(value: Int) { throw IOException("private filesystem detail") } }
        })
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget)
        val result=writer.result.get(3,TimeUnit.SECONDS)
        assertEquals("io_error",result.error);assertFalse(result.complete)
        assertDoesNotThrow { session.accept(ShotStamp(1,0),ShotTestFixtures.input());session.stop(ShotStamp(2,0),ShotStopReason.ERROR) }
        assertEquals(0,budget.usedBytes)
        assertTrue(Files.exists(directory.resolve("session-test.jsonl.part")))
    }
    @Test fun `blocked writer exhausts bounded queue without waiting on disk`() {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val budget=ShotByteBudget()
        val writer=writer(budget,stream={ path ->
            val output=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)
            object:FilterOutputStream(output) { override fun write(b:ByteArray,off:Int,len:Int) { entered.countDown();release.await();out.write(b,off,len) } }
        })
        try {
            val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget)
            assertTrue(entered.await(2,TimeUnit.SECONDS))
            session.accept(ShotStamp(0,0),ShotTestFixtures.input())
            val points=List(64) { ShotPathPoint(ShotVector(it.toDouble(),0.0,0.0)) }
            repeat(1500) { session.accept(ShotStamp(1,0),ShotObservation.Packet("c1",ShotUpdateKind.MOVE,points)) }
            assertEquals(ShotStopReason.QUEUE_LIMIT,session.status().stopReason)
            assertTrue(budget.usedBytes<=4*1024*1024)
            release.countDown();assertFalse(writer.result.get(4,TimeUnit.SECONDS).complete)
            assertEquals(0,budget.usedBytes)
        } finally { release.countDown();writer.shutdown() }
    }
    @Test fun `invalid session token cannot choose a different output path`() {
        assertThrows(IllegalArgumentException::class.java) { ShotExportWriter.open(directory,"../elsewhere",ShotByteBudget(),{}) }
    }
    @Test fun `existing partial export cannot be reopened or overwritten`() {
        val path=directory.resolve("session-test.jsonl.part");Files.writeString(path,"unfinished previous export")
        val writer=ShotExportWriter.open(directory,"session-test",ShotByteBudget(),{})
        assertThrows(ExecutionException::class.java) { writer.ready.get(2,TimeUnit.SECONDS) }
        assertFalse(writer.result.get(2,TimeUnit.SECONDS).complete)
        assertEquals("unfinished previous export",Files.readString(path))
    }
    @Test fun `shutdown timeout leaves partial file even if a blocked stream eventually returns`() {
        val entered=CountDownLatch(1);val release=CountDownLatch(1);val budget=ShotByteBudget()
        val writer=writer(budget,stream={ path ->
            val output=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)
            object:FilterOutputStream(output) {
                override fun write(b:ByteArray,off:Int,len:Int) {
                    entered.countDown()
                    while (release.count>0) try { release.await() } catch (_:InterruptedException) {}
                    out.write(b,off,len)
                }
            }
        })
        try {
            val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget)
            assertTrue(entered.await(2,TimeUnit.SECONDS));session.stop(ShotStamp(0,0),ShotStopReason.SHUTDOWN)
            writer.shutdown(1);release.countDown()
            val result=writer.result.get(3,TimeUnit.SECONDS)
            assertFalse(result.complete);assertEquals("shutdown_timeout",result.error)
            assertFalse(Files.exists(directory.resolve("session-test.jsonl")));assertEquals(0,budget.usedBytes)
        } finally { release.countDown();writer.shutdown() }
    }
    @Test fun `mutable observation collection is encoded before the worker can observe later changes`() {
        val budget=ShotByteBudget();val writer=writer(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget)
        session.accept(ShotStamp(0,0),ShotTestFixtures.input())
        val points=mutableListOf(ShotPathPoint(ShotVector(1.0,2.0,3.0)))
        session.accept(ShotStamp(1,0),ShotObservation.Packet("c1",ShotUpdateKind.POSITION_SYNC,points))
        points.clear();session.stop(ShotStamp(2,0),ShotStopReason.EXPORT)
        val result=writer.result.get(3,TimeUnit.SECONDS)
        val packet=Files.readAllLines(result.path!!).map { JsonParser.parseString(it).asJsonObject }.first { it["kind"]?.asString=="packet" }
        assertEquals(1,packet["data"].asJsonObject["points"].asJsonArray.size())
    }
}
