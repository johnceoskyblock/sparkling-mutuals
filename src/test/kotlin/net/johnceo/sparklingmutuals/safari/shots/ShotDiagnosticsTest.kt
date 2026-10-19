package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class ShotDiagnosticsTest {
    @TempDir lateinit var directory:Path
    @Test fun `codec failure keeps original cause locally and export is incomplete without private details`() {
        val failures=mutableListOf<ShotFailure>();val budget=ShotByteBudget()
        val writer=ShotExportWriter.open(directory,"codec",budget,{})
        writer.ready.get(3,TimeUnit.SECONDS)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),writer,budget,failures::add)
        assertFalse(session.accept(ShotStamp(1,1),ShotTestFixtures.input("s1").copy(pose=ShotTestFixtures.input("s1").pose.copy(yaw=Float.NaN))))
        val result=writer.result.get(3,TimeUnit.SECONDS)
        assertEquals(ShotStopReason.ERROR,session.status().stopReason);assertFalse(result.complete)
        assertEquals(1,failures.size);assertEquals("encode/input",failures.single().stage)
        assertInstanceOf(IllegalArgumentException::class.java,failures.single().cause)
        assertEquals(ShotStamp(1,1),failures.single().stamp)
        assertFalse(Files.readString(result.path!!).contains("NaN"))
    }
    @Test fun `writer failure reports stage original exception and nested cause once without dumping it to result`() {
        val original=IOException("private-message",IllegalStateException("nested-private"))
        val failures=ConcurrentLinkedQueue<ShotFailure>();val budget=ShotByteBudget()
        val writer=ShotExportWriter.openWithDiagnostics(directory,"io",budget,{},failures::add) { throw original }
        val result=writer.result.get(3,TimeUnit.SECONDS)
        assertEquals("io_error",result.error);assertFalse(result.complete)
        assertEquals(1,failures.size);assertSame(original,failures.single().cause)
        assertEquals("writer/open",failures.single().stage);assertEquals("io",failures.single().session)
        assertSame(original.cause,failures.single().cause.cause)
    }
    @Test fun `controller hook failure is diagnosed once with safe status and a later session can record`() {
        val tasks=ConcurrentLinkedQueue<()->Unit>();val failures=mutableListOf<ShotFailure>();val notices=mutableListOf<String>()
        var failed:(Throwable)->Unit={};var now=0L
        val r=ShotRecorderController(directory,{ShotStartContext(AnyWorld,ShotTestFixtures.zero,ShotTestFixtures.metadata())},
            {now},{tasks.add(it)},{notices.add(it)},{ adapter,_,f -> if(adapter!=null) failed=f },diagnose=failures::add)
        fun pump(done:()->Boolean) {
            val deadline=System.nanoTime()+3_000_000_000
            while(!done() && System.nanoTime()<deadline) { while(tasks.isNotEmpty()) tasks.poll()?.invoke();Thread.sleep(5) }
            assertTrue(done())
        }
        r.start();pump { r.status().recording };now=123_000
        val original=IllegalStateException("do-not-export",IOException("nested-private"))
        failed(ShotObservationFailure("packet",original));failed(ShotObservationFailure("tick",original))
        pump { r.phase=="off" }
        assertEquals(1,failures.size);assertEquals("packet",failures.single().stage);assertSame(original,failures.single().cause)
        assertEquals(123,failures.single().stamp!!.micros)
        assertTrue(r.describe().contains("latest.log"));assertFalse(r.describe().contains("do-not-export"))
        assertFalse(notices.joinToString().contains("do-not-export"));assertFalse(Files.readString(r.lastExport!!).contains("do-not-export"))
        r.start();pump { r.status().recording };r.stop(ShotStopReason.USER_STOP);pump { r.phase=="off" }
        assertTrue(Files.readString(r.lastExport!!).contains("\"complete\":true"))
    }
    @Test fun `diagnostic callback failure cannot escape the writer or codec boundary`() {
        val budget=ShotByteBudget();val writer=ShotExportWriter.openWithDiagnostics(directory,"broken",budget,{}, { throw IllegalStateException("logger") }) { throw IOException("disk") }
        assertEquals("io_error",writer.result.get(3,TimeUnit.SECONDS).error)
        val next=ShotExportWriter.open(directory,"next",budget,{})
        next.ready.get(3,TimeUnit.SECONDS)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),next,budget) { throw IllegalStateException("logger") }
        assertDoesNotThrow { session.accept(ShotStamp(1,1),ShotTestFixtures.input("s1").copy(pose=ShotTestFixtures.input("s1").pose.copy(yaw=Float.NaN))) }
        assertFalse(next.result.get(3,TimeUnit.SECONDS).complete)
    }
    private object AnyWorld
}
