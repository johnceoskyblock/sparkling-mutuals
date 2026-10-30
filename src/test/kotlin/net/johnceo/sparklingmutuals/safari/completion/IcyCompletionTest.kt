package net.johnceo.sparklingmutuals.safari.completion

import java.util.UUID
import net.johnceo.sparklingmutuals.safari.PartySparklingState
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IcyCompletionTest : net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture() {
    @Test fun `captured head models that linger beyond the grace period never become a fourth critter`() {
        for (species in listOf("Mantis Shrimp","Troodon")) {
            var now=0L
            val run=SafariRun(0) { now }
            run.visitBiome(SafariBiome.ICY)
            var live=List(3) { species to UUID.randomUUID() }
            run.observePopulation(live)
            repeat(3) {
                now+=10000
                run.record(SafariCatch(SafariRoster.named(species)!!),now)
                now+=5000 // Longer than the previous time-only guard.
                live=List(3-it) { species to UUID.randomUUID() } // Capsule transformations replace IDs.
                run.observePopulation(live)
                run.updateCaptureEvidence(SafariBiome.ICY,setOf(species),false,playerX=-73.0,playerZ=-46.0)
                assertFalse(run.fullClearComplete(species))
                run.observePopulation(live.drop(1))
            }
            run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=-73.0,playerZ=-46.0)
            assertTrue(run.fullClearMissingComplete(species),run.captureDebug(SafariBiome.ICY).toString())
            assertTrue(run.fullClearComplete(species))
            run.observePopulation(listOf(species to UUID.randomUUID()))
            run.updateCaptureEvidence(SafariBiome.ICY,setOf(species),false,playerX=-73.0,playerZ=-46.0)
            assertFalse(run.fullClearComplete(species),"A newly seen remainder still requires a catch")
            run.observePopulation(emptyList())
            run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=-73.0,playerZ=-46.0)
            assertFalse(run.fullClearComplete(species),"Unloading the new remainder is not a capture")
        }
    }
    @Test fun `four shrimp captures cannot be added to the same four lingering models`() {
        var now=0L
        val run=SafariRun(0) { now }
        run.visitBiome(SafariBiome.ICY)
        val shrimp=SafariRoster.named("Mantis Shrimp")!!
        val live=List(4) { shrimp.name to UUID.randomUUID() }
        run.observePopulation(live)
        now=1000
        repeat(4) { run.record(SafariCatch(shrimp),now) }
        now=1500
        run.observePopulation(live)
        run.updateCaptureEvidence(SafariBiome.ICY,setOf(shrimp.name),false,playerX=-73.0,playerZ=-46.0)
        assertFalse(run.fullClearComplete(shrimp.name),"Remaining labelled models still block during transition")
        now=4000
        run.observePopulation(emptyList())
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=-73.0,playerZ=-46.0)
        assertTrue(run.fullClearMissingComplete(shrimp.name))
        assertTrue(run.fullClearComplete(shrimp.name))
        run.observePopulation(listOf(shrimp.name to UUID.randomUUID()))
        run.updateCaptureEvidence(SafariBiome.ICY,setOf(shrimp.name),false,playerX=-73.0,playerZ=-46.0)
        assertFalse(run.fullClearComplete(shrimp.name),"A genuinely remaining fifth shrimp must still be caught")
    }
    @Test fun `captured Troodon models cannot inflate the fixed three target`() {
        var now=0L
        val run=SafariRun(0) { now }
        run.visitBiome(SafariBiome.ICY)
        val live=List(3) { "Troodon" to UUID.randomUUID() }
        run.observePopulation(live)
        now=1000
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Troodon")!!),now) }
        run.observePopulation(live)
        run.updateCaptureEvidence(SafariBiome.ICY,setOf("Troodon"),false,playerX=-73.0,playerZ=-46.0)
        assertFalse(run.fullClearComplete("Troodon"))
        now=4000
        run.observePopulation(emptyList())
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=-73.0,playerZ=-46.0)
        assertTrue(run.fullClearMissingComplete("Troodon"))
        assertTrue(run.fullClearComplete("Troodon"))
    }
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
            assertEquals(name == "Wumpa", c.checked(name))
            c.scan(SafariBiome.ICY,x+40.01,z,seen);assertEquals(name == "Wumpa", c.checked(name))
            c.scan(SafariBiome.ICY,x+40,z,emptyList());assertTrue(c.checked(name))
        }
    }
    @Test fun `Troodon accepts three captures without any location radius`() {
        val run=SafariRun(0);val ids=List(3) { UUID.randomUUID() }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Troodon")!!));run.observeCritter(it,"Troodon",ids[0]) }
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=500.0,playerZ=500.0)
        assertTrue(run.captureComplete("Troodon"))
        run.sparklingChecks.scan(SafariBiome.ICY,500.0,500.0,List(3) { "Troodon" to ids[0] })
        assertTrue(run.sparklingChecks.checked("Troodon"))
        ids.drop(1).forEachIndexed { i,id -> run.observeCritter(i+4,"Troodon",id) }
        run.updateCaptureEvidence(SafariBiome.ICY,emptySet(),false,playerX=500.0,playerZ=500.0)
        assertTrue(run.captureComplete("Troodon"))
        run.sparklingChecks.scan(SafariBiome.ICY,500.0,500.0,ids.map { "Troodon" to it })
        assertTrue(run.sparklingChecks.checked("Troodon"))
    }
    @Test fun `Wumpa prerequisites persist until capture with a profitable Shrimp exception`() {
        for (name in points.keys - "Wumpa" + "Troodon") {
            val run=SafariRun(0);val uuid=UUID.randomUUID()
            fun party(vararg needs:String)=PartySparklingState().apply { select(setOf("me"));accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet()-needs.toSet())) }
            val state=party("Wumpa",name)
            assertTrue(SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,0))
            assertTrue(SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,20000))
            run.record(SafariCatch(SafariRoster.named(name)!!))
            assertEquals(name == "Mantis Shrimp", SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,20001))
            assertEquals(name == "Mantis Shrimp", SafariEspRules.neededForSparkling(name,false,state,true,run,uuid,30001))
            val normal=SafariRun(0);val own=party(name)
            assertEquals(name == "Mantis Shrimp", SafariEspRules.neededForSparkling(name,false,own,true,normal,uuid,0))
            assertEquals(name == "Mantis Shrimp" || name == "Wumpa", SafariEspRules.neededForSparkling(name,false,own,true,normal,uuid,10000))
        }
    }
}
