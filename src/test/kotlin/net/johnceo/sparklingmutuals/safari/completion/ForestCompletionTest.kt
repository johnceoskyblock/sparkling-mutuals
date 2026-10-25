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

class ForestCompletionTest : net.johnceo.sparklingmutuals.safari.esp.ProfitableEspPriceFixture() {
    @Test fun `Forest zones retain captures and require UUID minimums inside each radius`() {
        for ((name,radius) in mapOf("Foxtrot" to 30.0,"Fluffling" to 30.0,"Woodchucker" to 50.0,"Treefrog" to 50.0,"Hideonfloor" to 50.0)) {
            val run=SafariRun(0)
            repeat(SafariFullClear.minimum(name)) { run.record(SafariCatch(SafariRoster.named(name)!!)) }
            run.updateCaptureEvidence(SafariBiome.FOREST,emptySet(),false,playerX=3+radius+.01,playerZ=47.0)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.FOREST,setOf(name),false,playerX=3+radius,playerZ=47.0)
            assertFalse(run.captureComplete(name))
            run.updateCaptureEvidence(SafariBiome.FOREST,emptySet(),false,playerX=3+radius,playerZ=47.0)
            assertTrue(run.captureComplete(name))
            val seen=List(SafariFullClear.minimum(name)) { name to UUID.randomUUID() }
            run.sparklingChecks.scan(SafariBiome.FOREST,3+radius+10+.01,47.0,seen)
            assertFalse(run.sparklingChecks.checked(name))
            run.sparklingChecks.scan(SafariBiome.FOREST,3+ForestCompletionZones.zones.getValue(name).sparklingRadius,47.0,emptyList())
            assertTrue(run.sparklingChecks.checked(name))
        }
    }
    @Test fun `Honeybug requires punched nests and all observed UUIDs captured`() {
        val run=SafariRun(0); val ids=List(4) { UUID.randomUUID() }
        ids.forEachIndexed { i,id -> run.observeCritter(i,"Honeybug",id) }
        repeat(3) { run.record(SafariCatch(SafariRoster.named("Honeybug")!!)) }
        run.updateCaptureEvidence(SafariBiome.FOREST,emptySet(),false,true,playerX=3.0,playerZ=47.0)
        assertFalse(run.captureComplete("Honeybug"))
        run.record(SafariCatch(SafariRoster.named("Honeybug")!!))
        assertTrue(run.captureComplete("Honeybug"))
        run.updateCaptureEvidence(SafariBiome.FOREST,emptySet(),false,false,playerX=3.0,playerZ=47.0)
        assertTrue(run.captureComplete("Honeybug"), "later empty scans retain the completed source survey")
        val checks=SparklingChecks()
        repeat(4) { checks.scan(SafariBiome.FOREST,500.0,500.0,List(3) { "Honeybug" to ids[0] },nestsChecked=true) }
        assertTrue(checks.checked("Honeybug"))
        checks.scan(SafariBiome.FOREST,500.0,500.0,ids.take(3).map { "Honeybug" to it },nestsChecked=true)
        assertTrue(checks.checked("Honeybug"))
    }
    @Test fun `Forest profitable ESP persists while enabled`() {
        for(name in listOf("Hideonfloor","Fluffling")) {
            val party=PartySparklingState().apply { select(setOf("me")); accept(setOf("me"),mapOf("me" to SafariRoster.all.map { it.name }.toSet())) }
            val run=SafariRun(0);val id=UUID.randomUUID()
            assertTrue(SafariEspRules.neededForSparkling(name,false,party,true,run,id,0))
            assertTrue(SafariEspRules.neededForSparkling(name,false,party,true,run,id,10000))
            assertTrue(SafariEspRules.neededForSparkling(name,false,party,true,run,UUID.randomUUID(),10000))
        }
    }
}
