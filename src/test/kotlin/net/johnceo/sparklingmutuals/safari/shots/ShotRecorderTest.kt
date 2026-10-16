package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue

class ShotRecorderTest {
    @TempDir lateinit var directory:Path
    private val tasks=ConcurrentLinkedQueue<()->Unit>()
    private var context:ShotStartContext?=ShotStartContext(Any(),ShotTestFixtures.zero,ShotTestFixtures.metadata())
    private var now=0L
    private val notices=mutableListOf<String>()
    private fun recorder()=ShotRecorderController(directory,{context},{now},{tasks.add(it)},{notices.add(it)},{ _,_,_ -> })
    private fun pump(until:()->Boolean) {
        val deadline=System.nanoTime()+3_000_000_000
        while(!until() && System.nanoTime()<deadline) { while(tasks.isNotEmpty()) tasks.poll()?.invoke();Thread.sleep(5) }
        assertTrue(until(),"worker did not finish")
    }
    @Test fun `off start range repeated start and finalize do not replace a session`() {
        val r=recorder();assertFalse(r.status().recording)
        assertFalse(r.start(9).ok);assertFalse(r.start(601).ok)
        assertTrue(r.start().ok);assertFalse(r.start().ok)
        pump { r.status().recording }
        assertTrue(r.mark("s999",ShotManualOutcome.MISS).ok.not())
        r.accept(ShotTestFixtures.input("s1"));assertTrue(r.mark("s1",ShotManualOutcome.UNKNOWN).ok)
        assertTrue(r.stop(ShotStopReason.EXPORT).ok);assertFalse(r.start().ok)
        pump { r.phase=="off" }
        assertNotNull(r.lastExport);assertTrue(Files.exists(r.lastExport!!));assertFalse(r.status().recording)
    }
    @Test fun `world switch is processed before new world input and disconnect never resumes`() {
        val r=recorder();r.start();pump { r.status().recording }
        r.accept(ShotTestFixtures.input("old"))
        context=context!!.copy(world=Any())
        r.accept(ShotTestFixtures.input("new"))
        pump { r.phase=="off" }
        val text=Files.readString(r.lastExport!!)
        assertTrue(text.contains("world_change"));assertFalse(text.contains("\"new\""))
        r.tick();assertFalse(r.status().recording)
        r.start();pump { r.status().recording };r.stop(ShotStopReason.DISCONNECT);pump { r.phase=="off" }
        assertFalse(recorder().status().recording)
    }
    @Test fun `unloaded and nonSafari worlds refuse start and opening cancellation drains correctly`() {
        val r=recorder();context=null;assertFalse(r.start().ok)
        context=ShotStartContext(Any(),ShotTestFixtures.zero,ShotTestFixtures.metadata())
        r.start();r.stop(ShotStopReason.DISCONNECT)
        pump { r.phase=="off" };assertFalse(r.status().recording)
    }
    @Test fun `distinct canonical feedback dispatches survive while repeat delivery of one dispatch does not`() {
        val r=recorder();r.start();pump { r.status().recording }
        r.accept(ShotTestFixtures.input("s1"))
        val hit=ShotFeedback.parse("You threw a Critter Capsule at the Gemzie!")!!
        r.feedback(hit,1);r.feedback(hit,1);r.feedback(hit,2)
        r.stop(ShotStopReason.USER_STOP);pump { r.phase=="off" }
        assertEquals(2,Files.readAllLines(r.lastExport!!).count { it.contains("\"kind\":\"feedback\"") })
    }
    @Test fun `stale queued completion cannot overwrite a later session and previous export survives failure`() {
        val r=recorder();r.start();pump { r.status().recording };r.stop(ShotStopReason.USER_STOP);pump { r.phase=="off" }
        val previous=r.lastExport
        r.start();pump { r.status().recording };val current=r.status().session
        r.workerCompleted(-1,ShotExportResult(directory.resolve("stale"),true))
        assertEquals(current,r.status().session);assertEquals(previous,r.lastExport)
        r.stop(ShotStopReason.ERROR);pump { r.phase=="off" }
    }
    @Test fun `worker IO failure reports once and retains the previous completed export`() {
        var fail=false
        val r=ShotRecorderController(directory,{context},{now},{tasks.add(it)},{notices.add(it)},{ _,_,_ -> },
            { dir,token,budget,done -> ShotExportWriter.open(dir,token,budget,done) { path ->
                if(fail) throw java.io.IOException("private-detail")
                Files.newOutputStream(path,java.nio.file.StandardOpenOption.CREATE_NEW)
            } })
        r.start();pump { r.status().recording };r.stop(ShotStopReason.USER_STOP);pump { r.phase=="off" }
        val previous=r.lastExport!!;val bytes=Files.readAllBytes(previous)
        fail=true;r.start();pump { r.phase=="off" }
        assertEquals(previous,r.lastExport);assertArrayEquals(bytes,Files.readAllBytes(previous))
        assertEquals(1,notices.count { it.contains("io_error") });assertFalse(notices.joinToString().contains("private-detail"))
    }
}
