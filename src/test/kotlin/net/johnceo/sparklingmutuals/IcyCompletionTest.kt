package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class IcyCompletionTest {
    private val points=mapOf("Tepid" to (-73.0 to -46.0), "Strongarm" to (-107.0 to -56.0),
        "Polaris" to (-110.0 to -78.0), "Shuddersquid" to (-127.0 to -48.0),
        "Nozzlenose" to (-73.0 to -46.0), "Mantis Shrimp" to (-73.0 to -46.0),
        "Billygoat" to (-121.0 to -55.0), "Wumpa" to (-110.0 to -78.0))
    @Test fun `Icy zones require absence and capture minimum in full clear and distinct UUID minimum in Sparkling`() {
        for ((name,point) in points) {
            val (x,z)=point;val run=SafariRun(0);val c=run.sparklingChecks
            repeat(SafariFullClear.minimum(name)) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
            run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=x+30.01,playerZ=z)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.ICY,setOf(name),false,playerX=x+30,playerZ=z)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=x+30,playerZ=z)
            assertTrue(run.captureComplete(name))
            val seen=List(SafariFullClear.minimum(name)) { name to UUID.randomUUID() }
            repeat(3) { c.scan(SafariBiome.ICY,x+40.01,z,seen.take(1)) }
            assertFalse(c.checked(name))
            c.scan(SafariBiome.ICY,x+40.01,z,seen);assertFalse(c.checked(name))
            c.scan(SafariBiome.ICY,x+40,z,emptyList());assertTrue(c.checked(name))
        }
    }
    @Test fun `Troodon needs three UUIDs and captures without any location radius`() {
        val run=SafariRun(0);val ids=List(3) { UUID.randomUUID() }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Troodon")!!));run.observeCritter(it,"Troodon",ids[0]) }
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=500.0,playerZ=500.0)
        assertFalse(run.captureComplete("Troodon"))
        run.sparklingChecks.scan(SafariBiome.ICY,500.0,500.0,List(3) { "Troodon" to ids[0] })
        assertFalse(run.sparklingChecks.checked("Troodon"))
        ids.drop(1).forEachIndexed { i,id -> run.observeCritter(i+4,"Troodon",id) }
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=500.0,playerZ=500.0)
        assertTrue(run.captureComplete("Troodon"))
        run.sparklingChecks.scan(SafariBiome.ICY,500.0,500.0,ids.map { "Troodon" to it })
        assertTrue(run.sparklingChecks.checked("Troodon"))
    }
    @Test fun `Wumpa prerequisite overrides ten second timer and profitability until one capture`() {
        for (name in points.keys - "Wumpa" + "Troodon") {
            val run=SafariRun(0);val uuid=UUID.randomUUID()
            fun party(vararg needs:String)=PartySparklingState().apply { select(setOf("me"));accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()-needs.toSet())) }
            val state=party("Wumpa",name)
            assertTrue(SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,0))
            assertTrue(SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,20000))
            run.record(SafariCatch(SafariRoster.named(name)!!))
            assertFalse(SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,20001))
            val normal=SafariRun(0);val own=party(name)
            assertTrue(SafariEspRules.neededForSparkling(name,false,own,true,normal,uuid,0))
            assertFalse(SafariEspRules.neededForSparkling(name,false,own,true,normal,uuid,10000))
        }
    }
}
