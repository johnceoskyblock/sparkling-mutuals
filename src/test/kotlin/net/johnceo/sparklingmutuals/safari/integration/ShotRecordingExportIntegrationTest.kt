package net.johnceo.sparklingmutuals.safari.integration

import net.johnceo.sparklingmutuals.safari.shots.*
import net.johnceo.sparklingmutuals.safari.SafariTracking
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue

class ShotRecordingExportIntegrationTest {
    @TempDir lateinit var directory:Path
    @Test fun `controlled service export preserves raw smooth display target and feedback provenance`() {
        val queue=ConcurrentLinkedQueue<()->Unit>();var ns=0L
        val origin=ShotVector(100.123456,40.222222,-20.333333)
        val context=ShotStartContext(Any(),origin,ShotMetadata("pending","26.1.2","test","test","minecraft:overworld",true,true))
        val previous=SafariTracking.run
        val recorder=ShotRecorderController(directory,{context},{ns},{queue.add(it)},{},{ _,_,_ -> })
        fun await(condition:()->Boolean) {
            val deadline=System.nanoTime()+5_000_000_000
            while(!condition() && System.nanoTime()<deadline) { while(queue.isNotEmpty()) queue.poll()?.invoke();Thread.sleep(2) }
            assertTrue(condition())
        }
        assertFalse(recorder.status().recording);recorder.start();await { recorder.status().recording }
        val zero=ShotVector(0.0,0.0,0.0)
        val pose=ShotPose(zero,zero,zero,0f,0f,ShotVector(0.0,0.0,1.0),zero)
        recorder.accept(ShotObservation.Input("s1",ShotHand.MAIN,ShotVariant.ORDINARY,ShotUseRoute.AIR,pose,ShotScenario.STATIONARY_LEVEL,ShotSplit.FIT))
        ns=100_000_000
        val cache=ShotEntityCache();cache.pendingSpawn(7,"private-runtime-identity",origin,100000)
        val capsule=cache.discover(7,"private-runtime-identity",ShotVariant.ORDINARY,null,ShotDiscovery.SPAWN,100000)!!
        recorder.accept(ShotObservation.Capsule(capsule.token,capsule.generation,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,zero,discovery=ShotDiscovery.SPAWN,spawnMicros=100000))
        recorder.accept(ShotPacketAdapter.translate(capsule,ShotPacketUpdate.Spawn(7,"private-runtime-identity",origin),origin,100000))
        ns=150_000_000
        recorder.accept(ShotPacketAdapter.translate(capsule,ShotPacketUpdate.Move(7,listOf(ShotPathPoint(ShotVector(.25,0.0,.5)))),origin,150000))
        recorder.accept(ShotObservation.DisplayState(capsule.token,"fixed",zero,ShotVector(1.0,1.0,1.0),ShotQuaternion(0f,0f,0f,1f),ShotQuaternion(0f,0f,0f,1f),0,2,3))
        recorder.accept(ShotObservation.Target("t2",2,"Gemzie",ShotVector(0.0,0.0,4.0),ShotBounds(ShotVector(-.5,0.0,3.5),ShotVector(.5,1.0,4.5))))
        recorder.feedback(ShotFeedback.parse("You threw a Critter Capsule at the Gemzie!")!!,1)
        assertTrue(recorder.mark("s1",ShotManualOutcome.UNKNOWN).ok)
        recorder.stop(ShotStopReason.USER_STOP);await { recorder.phase=="off" }
        val bytes=Files.readAllBytes(recorder.lastExport!!);val text=bytes.toString(Charsets.UTF_8)
        val records=text.lineSequence().filter { it.isNotBlank() }.map { JsonParser.parseString(it).asJsonObject }.toList()
        assertEquals(9,records.count { it["type"].asString=="event" })
        assertTrue(records.last()["complete"].asBoolean)
        assertEquals("candidate_unique",records.last()["shots"].asJsonArray[0].asJsonObject["association"].asString)
        assertTrue(text.contains("\"resolvedEnd\""));assertTrue(text.contains("\"source\":\"game\""))
        assertFalse(text.contains("private-runtime-identity"));assertFalse(text.contains("100.123456"));assertFalse(text.contains("You threw"))
        assertFalse(text.contains("\"capture\""));assertSame(previous,SafariTracking.run)
        val fixtures=Path.of("build/shot-fixtures");Files.createDirectories(fixtures)
        Files.write(fixtures.resolve("service-export.jsonl"),bytes)
    }
}
