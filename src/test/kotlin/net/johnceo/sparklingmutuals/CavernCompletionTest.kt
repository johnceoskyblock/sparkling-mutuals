package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class CavernCompletionTest {
    @Test fun `full clear absence is confirmed only inside each supplied radius and never invents captures`() {
        for ((name, point) in mapOf("Cavernfish" to Triple(-85.0,81.0,30.0), "Flitter" to Triple(-84.0,62.0,30.0),
            "Shyworm" to Triple(-119.0,43.0,30.0), "Driftling" to Triple(-120.0,55.0,40.0),
            "Chuckwalla" to Triple(-100.0,45.0,40.0), "Gemzie" to Triple(-141.0,51.0,30.0))) {
            val run=SafariRun(0); val (x,z,radius)=point
            run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=x,playerZ=z)
            assertFalse(run.captureComplete(name)); assertEquals(0,run.count(name))
            repeat(SafariFullClear.minimum(name)) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
            run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=x+radius+.01,playerZ=z)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.CAVERN,setOf(name),false,playerX=x+radius,playerZ=z)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=x+radius,playerZ=z)
            assertTrue(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.CAVERN,setOf(name),false,playerX=x,playerZ=z)
            assertFalse(run.captureComplete(name))
        }
    }
    @Test fun `sparkling requires each zone and distinct spawn minimum without shared center shortcut`() {
        for ((name, x, z, radius) in listOf(
            listOf("Cavernfish", -85.0, 81.0, 40.0), listOf("Flitter", -84.0, 62.0, 40.0),
            listOf("Shyworm", -119.0, 43.0, 40.0), listOf("Driftling", -120.0, 55.0, 60.0),
            listOf("Chuckwalla", -100.0, 45.0, 60.0), listOf("Gemzie", -141.0, 51.0, 40.0))) {
            val species = name as String; val cx = x as Double; val cz = z as Double; val r = radius as Double
            val c = SparklingChecks(); val seen = List(SafariFullClear.minimum(species)) { species to UUID.randomUUID() }
            c.scan(SafariBiome.CAVERN, cx, cz, seen.take(1)); assertFalse(c.checked(species))
            c.scan(SafariBiome.CAVERN, cx+r+.01, cz, seen); assertFalse(c.checked(species))
            c.scan(SafariBiome.CAVERN, cx+r, cz, emptyList()); assertTrue(c.checked(species))
        }
        val c=SparklingChecks();c.scan(SafariBiome.CAVERN,-114.0,49.0,emptyList())
        assertFalse(c.checked("Scrappy"));assertFalse(c.checked("Cavernfish"))
    }
    @Test fun `full clear cannot confirm absence outside species zone and Scrappy needs three UUIDs`() {
        val run=SafariRun(0)
        repeat(4) { run.record(SafariCatch(SafariRoster.named("Cavernfish")!!)) }
        run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=0.0,playerZ=0.0)
        assertFalse(run.captureComplete("Cavernfish"))
        run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=-55.0,playerZ=81.0)
        assertTrue(run.captureComplete("Cavernfish"))
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Scrappy")!!)) }
        val same=UUID.randomUUID()
        repeat(3) { run.observeCritter(it,"Scrappy",same) }
        run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=-85.0,playerZ=81.0)
        assertFalse(run.captureComplete("Scrappy"))
        repeat(2) { run.observeCritter(it+4,"Scrappy",UUID.randomUUID()) }
        run.updateCaptureEvidence(SafariBiome.CAVERN,emptySet(),false,playerX=-85.0,playerZ=81.0)
        assertTrue(run.captureComplete("Scrappy"))
    }
    @Test fun `sparkling keeps Driftling and never shows silverfish while timed Cavern ESP expires`() {
        val run=SafariRun(0);val party=PartySparklingState().apply { select(setOf("me"));accept(setOf("me"),mapOf("me" to emptySet())) }
        val id=UUID.randomUUID()
        assertFalse(SafariEspRules.neededForSparkling("Rockmite",false,party,true,run,id,0))
        assertTrue(SafariEspRules.neededForSparkling("Driftling",false,party,false,run,id,0))
        assertTrue(SafariEspRules.neededForSparkling("Driftling",false,party,false,run,id,10000))
        assertTrue(SafariEspRules.neededForSparkling("Chuckwalla",false,party,true,run,id,0))
        assertFalse(SafariEspRules.neededForSparkling("Chuckwalla",false,party,true,run,id,10000))
    }
}
