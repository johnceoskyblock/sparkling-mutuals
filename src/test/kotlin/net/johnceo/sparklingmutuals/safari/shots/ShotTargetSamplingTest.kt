package net.johnceo.sparklingmutuals.safari.shots

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import com.google.gson.JsonParser

class ShotTargetSamplingTest {
    @TempDir lateinit var directory:Path
    private val zero=ShotTestFixtures.zero
    private val pose=ShotTestFixtures.input("s1").pose.copy(crosshair=ShotCrosshair("entity",ShotVector(-4.8,0.0,10.0),"t0"))
    private fun target(i:Int,time:Double=0.0):ShotObservation.Target {
        val p=ShotVector((i%8-4)*1.2+.1*kotlin.math.sin(time+i),0.0,10.0+(i/8)*2)
        return ShotObservation.Target("t$i",i+1,"Gemzie",p,
            ShotBounds(ShotVector(p.x-.4,-.1,p.z-.4),ShotVector(p.x+.4,1.2,p.z+.4)),ShotVector(.1*kotlin.math.cos(time+i),0.0,0.0))
    }
    @Test fun `stationary neighbors use sparse keyframes and inputs force fresh complete context`() {
        val sampler=ShotTargetSampler();val targets=List(64) { target(it) }
        assertEquals(64,sampler.sample(ShotStamp(0,0),pose,emptyList(),targets).size)
        for(tick in 1..39) assertTrue(sampler.sample(ShotStamp(tick*50_000L,tick.toLong()),pose,emptyList(),targets).isEmpty())
        assertEquals(64,sampler.sample(ShotStamp(2_000_000,40),pose,emptyList(),targets).size)
        assertEquals(64,sampler.sample(ShotStamp(2_050_000,41),pose,emptyList(),targets,force=true).size)
    }
    @Test fun `moving focus is bounded across rapid changes while broad context still covers all candidates`() {
        val sampler=ShotTargetSampler();sampler.sample(ShotStamp(0,0),pose,emptyList(),List(64) { target(it) })
        val counts=(1..39).map { tick ->
            val changedPose=pose.copy(crosshair=pose.crosshair!!.copy(target="t${tick%64}"))
            sampler.sample(ShotStamp(tick*50_000L,tick.toLong()),changedPose,emptyList(),List(64) { target(it,tick.toDouble()) }).size
        }
        assertTrue(counts.sum()<=72);assertTrue(counts.max()<=8)
        val all=sampler.sample(ShotStamp(2_000_000,40),pose,emptyList(),List(64) { target(it,2.0) })
        assertEquals((0..63).map { "t$it" }.toSet(),all.map { it.token }.toSet())
    }
    @Test fun `range is revalidated and observed capsules retain nearby target context`() {
        val sampler=ShotTargetSampler();val far=target(0).copy(position=ShotVector(100.0,0.0,0.0))
        assertTrue(sampler.sample(ShotStamp(0,0),pose,emptyList(),listOf(far),force=true).isEmpty())
        assertEquals(1,sampler.sample(ShotStamp(50_000,1),pose,listOf(ShotVector(99.0,0.0,0.0)),listOf(far),force=true).size)
        assertTrue(sampler.sample(ShotStamp(100_000,2),pose,emptyList(),listOf(far),force=true).isEmpty())
    }
    @Test fun `target packet traffic updates its base without exporting raw traffic while capsule updates survive`() {
        val adapter=ShotObservationAdapter(zero) { _,_ -> }
        val cache=ShotEntityCache();val target=cache.discover(7,"private-target",null,"Gemzie",ShotDiscovery.BOOTSTRAP,0)!!
        target.base=zero
        val delta=ShotPacketUpdate.Move(7,listOf(ShotPathPoint(ShotVector(1.0,0.0,0.0))))
        assertNull(adapter.packetObservation(target,delta,ShotStamp(1,0)))
        assertEquals(ShotVector(1.0,0.0,0.0),target.base)
        val capsule=cache.discover(8,"private-capsule",ShotVariant.MASTERFUL,null,ShotDiscovery.SPAWN,0)!!;capsule.base=zero
        val actual=adapter.packetObservation(capsule,delta.copy(id=8),ShotStamp(2,0))!!
        assertEquals(capsule.token,actual.token);assertEquals(ShotVector(1.0,0.0,0.0),actual.resolvedEnd)
    }
    @Test fun `still loaded distant targets release bounded cache capacity for new nearby population`() {
        val cache=ShotEntityCache();val adapter=ShotObservationAdapter(zero,cache) { _,_ -> }
        repeat(64) { cache.discover(it,"old$it",null,"Gemzie",ShotDiscovery.BOOTSTRAP,0) }
        val capsule=cache.discover(100,"capsule",ShotVariant.ORDINARY,null,ShotDiscovery.SPAWN,0)!!
        adapter.refreshTargetRange(pose,emptyList()) { ShotVector(100.0,0.0,0.0) }
        repeat(64) { assertNotNull(cache.discover(200+it,"new$it",null,"Gemzie",ShotDiscovery.BOOTSTRAP,1)) }
        assertEquals(64,cache.entries().count { it.species!=null });assertSame(capsule,cache.get(100))
        assertFalse(cache.takeGaps().contains(ShotGap.TARGET_LIMIT))
        val snapshots=cache.entries().filter { it.species!=null }.mapIndexed { i,e -> target(i).copy(token=e.token,generation=e.generation) }
        val sampler=ShotTargetSampler()
        assertEquals(64,sampler.sample(ShotStamp(1,1),pose,emptyList(),snapshots,force=true).size)
        assertEquals(64,sampler.sample(ShotStamp(2_000_001,41),pose,emptyList(),snapshots).size)
        adapter.refreshTargetRange(pose,listOf(ShotVector(99.0,0.0,0.0))) { ShotVector(100.0,0.0,0.0) }
        assertEquals(64,cache.entries().count { it.species!=null })
    }
    @Test fun `populated 180 second collection and longer sparse collection fit existing encoded budgets`() {
        for((seconds,shotInterval) in listOf(180 to 180,600 to 1400)) {
            val budget=ShotByteBudget();val writer=ShotExportWriter.open(directory,"populated-$seconds",budget,{})
            writer.ready.get(3,TimeUnit.SECONDS)
            val session=ShotSession(ShotTestFixtures.metadata().copy(durationSeconds=seconds),ShotLimits(),writer,budget)
            val sampler=ShotTargetSampler();val adapter=ShotObservationAdapter(zero) { _,_ -> }
            val cache=ShotEntityCache();val entries=List(64) { cache.discover(it,"private$it",null,"Gemzie",ShotDiscovery.BOOTSTRAP,0)!!.also { e -> e.base=zero } }
            var inputs=0;var rawCapsules=0;var capsule:ShotObservation.Capsule?=null
            for(tick in 0 until seconds*20) {
                val stamp=ShotStamp(tick*50_000L,tick.toLong());val phase=(tick-10).mod(shotInterval)
                val input=tick>=10 && phase==0
                if(input) { inputs++;session.accept(stamp,ShotTestFixtures.input("s$inputs"))
                    session.accept(stamp,ShotObservation.ContactProbe(ShotContactProbeTest.probe("s$inputs",large=true))) }
                if(input) {
                    capsule=ShotObservation.Capsule("c$inputs",inputs,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,zero,discovery=ShotDiscovery.SPAWN)
                    session.accept(stamp,capsule!!)
                }
                session.accept(stamp,ShotObservation.PlayerState(pose.copy(yaw=(tick%360).toFloat())))
                val targets=List(64) { target(it,tick/20.0) }
                sampler.sample(stamp,pose,emptyList(),targets,force=input).forEach { session.accept(stamp,it) }
                // Busy target packets use the same adapter boundary as the client, not a test-side filter.
                entries.forEach { e -> adapter.packetObservation(e,ShotPacketUpdate.Move(e.id,listOf(ShotPathPoint(ShotVector(.01,0.0,0.0)))),stamp)?.let { session.accept(stamp,it) } }
                if(capsule!=null && phase<40) {
                    val p=ShotVector(0.0,0.0,phase/4.0)
                    session.accept(stamp,ShotObservation.Packet(capsule!!.token,ShotUpdateKind.POSITION_SYNC,listOf(ShotPathPoint(p)),resolvedPoints=listOf(ShotPathPoint(p)),resolvedEnd=p))
                    session.accept(stamp,capsule!!.copy(action=ShotEntityAction.SAMPLE,position=p))
                    session.accept(stamp,ShotObservation.Obstruction(null,capsule!!.token,true,zero,p));rawCapsules++
                }
                if(capsule!=null && phase==40) { session.accept(stamp,capsule!!.copy(action=ShotEntityAction.REMOVED,removal=ShotRemoval.SERVER_REMOVE));capsule=null }
                assertTrue(session.status().recording,"$seconds seconds stopped at tick $tick: ${session.status().stopReason}")
            }
            session.stop(ShotStamp(seconds*1_000_000L-1,seconds*20L-1),ShotStopReason.USER_STOP)
            val result=writer.result.get(5,TimeUnit.SECONDS);assertTrue(result.complete)
            val file=result.path!!;assertTrue(Files.size(file)<10_485_760);assertTrue(session.status().eventCount<100_000)
            var packets=0;var targets=0;var probes=0
            Files.lines(file).use { lines -> lines.forEach { line ->
                val r=JsonParser.parseString(line).asJsonObject
                if(r.get("kind")?.asString=="packet") { packets++;assertTrue(r["data"].asJsonObject["token"].asString.startsWith("c")) }
                if(r.get("kind")?.asString=="target") targets++
                if(r.get("kind")?.asString=="contact_probe") probes++
            } }
            assertEquals(if(seconds==180) 20 else 9,inputs);assertEquals(rawCapsules,packets);assertTrue(targets>64)
            assertEquals(inputs,probes)
            println("POPULATED_WORKLOAD duration=$seconds inputs=$inputs targets=$targets capsulePackets=$packets events=${session.status().eventCount} bytes=${Files.size(file)}")
            assertEquals(0,budget.usedBytes)
        }
    }
}
