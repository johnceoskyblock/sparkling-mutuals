package net.johnceo.sparklingmutuals.safari.shots
import net.johnceo.sparklingmutuals.safari.shots.preview.*
import net.johnceo.sparklingmutuals.safari.shots.preview.contacts.*
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.*
import java.util.concurrent.TimeUnit

class ShotContactProbeTest {
 @TempDir lateinit var directory:Path
 companion object {
  fun probe(shot:String="s1",large:Boolean=false):ShotContactProbeData {
   val p=ShotVector(100.0,20.0,100.0);val nodes=ContactCurveSampler.sample(p,ShotVector(0.0,0.0,1.0),35.0).nodes
   val targets=List(if(large)64 else 1){ContactTarget("private-runtime-identity-$it",it,"Gemzie",ShotBounds(p,ShotVector(101.0,21.0,101.0)),"client_body_v1")}
   val detail=if(large) List(256){ContactTerrainDetail(0.0,.5,p,ShotVector(0.0,1.0,0.0),"minecraft:stone",List(32){ShotBounds(p,ShotVector(101.0,21.0,101.0))},null)} else emptyList()
   return ShotContactProbe.fromSnapshot(ShotTestFixtures.input(shot),p,ContactSceneSnapshot(ContactTrace(nodes,null,ContactStop.RANGE_LIMIT,emptySet()),targets,ContactCoverage(true,emptySet()),1,detail))
  }
 }
 @Test fun `relative coordinates and shot scoped aliases remove runtime identity canaries`() {
  val p=probe();assertEquals(ShotVector(0.0,0.0,0.0),p.nodes.first().position)
  assertEquals("p1",p.targets.single().key)
  assertFalse(ShotCodec.encode(ShotRecord.Event(2,0,0,ShotObservation.ContactProbe(p))).toString(Charsets.UTF_8).contains("private-runtime"))
 }
 @Test fun oversizedProbeCompactsBeforeEncoding() {
  val line=ShotCodec.encode(ShotRecord.Event(2,0,0,ShotObservation.ContactProbe(probe(large=true))))
  assertTrue(line.size<=65536);val p=JsonParser.parseString(line.toString(Charsets.UTF_8)).asJsonObject["data"].asJsonObject["probe"].asJsonObject
  assertTrue(p["truncated"].asBoolean);assertEquals("s1",p["shot"].asString)
  assertTrue(p["omittedTerrain"].asInt>0);assertFalse(line.toString(Charsets.UTF_8).contains("private-runtime"))
 }
 @Test fun `unsupported inputs do not evaluate prediction provider`() {
  var calls=0
  assertNull(ShotContactProbe.produce(ShotTestFixtures.input(variant=ShotVariant.MASTERFUL),true){calls++;probe()})
  assertNull(ShotContactProbe.produce(ShotTestFixtures.input(),false){calls++;probe()})
  assertNull(ShotContactProbe.produce(ShotTestFixtures.input(hand=ShotHand.OFF),true){calls++;probe()})
  assertEquals(0,calls)
 }
 @Test fun `reducer accepts one known ordinary probe without changing association`() {
  val b=ShotTestFixtures.Budget();val sink=ShotTestFixtures.Sink(b);val s=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),sink,b)
  val event=ShotObservation.ContactProbe(probe())
  assertFalse(s.accept(ShotStamp(0,0),event));assertTrue(s.accept(ShotStamp(0,0),ShotTestFixtures.input("s1")))
  assertTrue(s.accept(ShotStamp(0,0),event));assertFalse(s.accept(ShotStamp(0,0),event))
  s.stop(ShotStamp(1,1),ShotStopReason.USER_STOP)
  assertEquals("unmatched",sink.records().last()["shots"].asJsonArray[0].asJsonObject["association"].asString)
 }
 @Test fun `real writer emits version two fixture and finite typed probe`() {
  val b=ShotByteBudget();val w=ShotExportWriter.open(directory,"contact-probe-v2",b,{})
  w.ready.get(3,TimeUnit.SECONDS);val s=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),w,b)
  s.accept(ShotStamp(0,0),ShotTestFixtures.input("s1"));s.accept(ShotStamp(0,0),ShotObservation.ContactProbe(probe()))
  s.stop(ShotStamp(1,1),ShotStopReason.USER_STOP)
  val result=w.result.get(3,TimeUnit.SECONDS);assertTrue(result.complete)
  val content=Files.readString(result.path!!)
  assertEquals(2,JsonParser.parseString(content.lineSequence().first()).asJsonObject["metadata"].asJsonObject["schemaVersion"].asInt)
  val fixture=Path.of("build/shot-fixtures/contact-probe-v2.jsonl");Files.createDirectories(fixture.parent);Files.writeString(fixture,content)
  assertEquals(0,b.usedBytes)
 }
 @Test fun `privacy strings and nonfinite probe fields cannot cross encoder`() {
  for(p in listOf(probe().copy(hypothesis="秘密".repeat(30000)),probe().copy(pitch=Double.NaN),probe().copy(coverage=ContactCoverage(false,setOf("private-user")))))
   assertThrows(IllegalArgumentException::class.java){ShotCodec.encode(ShotRecord.Event(2,0,0,ShotObservation.ContactProbe(p)))}
 }
}
